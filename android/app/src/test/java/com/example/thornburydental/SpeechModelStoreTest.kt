package com.example.thornburydental

import com.example.thornburydental.speech.model.ModelByteSource
import com.example.thornburydental.speech.model.ModelByteStream
import com.example.thornburydental.speech.model.SpeechModelDownloader
import com.example.thornburydental.speech.model.SpeechModelSpec
import com.example.thornburydental.speech.model.SpeechModelState
import com.example.thornburydental.speech.model.SpeechModelStore
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayInputStream
import java.io.File
import java.io.IOException
import java.security.MessageDigest

class SpeechModelStoreTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val payload: ByteArray = ByteArray(4096) { index -> (index % 251).toByte() }

    private fun sha256(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes)
            .joinToString("") { byte -> "%02x".format(byte) }

    private fun spec(
        sha: String = sha256(payload),
        size: Long = payload.size.toLong()
    ) = SpeechModelSpec(
        id = "test-model",
        displayName = "Test model",
        fileName = "test-model.bin",
        downloadUrl = "https://example.invalid/test-model.bin",
        sizeBytes = size,
        sha256 = sha
    )

    /** Serves [content], honouring byte ranges so resume behaviour can be exercised. */
    private class FakeSource(
        private val content: ByteArray,
        private val honourRange: Boolean = true,
        private val truncateTo: Int? = null
    ) : ModelByteSource {
        var lastStartByte: Long = -1

        override fun open(url: String, startByte: Long): ModelByteStream {
            lastStartByte = startByte
            val effectiveStart = if (honourRange) startByte.toInt() else 0
            val remaining = content.copyOfRange(effectiveStart, content.size)
            val served = truncateTo?.let { remaining.copyOfRange(0, minOf(it, remaining.size)) }
                ?: remaining
            return ModelByteStream(
                stream = ByteArrayInputStream(served),
                totalBytes = content.size.toLong(),
                resumedAt = effectiveStart.toLong()
            )
        }
    }

    private fun storeIn(dir: File, source: ModelByteSource): SpeechModelStore =
        SpeechModelStore(modelsDir = dir, downloader = SpeechModelDownloader(source = source))

    @Test
    fun ensureAvailable_downloadsVerifiesAndInstalls() = runBlocking {
        val store = storeIn(temporaryFolder.newFolder("install"), FakeSource(payload))

        val file = store.ensureAvailable(spec())

        assertNotNull("A verified model should be returned", file)
        assertEquals(payload.size.toLong(), file!!.length())
        assertTrue(store.state.value is SpeechModelState.Ready)
    }

    /**
     * The property that matters most: bytes failing the published digest must never be
     * installed. A substituted or corrupted model does not crash the decoder, it produces
     * confident nonsense, and that would land in a patient chart looking like a measurement.
     */
    @Test
    fun ensureAvailable_rejectsContentThatFailsTheDigest() = runBlocking {
        val tampered = payload.copyOf().also { it[10] = (it[10] + 1).toByte() }
        val store = storeIn(temporaryFolder.newFolder("tampered"), FakeSource(tampered))

        val file = store.ensureAvailable(spec())

        assertNull("A model failing its digest must not be installed", file)
        val state = store.state.value
        assertTrue("State should report failure, was $state", state is SpeechModelState.Failed)
    }

    @Test
    fun ensureAvailable_discardsRejectedBytesRatherThanKeepingThem() = runBlocking {
        val tampered = payload.copyOf().also { it[0] = (it[0] + 7).toByte() }
        val dir = temporaryFolder.newFolder("discard")
        val store = storeIn(dir, FakeSource(tampered))

        store.ensureAvailable(spec())

        val leftovers = dir.listFiles().orEmpty().filter { it.length() > 0L }
        assertTrue("Rejected download should be deleted, found $leftovers", leftovers.isEmpty())
    }

    @Test
    fun ensureAvailable_failsWhenTheTransferEndsEarly() = runBlocking {
        val store = storeIn(temporaryFolder.newFolder("short"), FakeSource(payload, truncateTo = 100))

        val file = store.ensureAvailable(spec())

        assertNull("A short transfer must not be installed", file)
        assertTrue(store.state.value is SpeechModelState.Failed)
    }

    @Test
    fun installedModel_returnsNullAndDeletesAFileOfTheWrongSize() = runBlocking {
        val dir = temporaryFolder.newFolder("wrongsize")
        val store = storeIn(dir, FakeSource(payload))
        val planted = File(dir, "test-model.bin")
        planted.writeBytes(ByteArray(12))

        val result = store.installedModel(spec())

        assertNull(result)
        assertFalse("A wrong-sized model should be removed", planted.exists())
    }

    @Test
    fun ensureAvailable_reusesAVerifiedModelWithoutDownloadingAgain() = runBlocking {
        val source = FakeSource(payload)
        val store = storeIn(temporaryFolder.newFolder("reuse"), source)

        assertNotNull(store.ensureAvailable(spec()))
        source.lastStartByte = -1

        val second = store.ensureAvailable(spec())

        assertNotNull(second)
        assertEquals("Second call must not open a transfer", -1L, source.lastStartByte)
    }

    @Test
    fun ensureAvailable_resumesFromAPartialDownload() = runBlocking {
        val dir = temporaryFolder.newFolder("resume")
        File(dir, "test-model.bin.part").writeBytes(payload.copyOfRange(0, 1000))

        val source = FakeSource(payload)
        val store = storeIn(dir, source)

        val file = store.ensureAvailable(spec())

        assertNotNull(file)
        assertEquals("Downloader should have asked to resume", 1000L, source.lastStartByte)
        assertEquals(payload.size.toLong(), file!!.length())
    }

    /**
     * A server that ignores the Range header replies with the whole file from byte zero.
     * Appending that to bytes already on disk would give a file that is the right length
     * only by accident, so the transfer has to restart.
     */
    @Test
    fun ensureAvailable_restartsWhenTheServerIgnoresTheRangeRequest() = runBlocking {
        val dir = temporaryFolder.newFolder("norange")
        File(dir, "test-model.bin.part").writeBytes(payload.copyOfRange(0, 1000))

        val store = storeIn(dir, FakeSource(payload, honourRange = false))

        val file = store.ensureAvailable(spec())

        assertNotNull("Restarting should still yield a valid model", file)
        assertEquals(payload.size.toLong(), file!!.length())
        assertTrue(store.state.value is SpeechModelState.Ready)
    }

    @Test
    fun ensureAvailable_reportsFailureWhenTheSourceThrows() = runBlocking {
        val failing = object : ModelByteSource {
            override fun open(url: String, startByte: Long): ModelByteStream =
                throw IOException("network down")
        }
        val store = storeIn(temporaryFolder.newFolder("neterror"), failing)

        assertNull(store.ensureAvailable(spec()))
        assertTrue(store.state.value is SpeechModelState.Failed)
    }

    @Test
    fun refreshState_reportsAbsentWhenNothingIsInstalled() = runBlocking {
        val store = storeIn(temporaryFolder.newFolder("empty"), FakeSource(payload))

        assertEquals(SpeechModelState.Absent, store.refreshState(spec()))
    }
}
