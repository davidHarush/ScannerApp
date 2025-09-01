package com.david.scanner

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object FileUtil {

    enum class FileType { PDF, IMAGE }

    suspend fun saveFileToDownloads(
        context: Context,
        sourceUri: Uri,
        baseFileName: String,
        fileType: FileType = FileType.PDF
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val resolver = context.contentResolver

            val contentValues = ContentValues().apply {
                when (fileType) {
                    FileType.PDF -> {
                        put(MediaStore.MediaColumns.DISPLAY_NAME, "$baseFileName.pdf")
                        put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                        put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                    }

                    FileType.IMAGE -> {
                        put(MediaStore.MediaColumns.DISPLAY_NAME, "$baseFileName.jpg")
                        put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                        put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
                    }
                }
            }

            val targetUri: Uri? = if (fileType == FileType.PDF) {
                resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            } else {
                resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            }

            if (targetUri == null) {
                Log.e("FileUtil", "Failed to create MediaStore record.")
                return@withContext false
            }

            resolver.openInputStream(sourceUri)?.use { input ->
                resolver.openOutputStream(targetUri)?.use { output ->
                    input.copyTo(output)
                } ?: throw Exception("Failed to open output stream.")
            }
            true
        } catch (e: Exception) {
            Log.e("FileUtil", "Error saving file: ${e.message}", e)
            false
        }
    }

    suspend fun createMultiPagePdf(
        context: Context,
        imageUriList: List<Uri>,
        baseFileName: String
    ): File? = withContext(Dispatchers.IO) {
        try {
            val pdfDocument = PdfDocument()
            val resolver = context.contentResolver

            imageUriList.forEachIndexed { index, uri ->
                resolver.openInputStream(uri)?.use { input ->
                    val bitmap = BitmapFactory.decodeStream(input)
                    val pageInfo =
                        PdfDocument.PageInfo.Builder(bitmap.width, bitmap.height, index + 1)
                            .create()
                    val page = pdfDocument.startPage(pageInfo)
                    page.canvas.drawBitmap(bitmap, 0f, 0f, null)
                    pdfDocument.finishPage(page)
                    bitmap.recycle()
                }
            }

            val pdfFile = File(context.filesDir, "$baseFileName.pdf")
            pdfFile.outputStream().use { output ->
                pdfDocument.writeTo(output)
            }
            pdfDocument.close()
            pdfFile
        } catch (e: Exception) {
            Log.e("FileUtil", "Error creating multi-page pdf: ${e.message}", e)
            null
        }
    }

    fun shareFile(context: Context, file: File, mimeType: String = "application/pdf"): Intent {
        val contentUri: Uri = FileProvider.getUriForFile(
            context,
            "com.david.scanner.provider",
            file
        )
        return Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, contentUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}
