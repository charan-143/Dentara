package com.example.thornburydental.ui.clinic

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.thornburydental.data.ReportAttachment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream

/**
 * Utility helpers for safely handling, decoding, opening, and sharing patient attachments.
 */
object AttachmentViewerUtils {

    private const val TAG = "AttachmentViewerUtils"

    /**
     * Resolves an arbitrary URI string or path to a physical [File] on device storage.
     */
    fun resolveFile(context: Context, uriString: String?): File? {
        if (uriString.isNullOrBlank()) return null

        val cleanPath = when {
            uriString.startsWith("file://") -> uriString.removePrefix("file://")
            uriString.startsWith("file:") -> uriString.removePrefix("file:")
            uriString.startsWith("/") -> uriString
            else -> null
        }
        if (cleanPath != null) {
            val f = File(cleanPath)
            if (f.exists()) return f
        }

        // Search within report_attachments directory by filename
        val fileName = uriString.substringAfterLast('/')
        val dir = File(context.filesDir, "report_attachments")
        if (dir.exists()) {
            val direct = File(dir, fileName)
            if (direct.exists()) return direct

            val matches = dir.listFiles { _, name ->
                name.equals(fileName, ignoreCase = true) ||
                        name.endsWith(fileName, ignoreCase = true) ||
                        fileName.endsWith(name, ignoreCase = true)
            }
            if (!matches.isNullOrEmpty()) return matches.first()
        }
        return null
    }

    /**
     * Resolves a [ReportAttachment] to a physical [File] on local storage if possible.
     */
    fun resolveAttachmentFile(context: Context, attachment: ReportAttachment): File? {
        val fileFromUri = resolveFile(context, attachment.uri)
        if (fileFromUri != null && fileFromUri.exists()) {
            return fileFromUri
        }

        // Search within report_attachments directory by ID or name
        val dir = File(context.filesDir, "report_attachments")
        if (dir.exists()) {
            val matchingFiles = dir.listFiles { _, name ->
                name.contains(attachment.id) ||
                        name.endsWith(attachment.name, ignoreCase = true) ||
                        attachment.name.endsWith(name, ignoreCase = true)
            }
            if (!matchingFiles.isNullOrEmpty()) {
                return matchingFiles.first()
            }
        }

        return null
    }

    /**
     * Safely decodes a bitmap from a URI or file path with inSampleSize calculation to prevent OOM.
     */
    suspend fun decodeBitmapSafely(
        context: Context,
        uriString: String?,
        reqWidth: Int = 1200,
        reqHeight: Int = 1200
    ): Bitmap? = withContext(Dispatchers.IO) {
        if (uriString.isNullOrBlank()) return@withContext null

        try {
            // Check if URI resolves to a physical File on disk
            val fileCandidate = resolveFile(context, uriString)
            if (fileCandidate != null && fileCandidate.exists()) {
                val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(fileCandidate.absolutePath, options)
                if (options.outWidth > 0 && options.outHeight > 0) {
                    options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)
                    options.inJustDecodeBounds = false
                    return@withContext BitmapFactory.decodeFile(fileCandidate.absolutePath, options)
                }
            }

            // Fallback to ContentResolver for content:// URIs
            val uri = Uri.parse(uriString)
            fun openStream(): InputStream? {
                return try {
                    context.contentResolver.openInputStream(uri)
                } catch (e: Exception) {
                    null
                }
            }

            // 1. Decode bounds
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            openStream()?.use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
            } ?: return@withContext null

            // 2. Calculate sample size
            options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)
            options.inJustDecodeBounds = false

            // 3. Decode actual sampled bitmap
            openStream()?.use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to decode bitmap from: $uriString", e)
            null
        }
    }

    private fun calculateInSampleSize(
        options: BitmapFactory.Options,
        reqWidth: Int,
        reqHeight: Int
    ): Int {
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize.coerceAtLeast(1)
    }

    /**
     * Opens a document or image file in the user's external/system viewer using FileProvider.
     */
    fun openAttachment(context: Context, attachment: ReportAttachment) {
        val file = resolveAttachmentFile(context, attachment)
        if (file == null || !file.exists()) {
            Toast.makeText(context, "File is not available on device", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val contentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val mimeType = attachment.mimeType.ifBlank {
                when {
                    file.name.endsWith(".pdf", ignoreCase = true) -> "application/pdf"
                    file.name.endsWith(".jpg", ignoreCase = true) || file.name.endsWith(".jpeg", ignoreCase = true) -> "image/jpeg"
                    file.name.endsWith(".png", ignoreCase = true) -> "image/png"
                    else -> "*/*"
                }
            }

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(contentUri, mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(
                context,
                "No application found to open this ${attachment.name.substringAfterLast('.', "file")}",
                Toast.LENGTH_LONG
            ).show()
        } catch (e: Exception) {
            Log.e(TAG, "Error opening attachment", e)
            Toast.makeText(context, "Unable to open document: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Launches Android's share sheet for an attached file.
     */
    fun shareAttachment(context: Context, attachment: ReportAttachment) {
        val file = resolveAttachmentFile(context, attachment)
        if (file == null || !file.exists()) {
            Toast.makeText(context, "File is not available to share", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val contentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val mimeType = attachment.mimeType.ifBlank {
                when {
                    file.name.endsWith(".pdf", ignoreCase = true) -> "application/pdf"
                    file.name.endsWith(".jpg", ignoreCase = true) || file.name.endsWith(".jpeg", ignoreCase = true) -> "image/jpeg"
                    file.name.endsWith(".png", ignoreCase = true) -> "image/png"
                    else -> "*/*"
                }
            }

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, contentUri)
                putExtra(Intent.EXTRA_SUBJECT, attachment.name)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Share Document / Media").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        } catch (e: Exception) {
            Log.e(TAG, "Error sharing attachment", e)
            Toast.makeText(context, "Could not share file: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
