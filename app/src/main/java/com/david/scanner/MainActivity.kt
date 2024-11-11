package com.david.scanner

import android.app.AlertDialog
import android.net.Uri
import android.os.Bundle
import android.widget.EditText
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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import com.david.scanner.ui.theme.ScannerAppTheme
import com.google.mlkit.vision.documentscanner.GmsDocumentScanner
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult


class MainActivity : ComponentActivity() {

    private var pdfFile: GmsDocumentScanningResult.Pdf? = null
    private var pageUriList = mutableListOf<Uri>()

    private var imagePreViewUri: Uri? = null
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<String>


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


        requestPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {

                pdfFile?.let {
                    saveAsImage()
                }
            } else {
                Toast.makeText(this, "Permission required to save the file", Toast.LENGTH_SHORT)
                    .show()
            }
        }

        setContent {
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
                            pageUriList.addAll(imageUris.value)

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
                            Divider(thickness = 3.dp)
                            SaveOpenShareButtons(pdfFile)
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
    fun ScanDocumentButton(
        scanner: GmsDocumentScanner,
        scannerLauncher: ActivityResultLauncher<IntentSenderRequest>
    ) {
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
        pdfFile: GmsDocumentScanningResult.Pdf?
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    if (pdfFile != null)
                        MaterialTheme.colorScheme.primaryContainer.copy(
                            alpha = 0.7f
                        )
                    else
                        Color.LightGray.copy(
                            alpha = 0.5f
                        )

                )
                .padding(2.dp),
            contentPadding = PaddingValues(2.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            item {
                AppBtn(
                    enabled = pdfFile != null,
                    icon = R.drawable.share,
                    text = "Share",
                    onClick = { FileUtil.shareFile(pdfFile, this@MainActivity) },
                )
            }
            item {
                AppBtn(
                    enabled = pdfFile != null,
                    icon = R.drawable.file_open,
                    text = "Open",
                    onClick = { FileUtil.openFile(pdfFile, this@MainActivity) },
                )
            }
            item {
                AppBtn(
                    enabled = pdfFile != null,
                    icon = R.drawable.save,
                    text = "Save as PDF",
                    onClick = { saveAsPdfToDownloads(pdfFile!!) },
                )
            }
            item {
                AppBtn(
                    enabled = pdfFile != null,
                    icon = R.drawable.save,
                    text = "Save as Image",
                    onClick = { saveAsImage() },
                )
            }
        }
    }

    @Composable
    fun AppBtn(
        enabled: Boolean,
        icon: Int,
        text: String,
        onClick: () -> Unit,
        modifier: Modifier = Modifier
    ) {
        Button(
            enabled = enabled,
            modifier = modifier
                .padding(
                    horizontal = 8.dp,
                    vertical = 8.dp
                ),  // Increased horizontal and vertical padding for better spacing
            onClick = onClick
        ) {
            Icon(painterResource(id = icon), contentDescription = "Scan Document Icon")
            Spacer(modifier = Modifier.padding(4.dp))
            Text(text)
        }
    }


    private fun saveAsImage(
    ) {
        val editText = EditText(this)
        editText.hint = "Imag_name"
        AlertDialog.Builder(this)
            .setTitle("Save Image to Downloads")
            .setView(editText)
            .setPositiveButton("Save") { _, _ ->
                val fileName = editText.text.toString().ifEmpty {
                    editText.hint.toString()
                }
                FileUtil.saveMultipleImagesToDownloads(
                    uriList = pageUriList,
                    context = baseContext,
                    baseFileName = fileName
                )
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

//    @Composable
//    fun AppBtn(enabled: Boolean, icon: Int, text: String, onClick: () -> Unit) {
//
//        Button(
//            enabled = enabled,
//            modifier = Modifier.padding(horizontal = 6.dp),
//            onClick = onClick
//        )
//        {
//            Icon(painterResource(id = icon), contentDescription = "Scan Document Icon")
//            Spacer(modifier = Modifier.padding(4.dp))
//            Text(text)
//        }
//    }

    private fun saveAsPdfToDownloads(
        pdf: GmsDocumentScanningResult.Pdf,
    ) {
        val editText = EditText(this)
        editText.hint = "file_name"
        AlertDialog.Builder(this)
            .setTitle("Save PDF to Downloads")
            .setView(editText)
            .setPositiveButton("Save") { _, _ ->
                val fileName = editText.text.toString().ifEmpty {
                    editText.hint.toString()
                }
                FileUtil.saveFileToDownloads(
                    uri = pdf.uri,
                    fileName = fileName,
                    context = baseContext,
                    fileType = FileUtil.FileType.PDF
                )
            }
            .setNegativeButton("Cancel", null)
            .show()
    }


}