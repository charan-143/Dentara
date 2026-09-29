package com.example.thornburydental

import com.example.thornburydental.data.db.AuditDao
import com.example.thornburydental.data.db.AuditLogEntry
import com.example.thornburydental.data.security.AuditChainIntegrityVerifier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AuditChainIntegrityTest {

    @Test
    fun testSha256Hashing() {
        val hash = AuditDao.sha256("test_payload")
        assertEquals(64, hash.length)
        // Verify deterministic hashing
        assertEquals(hash, AuditDao.sha256("test_payload"))
    }

    @Test
    fun testGenesisChainVerification() {
        // Mocking an in-memory sequence of 3 cryptographic audit entries
        val ts1 = 1000L
        val ts2 = 2000L
        val ts3 = 3000L

        val pHash1 = AuditDao.sha256("{\"patient_id\": \"P1\", \"action\": \"CREATE\"}")
        val entryHash1 = AuditDao.computeEntryHash(AuditDao.GENESIS_HASH, ts1, "dr_smith", "CREATE_PATIENT", "P1", pHash1)

        val pHash2 = AuditDao.sha256("{\"patient_id\": \"P1\", \"tooth\": 16, \"condition\": \"DECAY\"}")
        val entryHash2 = AuditDao.computeEntryHash(entryHash1, ts2, "dr_smith", "CHART_TOOTH", "P1_16", pHash2)

        val pHash3 = AuditDao.sha256("{\"patient_id\": \"P1\", \"drug\": \"Amoxicillin\"}")
        val entryHash3 = AuditDao.computeEntryHash(entryHash2, ts3, "dr_smith", "ISSUE_RX", "RX_99", pHash3)

        val validChain = listOf(
            AuditLogEntry(1, ts1, "dr_smith", "CREATE_PATIENT", "P1", pHash1, AuditDao.GENESIS_HASH, entryHash1),
            AuditLogEntry(2, ts2, "dr_smith", "CHART_TOOTH", "P1_16", pHash2, entryHash1, entryHash2),
            AuditLogEntry(3, ts3, "dr_smith", "ISSUE_RX", "RX_99", pHash3, entryHash2, entryHash3)
        )

        // Verify valid chain
        var prevHash = AuditDao.GENESIS_HASH
        for (entry in validChain) {
            assertEquals(prevHash, entry.prevHash)
            val expected = AuditDao.computeEntryHash(entry.prevHash, entry.timestamp, entry.actorId, entry.actionType, entry.resourceId, entry.payloadHash)
            assertEquals(expected, entry.entryHash)
            prevHash = entry.entryHash
        }
        assertEquals(entryHash3, prevHash)
    }

    @Test
    fun testTamperDetectionOnAlteredPayload() {
        val ts1 = 1000L
        val pHash1 = AuditDao.sha256("Original Patient Name")
        val entryHash1 = AuditDao.computeEntryHash(AuditDao.GENESIS_HASH, ts1, "dr_smith", "UPDATE", "P1", pHash1)

        // Adversary alters payload hash but leaves entryHash untouched
        val tamperedPayloadHash = AuditDao.sha256("Tampered Altered Name")
        val recomputedHash = AuditDao.computeEntryHash(AuditDao.GENESIS_HASH, ts1, "dr_smith", "UPDATE", "P1", tamperedPayloadHash)

        assertTrue(entryHash1 != recomputedHash)
    }
}
