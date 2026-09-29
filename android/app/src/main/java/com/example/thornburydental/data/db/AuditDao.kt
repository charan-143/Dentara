package com.example.thornburydental.data.db

import android.content.ContentValues
import android.database.Cursor
import net.sqlcipher.database.SQLiteDatabase
import java.security.MessageDigest

/**
 * Cryptographic Audit Log entry model representing an immutable block in the SHA-256 audit chain.
 */
data class AuditLogEntry(
    val id: Long,
    val timestamp: Long,
    val actorId: String,
    val actionType: String,
    val resourceId: String,
    val payloadHash: String,
    val prevHash: String,
    val entryHash: String
)

/**
 * Data Access Object for managing tamper-evident audit logs.
 * Enforces SHA-256 cryptographic chain continuity and provides chronological traversal for integrity verification.
 */
class AuditDao(private val dbHelper: ThornburyDbHelper) {

    companion object {
        const val GENESIS_HASH = "0000000000000000000000000000000000000000000000000000000000000000"

        fun sha256(input: String): String {
            val digest = MessageDigest.getInstance("SHA-256")
            val hashBytes = digest.digest(input.toByteArray(Charsets.UTF_8))
            return hashBytes.joinToString("") { "%02x".format(it) }
        }

        fun computeEntryHash(
            prevHash: String,
            timestamp: Long,
            actorId: String,
            actionType: String,
            resourceId: String,
            payloadHash: String
        ): String {
            return sha256("$prevHash|$timestamp|$actorId|$actionType|$resourceId|$payloadHash")
        }
    }

    /**
     * Retrieves all audit logs in strict ascending chronological order (genesis to tip).
     */
    fun getAllLogsChronological(): List<AuditLogEntry> {
        val db = dbHelper.readableDatabase
        val list = mutableListOf<AuditLogEntry>()
        val columns = arrayOf(
            ThornburyDbHelper.COL_AUDIT_ID,
            ThornburyDbHelper.COL_AUDIT_TIMESTAMP,
            ThornburyDbHelper.COL_AUDIT_ACTOR_ID,
            ThornburyDbHelper.COL_AUDIT_ACTION_TYPE,
            ThornburyDbHelper.COL_AUDIT_RESOURCE_ID,
            ThornburyDbHelper.COL_AUDIT_PAYLOAD_HASH,
            ThornburyDbHelper.COL_AUDIT_PREV_HASH,
            ThornburyDbHelper.COL_AUDIT_ENTRY_HASH
        )

        db.query(
            ThornburyDbHelper.TABLE_AUDIT_LOGS,
            columns,
            null,
            null,
            null,
            null,
            "${ThornburyDbHelper.COL_AUDIT_ID} ASC"
        ).use { cursor ->
            val idIdx = cursor.getColumnIndexOrThrow(ThornburyDbHelper.COL_AUDIT_ID)
            val tsIdx = cursor.getColumnIndexOrThrow(ThornburyDbHelper.COL_AUDIT_TIMESTAMP)
            val actorIdx = cursor.getColumnIndexOrThrow(ThornburyDbHelper.COL_AUDIT_ACTOR_ID)
            val actionIdx = cursor.getColumnIndexOrThrow(ThornburyDbHelper.COL_AUDIT_ACTION_TYPE)
            val resIdx = cursor.getColumnIndexOrThrow(ThornburyDbHelper.COL_AUDIT_RESOURCE_ID)
            val payloadIdx = cursor.getColumnIndexOrThrow(ThornburyDbHelper.COL_AUDIT_PAYLOAD_HASH)
            val prevIdx = cursor.getColumnIndexOrThrow(ThornburyDbHelper.COL_AUDIT_PREV_HASH)
            val entryIdx = cursor.getColumnIndexOrThrow(ThornburyDbHelper.COL_AUDIT_ENTRY_HASH)

            while (cursor.moveToNext()) {
                list.add(
                    AuditLogEntry(
                        id = cursor.getLong(idIdx),
                        timestamp = cursor.getLong(tsIdx),
                        actorId = cursor.getString(actorIdx) ?: "",
                        actionType = cursor.getString(actionIdx) ?: "",
                        resourceId = cursor.getString(resIdx) ?: "",
                        payloadHash = cursor.getString(payloadIdx) ?: "",
                        prevHash = cursor.getString(prevIdx) ?: "",
                        entryHash = cursor.getString(entryIdx) ?: ""
                    )
                )
            }
        }
        return list
    }

    /**
     * Retrieves the most recent audit entry to obtain the tip of the hash chain.
     */
    fun getLatestLogEntry(): AuditLogEntry? {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery(
            "SELECT ${ThornburyDbHelper.COL_AUDIT_ID}, ${ThornburyDbHelper.COL_AUDIT_TIMESTAMP}, ${ThornburyDbHelper.COL_AUDIT_ACTOR_ID}, ${ThornburyDbHelper.COL_AUDIT_ACTION_TYPE}, ${ThornburyDbHelper.COL_AUDIT_RESOURCE_ID}, ${ThornburyDbHelper.COL_AUDIT_PAYLOAD_HASH}, ${ThornburyDbHelper.COL_AUDIT_PREV_HASH}, ${ThornburyDbHelper.COL_AUDIT_ENTRY_HASH} FROM ${ThornburyDbHelper.TABLE_AUDIT_LOGS} ORDER BY ${ThornburyDbHelper.COL_AUDIT_ID} DESC LIMIT 1",
            null
        )
        return cursor.use {
            if (it.moveToFirst()) {
                AuditLogEntry(
                    id = it.getLong(0),
                    timestamp = it.getLong(1),
                    actorId = it.getString(2) ?: "",
                    actionType = it.getString(3) ?: "",
                    resourceId = it.getString(4) ?: "",
                    payloadHash = it.getString(5) ?: "",
                    prevHash = it.getString(6) ?: "",
                    entryHash = it.getString(7) ?: ""
                )
            } else null
        }
    }

    /**
     * Appends a new audit log entry linked to the current head of the hash chain.
     * Synchronized to prevent concurrent race conditions during hash chain construction.
     */
    @Synchronized
    fun recordAction(
        actorId: String,
        actionType: String,
        resourceId: String,
        payloadContent: String
    ): Long {
        val db = dbHelper.writableDatabase
        val latest = getLatestLogEntry()
        val prevHash = latest?.entryHash ?: GENESIS_HASH
        val timestamp = System.currentTimeMillis()
        val payloadHash = sha256(payloadContent)
        val entryHash = computeEntryHash(prevHash, timestamp, actorId, actionType, resourceId, payloadHash)

        val values = ContentValues().apply {
            put(ThornburyDbHelper.COL_AUDIT_TIMESTAMP, timestamp)
            put(ThornburyDbHelper.COL_AUDIT_ACTOR_ID, actorId)
            put(ThornburyDbHelper.COL_AUDIT_ACTION_TYPE, actionType)
            put(ThornburyDbHelper.COL_AUDIT_RESOURCE_ID, resourceId)
            put(ThornburyDbHelper.COL_AUDIT_PAYLOAD_HASH, payloadHash)
            put(ThornburyDbHelper.COL_AUDIT_PREV_HASH, prevHash)
            put(ThornburyDbHelper.COL_AUDIT_ENTRY_HASH, entryHash)
        }

        return db.insert(ThornburyDbHelper.TABLE_AUDIT_LOGS, null, values)
    }
}
