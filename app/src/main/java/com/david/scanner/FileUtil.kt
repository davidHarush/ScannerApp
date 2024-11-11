package com.david.scanner

import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import java.io.File


object FileUtil {

    enum class FileType {
        PDF,
        IMAGE
    }

    fun saveMultipleImagesToDownloads(
        uriList: List<Uri>,
        context: Context,
        baseFileName: String
    ) {
        var successCount = 0
        var failureCount = 0

        for ((index, uri) in uriList.withIndex()) {
            val fileName = "$baseFileName-$index"
            val isSavedSuccessfully = saveFileToDownloads(uri, context, fileName, FileType.IMAGE)
            if (isSavedSuccessfully) {
                successCount++
            } else {
                failureCount++
            }
        }

        val message = "Saved $successCount files successfully." +
                if (failureCount > 0) {
                    " Failed to save $failureCount files."
                } else {
                    ""
                }

        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }


    fun saveFileToDownloads(
        uri: Uri?, context: Context, fileName: String, fileType: FileType = FileType.PDF
    ): Boolean {
        if (uri == null) {
            Toast.makeText(context, "Uri is null", Toast.LENGTH_LONG).show()
            return false
        }

        val resolver = context.contentResolver

        val contentValues = if (fileType == FileType.PDF) {
            ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, "$fileName.pdf")
                put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
        } else {
            ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, "$fileName.jpg")
                put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
            }
        }

        try {
            val newUri = if (fileType == FileType.PDF) {
                resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            } else {
                resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            }

            if (newUri != null) {
                resolver.openOutputStream(newUri).use { outputStream ->
                    resolver.openInputStream(uri).use { inputStream ->
                        if (inputStream != null && outputStream != null) {
                            inputStream.copyTo(outputStream)
                            Toast.makeText(
                                context,
                                "File saved successfully!",
                                Toast.LENGTH_LONG
                            ).show()
                        } else {
                            //   throw Exception("Failed to open input or output stream.")
                            return false
                        }
                    }
                }
            } else {
//                throw Exception("Failed to create new MediaStore record.")
                return false
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Error saving: ${e.message}", Toast.LENGTH_LONG).show()
            Log.e("FileUtil", "Error saving file: ${e.message}")
            return false
        }
        return true
    }

    fun shareFile(pdfFile: GmsDocumentScanningResult.Pdf?, activity: MainActivity) {

        val contentUri = FileProvider.getUriForFile(
            activity,
            "com.david.scanner.provider",
            File(pdfFile?.uri?.path!!)
        )

        val intent = Intent().apply {
            action = Intent.ACTION_SEND
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, contentUri)
        }
        activity.startActivity(Intent.createChooser(intent, "Share PDF"))
    }

    fun openFile( pdfFile: GmsDocumentScanningResult.Pdf?,  activity: MainActivity) {
        val file = File(pdfFile?.uri?.path!!)
        val contentUri = FileProvider.getUriForFile(
            activity,
            "com.david.scanner.provider",
            file
        )
        val intent = Intent().apply {
            action = Intent.ACTION_VIEW
            data = contentUri
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            val mime: String? =
                activity.contentResolver.getType(contentUri)
            setDataAndType(contentUri, mime)
        }
        activity.startActivity(intent)

    }
}
