package com.example.thornburydental.data.security

import android.util.Log
import com.example.thornburydental.data.db.AuditDao
import com.example.thornburydental.data.db.AuditLogEntry

/**
 * Startup and runtime cryptographic integrity verifier for the append-only SHA-256 audit chain.
 * Re-validates the entire cryptographic hash chain from Genesis block to current tip.
 */
object AuditChainIntegrityVerifier {

    private const val TAG = "AuditIntegrityVerifier"

    sealed class VerificationResult {
        data class Valid(val totalEntries: Int, val tipHash: String) : VerificationResult()
        data class Tampered(
            val brokenEntryId: Long,
            val reason: String,
            val expectedHash: String,
            val actualHash: String
        ) : VerificationResult()
    }

    /**
     * Revalidates the entire cryptographic hash chain.
     * Checks:
     * 1. Genesis block references the exact standard 64-zero genesis hash.
     * 2. Every subsequent entry's prevHash matches the previous entry's entryHash.
     * 3. Every entry's entryHash matches SHA256(prevHash|timestamp|actorId|actionType|resourceId|payloadHash).
     */
    fun verifyChain(auditDao: AuditDao): VerificationResult {
        val logs = auditDao.getAllLogsChronological()
        if (logs.isEmpty()) {
            Log.i(TAG, "Audit log chain is empty. Genesis verification successful.")
            return VerificationResult.Valid(0, AuditDao.GENESIS_HASH)
        }

        var expectedPrevHash = AuditDao.GENESIS_HASH

        for ((index, entry) in logs.withIndex()) {
            // Check previous hash continuity
            if (entry.prevHash != expectedPrevHash) {
                val reason = "Previous hash mismatch at entry #${entry.id} (index $index). Chain bifurcation detected."
                Log.e(TAG, reason)
                return VerificationResult.Tampered(
                    brokenEntryId = entry.id,
                    reason = reason,
                    expectedHash = expectedPrevHash,
                    actualHash = entry.prevHash
                )
            }

            // Recalculate and verify the current block's hash
            val calculatedHash = AuditDao.computeEntryHash(
                prevHash = entry.prevHash,
                timestamp = entry.timestamp,
                actorId = entry.actorId,
                actionType = entry.actionType,
                resourceId = entry.resourceId,
                payloadHash = entry.payloadHash
            )

            if (entry.entryHash != calculatedHash) {
                val reason = "Cryptographic hash mismatch at entry #${entry.id} (index $index). Record contents have been altered."
                Log.e(TAG, reason)
                return VerificationResult.Tampered(
                    brokenEntryId = entry.id,
                    reason = reason,
                    expectedHash = calculatedHash,
                    actualHash = entry.entryHash
                )
            }

            expectedPrevHash = entry.entryHash
        }

        Log.i(TAG, "Audit log chain integrity successfully verified (${logs.size} immutable entries). Tip: $expectedPrevHash")
        return VerificationResult.Valid(logs.size, expectedPrevHash)
    }
}
