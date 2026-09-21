package com.example.thornburydental.data.cds

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.Base64
import com.example.thornburydental.data.DiagnosticReport
import com.example.thornburydental.data.ReportAttachment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.util.Locale

/**
 * Preprocesses multimodal clinical diagnostic files (radiographs, CBCT slices,
 * intraoral photos, and PDF lab reports) for multimodal AI agent inspection.
 */
object MultimodalReportProcessor {

    private const val MAX_IMAGE_DIMENSION = 1024
    private const val JPEG_COMPRESSION_QUALITY = 85

    /**
     * Converts a DiagnosticReport and its ReportAttachments into a list of MultimodalMediaItems.
     */
    suspend fun processReportAttachments(
        context: Context,
        report: DiagnosticReport
    ): List<MultimodalMediaItem> = withContext(Dispatchers.IO) {
        val items = mutableListOf<MultimodalMediaItem>()

        for (attachment in report.attachments) {
            val mediaKind = classifyMediaKind(report.kind, attachment.name, attachment.mimeType)
            val uri = attachment.uri?.let { Uri.parse(it) }

            var bitmap: Bitmap? = null
            var base64Str: String? = null
            var textExtract: String? = null

            if (uri != null) {
                if (attachment.mimeType.startsWith("image/") || attachment.name.endsWith(".jpg", true) || attachment.name.endsWith(".png", true)) {
                    bitmap = loadAndScaleBitmap(context, uri)
                    if (bitmap != null) {
                        base64Str = bitmapToBase64(bitmap)
                    }
                } else if (attachment.mimeType == "application/pdf" || attachment.name.endsWith(".pdf", true)) {
                    val pdfResult = extractPdfPreviewAndText(context, uri)
                    bitmap = pdfResult.first
                    if (bitmap != null) {
                        base64Str = bitmapToBase64(bitmap)
                    }
                    textExtract = pdfResult.second
                }
            }

            items.add(
                MultimodalMediaItem(
                    id = attachment.id,
                    reportId = report.id,
                    name = attachment.name,
                    mimeType = attachment.mimeType,
                    localUri = attachment.uri,
                    mediaKind = mediaKind,
                    thumbnailBitmap = bitmap,
                    base64Data = base64Str,
                    isSelected = true,
                    sizeBytes = 0L,
                    textExtract = textExtract ?: report.summary
                )
            )
        }

        items
    }

    /**
     * Classifies media kind based on report kind title, filename, and mimeType.
     */
    fun classifyMediaKind(reportKind: String, filename: String, mimeType: String): MediaAnalysisKind {
        val lowerKind = reportKind.lowercase(Locale.ROOT)
        val lowerName = filename.lowercase(Locale.ROOT)

        return when {
            lowerKind.contains("cbct") || lowerName.contains("cbct") || lowerName.contains("axial") || lowerName.contains("sagittal") -> MediaAnalysisKind.CBCT_SCAN
            lowerKind.contains("radiograph") || lowerKind.contains("x-ray") || lowerName.contains("xray") || lowerName.contains("opg") || lowerName.contains("periapical") || lowerName.contains("bitewing") -> MediaAnalysisKind.RADIOGRAPH
            lowerName.contains("photo") || lowerName.contains("camera") || lowerKind.contains("charting") || lowerName.contains("intraoral") -> MediaAnalysisKind.INTRAORAL_PHOTO
            mimeType == "application/pdf" || lowerName.endsWith(".pdf") || lowerKind.contains("lab") -> MediaAnalysisKind.LAB_PDF
            else -> MediaAnalysisKind.GENERAL_DOCUMENT
        }
    }

    /**
     * Loads a bitmap from a content Uri or file path with subsampling and scale constraints.
     */
    fun loadAndScaleBitmap(context: Context, uri: Uri): Bitmap? {
        return try {
            val inputStream: InputStream? = if (uri.scheme == "file") {
                File(uri.path ?: return null).inputStream()
            } else {
                context.contentResolver.openInputStream(uri)
            }

            inputStream?.use { stream ->
                val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeStream(stream, null, boundsOptions)

                var sampleSize = 1
                while (boundsOptions.outWidth / sampleSize > MAX_IMAGE_DIMENSION || boundsOptions.outHeight / sampleSize > MAX_IMAGE_DIMENSION) {
                    sampleSize *= 2
                }

                val decodeStream = if (uri.scheme == "file") {
                    File(uri.path ?: return null).inputStream()
                } else {
                    context.contentResolver.openInputStream(uri)
                }

                decodeStream?.use { secondStream ->
                    val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }
                    val decoded = BitmapFactory.decodeStream(secondStream, null, decodeOptions) ?: return null
                    
                    // Constrain exact max dimensions if needed
                    val maxSide = maxOf(decoded.width, decoded.height)
                    if (maxSide > MAX_IMAGE_DIMENSION) {
                        val scale = MAX_IMAGE_DIMENSION.toFloat() / maxSide
                        val matrix = Matrix().apply { postScale(scale, scale) }
                        Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
                    } else {
                        decoded
                    }
                }
            }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Extracts first page preview bitmap and metadata from PDF files using Android native PdfRenderer.
     */
    private fun extractPdfPreviewAndText(context: Context, uri: Uri): Pair<Bitmap?, String?> {
        return try {
            val pfd: ParcelFileDescriptor? = if (uri.scheme == "file") {
                ParcelFileDescriptor.open(File(uri.path ?: return Pair(null, null)), ParcelFileDescriptor.MODE_READ_ONLY)
            } else {
                context.contentResolver.openFileDescriptor(uri, "r")
            }

            pfd?.use { descriptor ->
                val renderer = PdfRenderer(descriptor)
                val pageCount = renderer.pageCount
                var firstPageBitmap: Bitmap? = null

                if (pageCount > 0) {
                    val page = renderer.openPage(0)
                    val width = minOf(page.width, 1024)
                    val height = (page.height * (width.toFloat() / page.width)).toInt()
                    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    page.close()
                    firstPageBitmap = bitmap
                }
                renderer.close()

                val summary = "Diagnostic PDF Document ($pageCount page(s)). Clinical lab parameters & findings attached."
                Pair(firstPageBitmap, summary)
            } ?: Pair(null, null)
        } catch (_: Exception) {
            Pair(null, null)
        }
    }

    /**
     * Encodes a Bitmap to Base64 JPEG string.
     */
    fun bitmapToBase64(bitmap: Bitmap): String {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_COMPRESSION_QUALITY, stream)
        val byteArray = stream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.NO_WRAP)
    }
}
