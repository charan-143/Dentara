package com.example.thornburydental.data.sync

import android.content.ContentValues
import android.content.Context
import android.util.Log
import com.example.thornburydental.data.db.ThornburyDbHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

data class QueuedCloudRequest(
    val id: String,
    val reportId: String,
    val requestType: String,
    val payloadJson: String,
    val status: String,
    val retryCount: Int,
    val createdAt: Long
)

/**
 * Queue manager for offline-first resilient cloud processing.
 * Manages disconnected Gemini multimodal radiograph analyses and safety CDS queries,
 * automatically processing them with exponential backoff when connectivity is restored.
 */
class CloudAnalysisQueueManager(private val dbHelper: ThornburyDbHelper) {

    companion object {
        private const val TAG = "CloudQueueManager"
        const val STATUS_PENDING = "PENDING"
        const val STATUS_PROCESSING = "PROCESSING"
        const val STATUS_COMPLETED = "COMPLETED"
        const val STATUS_FAILED = "FAILED"
        const val MAX_RETRIES = 5
    }

    /**
     * Enqueues a clinical radiograph or CDS query for background processing.
     */
    fun enqueue(reportId: String, requestType: String, payloadJson: String): String {
        val queueId = UUID.randomUUID().toString()
        val db = dbHelper.writableDatabase

        val values = ContentValues().apply {
            put(ThornburyDbHelper.COL_QUEUE_ID, queueId)
            put(ThornburyDbHelper.COL_QUEUE_REPORT_ID, reportId)
            put(ThornburyDbHelper.COL_QUEUE_REQUEST_TYPE, requestType)
            put(ThornburyDbHelper.COL_QUEUE_PAYLOAD_JSON, payloadJson)
            put(ThornburyDbHelper.COL_QUEUE_STATUS, STATUS_PENDING)
            put(ThornburyDbHelper.COL_QUEUE_RETRY_COUNT, 0)
            put(ThornburyDbHelper.COL_QUEUE_CREATED_AT, System.currentTimeMillis())
        }

        db.insert(ThornburyDbHelper.TABLE_CLOUD_REQUEST_QUEUE, null, values)
        Log.i(TAG, "Enqueued offline cloud request $queueId for report $reportId ($requestType)")
        return queueId
    }

    /**
     * Retrieves all pending requests awaiting network dispatch.
     */
    fun getPendingRequests(): List<QueuedCloudRequest> {
        val db = dbHelper.readableDatabase
        val list = mutableListOf<QueuedCloudRequest>()

        db.query(
            ThornburyDbHelper.TABLE_CLOUD_REQUEST_QUEUE,
            null,
            "${ThornburyDbHelper.COL_QUEUE_STATUS} = ? OR (${ThornburyDbHelper.COL_QUEUE_STATUS} = ? AND ${ThornburyDbHelper.COL_QUEUE_RETRY_COUNT} < ?)",
            arrayOf(STATUS_PENDING, STATUS_PROCESSING, MAX_RETRIES.toString()),
            null,
            null,
            "${ThornburyDbHelper.COL_QUEUE_CREATED_AT} ASC"
        ).use { cursor ->
            val idIdx = cursor.getColumnIndexOrThrow(ThornburyDbHelper.COL_QUEUE_ID)
            val repIdx = cursor.getColumnIndexOrThrow(ThornburyDbHelper.COL_QUEUE_REPORT_ID)
            val typeIdx = cursor.getColumnIndexOrThrow(ThornburyDbHelper.COL_QUEUE_REQUEST_TYPE)
            val payloadIdx = cursor.getColumnIndexOrThrow(ThornburyDbHelper.COL_QUEUE_PAYLOAD_JSON)
            val statusIdx = cursor.getColumnIndexOrThrow(ThornburyDbHelper.COL_QUEUE_STATUS)
            val retryIdx = cursor.getColumnIndexOrThrow(ThornburyDbHelper.COL_QUEUE_RETRY_COUNT)
            val createdIdx = cursor.getColumnIndexOrThrow(ThornburyDbHelper.COL_QUEUE_CREATED_AT)

            while (cursor.moveToNext()) {
                list.add(
                    QueuedCloudRequest(
                        id = cursor.getString(idIdx),
                        reportId = cursor.getString(repIdx),
                        requestType = cursor.getString(typeIdx),
                        payloadJson = cursor.getString(payloadIdx),
                        status = cursor.getString(statusIdx),
                        retryCount = cursor.getInt(retryIdx),
                        createdAt = cursor.getLong(createdIdx)
                    )
                )
            }
        }
        return list
    }

    /**
     * Marks a request as successfully completed.
     */
    fun markCompleted(queueId: String) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(ThornburyDbHelper.COL_QUEUE_STATUS, STATUS_COMPLETED)
        }
        db.update(
            ThornburyDbHelper.TABLE_CLOUD_REQUEST_QUEUE,
            values,
            "${ThornburyDbHelper.COL_QUEUE_ID} = ?",
            arrayOf(queueId)
        )
        Log.i(TAG, "Completed queued cloud request $queueId")
    }

    /**
     * Increments retry count or marks request as permanently failed if max retries exceeded.
     */
    fun recordFailure(queueId: String, currentRetries: Int) {
        val db = dbHelper.writableDatabase
        val nextRetries = currentRetries + 1
        val newStatus = if (nextRetries >= MAX_RETRIES) STATUS_FAILED else STATUS_PENDING

        val values = ContentValues().apply {
            put(ThornburyDbHelper.COL_QUEUE_RETRY_COUNT, nextRetries)
            put(ThornburyDbHelper.COL_QUEUE_STATUS, newStatus)
        }
        db.update(
            ThornburyDbHelper.TABLE_CLOUD_REQUEST_QUEUE,
            values,
            "${ThornburyDbHelper.COL_QUEUE_ID} = ?",
            arrayOf(queueId)
        )
        Log.w(TAG, "Cloud request $queueId failed (Attempt $nextRetries/$MAX_RETRIES) -> Status: $newStatus")
    }

    /**
     * Processes pending queue items asynchronously when online.
     */
    suspend fun processQueue(
        processor: suspend (QueuedCloudRequest) -> Boolean
    ) = withContext(Dispatchers.IO) {
        val pending = getPendingRequests()
        for (request in pending) {
            try {
                val success = processor(request)
                if (success) {
                    markCompleted(request.id)
                } else {
                    recordFailure(request.id, request.retryCount)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error processing cloud request ${request.id}", e)
                recordFailure(request.id, request.retryCount)
            }
        }
    }
}
