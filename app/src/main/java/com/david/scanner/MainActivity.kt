package com.david.scanner

import android.app.AlertDialog
import android.content.ClipData
import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.EditText
import android.widget.Space
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.david.scanner.ui.theme.ScannerAppTheme
import com.google.mlkit.vision.documentscanner.GmsDocumentScanner
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import java.io.File

class MainActivity : ComponentActivity() {

    private var pdfFile: GmsDocumentScanningResult.Pdf? = null
    private var imagePreViewUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val options = GmsDocumentScannerOptions.Builder()
            .setGalleryImportAllowed(true)
            .setPageLimit(5)
            .setResultFormats(
                GmsDocumentScannerOptions.RESULT_FORMAT_JPEG,
                GmsDocumentScannerOptions.RESULT_FORMAT_PDF
            )
            .build()
        val scanner = GmsDocumentScanning.getClient(options)

        setContent {
            val saveSuccess = remember { mutableStateOf(false) }

            ScannerAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {

                    val imageUris = remember { mutableStateOf(emptyList<Uri>()) }

                    val scannerLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.StartIntentSenderForResult(),
                        onResult = { activityResult ->
                            if (activityResult.resultCode != RESULT_OK) return@rememberLauncherForActivityResult
                            val result =
                                GmsDocumentScanningResult.fromActivityResultIntent(activityResult.data)
                            imageUris.value = emptyList()
                            imageUris.value = result?.pages?.map { it.imageUri } ?: emptyList()

                            result?.pdf?.let { gmsPdf ->
                                pdfFile = gmsPdf
                            }

                            result?.pages?.let { pages ->
                                imagePreViewUri = pages.first().imageUri
                            }
                        }
                    )


                    Box(modifier = Modifier.fillMaxSize()) {
                        Column {
                            ScanDocumentButton(scanner, scannerLauncher)
                            SaveOpenShareButtons(saveSuccess, pdfFile)
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.Top,
                                horizontalAlignment = Alignment.CenterHorizontally
                            )
                            {
                                imageUris.value.forEach { uri ->
                                    Box(modifier = Modifier.padding(8.dp)) {
                                        AsyncImage(
                                            model = uri,
                                            contentDescription = null,
                                            contentScale = ContentScale.FillWidth,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }

                            }
                        }
                    }


                }
            }
        }
    }

@Composable
fun ScanDocumentButton(scanner: GmsDocumentScanner, scannerLauncher: ActivityResultLauncher<IntentSenderRequest>) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .zIndex(1f)
            .background(
                MaterialTheme.colorScheme.primaryContainer.copy(
                    alpha = 0.8f
                )
            )
            .padding(8.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        Button(
            modifier = Modifier.padding(6.dp),

            onClick = {
                scanner.getStartScanIntent(this@MainActivity)
                    .addOnSuccessListener { intent ->
                        scannerLauncher.launch(
                            IntentSenderRequest.Builder(intent).build()
                        )
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(
                            applicationContext,
                            "Error: ${e.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }

            }

        ) {
            Text("Scan Document")
        }

    }
}


    @Composable
    fun SaveOpenShareButtons(
        saveSuccess: MutableState<Boolean>,
        pdfFile: GmsDocumentScanningResult.Pdf?
    ) {
        Row( // Row for Save, Open and Share buttons
            modifier = Modifier
                .fillMaxWidth()
                .zIndex(1f)
                .background(
                    MaterialTheme.colorScheme.primaryContainer.copy(
                        alpha = 0.5f
                    )
                )
                .padding(6.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            Button(
                enabled = pdfFile != null,
                modifier = Modifier.padding(horizontal = 6.dp),
                onClick = {
                    saveAsPdfToDownloads(pdfFile!!, saveSuccess)
                }) {
                Icon(painterResource(id = R.drawable.save), contentDescription = "Scan Document Icon")
                Spacer(modifier = Modifier.padding(4.dp))
                Text("Save")
            }
            Button(
                enabled = pdfFile != null,
                modifier = Modifier.padding(horizontal = 6.dp),
                onClick = {openFile()}
            )
            {
                Icon(painterResource(id = R.drawable.file_open), contentDescription = "Scan Document Icon")
                Spacer(modifier = Modifier.padding(4.dp))
                Text("Open")
            }

            Button(
                enabled = pdfFile != null,
                modifier = Modifier.padding(horizontal = 6.dp),
                onClick = { shareFile()}
            )
            {
                Icon(painterResource(id = R.drawable.share), contentDescription = "Scan Document Icon")
                Spacer(modifier = Modifier.padding(4.dp))
                Text("Share" )
            }
        }
    }

    private fun shareFile() {

        val contentUri = FileProvider.getUriForFile(
            applicationContext,
            "com.david.scanner.provider",
            File(pdfFile?.uri?.path!!)
        )

        val intent = Intent().apply {
            action = Intent.ACTION_SEND
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, contentUri)
        }
        startActivity(Intent.createChooser(intent, "Share PDF"))
    }

    private fun openFile() {
        val file = File(pdfFile?.uri?.path!!)
        val contentUri = FileProvider.getUriForFile(
            applicationContext,
            "com.david.scanner.provider",
            file
        )
        val intent = Intent().apply {
            action = Intent.ACTION_VIEW
            data = contentUri
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            val mime: String? =
                applicationContext.contentResolver.getType(contentUri)
            setDataAndType(contentUri, mime)
        }
        startActivity(intent)

    }


    private fun saveAsPdfToDownloads(
        pdf: GmsDocumentScanningResult.Pdf,
        saveSuccess: MutableState<Boolean>
    ) {
        val editText = EditText(this)
        editText.hint = "file_name"
        AlertDialog.Builder(this)
            .setTitle("Save PDF to Downloads")
            .setView(editText)
            .setPositiveButton("Save") { _, _ ->
                val fileName = editText.text.toString()
                if (fileName.isNotEmpty()) {
                    saveFileWithGivenName(pdf, fileName, saveSuccess)
                } else {
                    saveFileWithGivenName(pdf, editText.hint.toString(), saveSuccess)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }


    private fun saveFileWithGivenName(
        pdf: GmsDocumentScanningResult.Pdf,
        fileName: String,
        saveSuccess: MutableState<Boolean>
    ) {
        val resolver = applicationContext.contentResolver

        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, "$fileName.pdf")
            put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
        }

        try {
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            if (uri != null) {
                resolver.openOutputStream(uri).use { outputStream ->
                    resolver.openInputStream(pdf.uri).use { inputStream ->
                        if (inputStream != null && outputStream != null) {
                            inputStream.copyTo(outputStream)
                        } else {
                            throw Exception("Failed to open stream.")
                        }
                    }
                }
                saveSuccess.value = true
                Toast.makeText(applicationContext, "PDF saved to Downloads", Toast.LENGTH_LONG)
                    .show()
            } else {
                saveSuccess.value = false
                throw Exception("Failed to create new MediaStore record.")
            }
        } catch (e: Exception) {
            Toast.makeText(applicationContext, "Error saving PDF: ${e.message}", Toast.LENGTH_LONG)
                .show()
        }
    }


}

