package com.david.scanner

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.ColorInt
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.david.scanner.presentation.ScannerViewModel
import com.david.scanner.ui.screens.ScannerScreen
import com.david.scanner.ui.theme.ScannerAppTheme
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.util.Calendar

class MainActivity : ComponentActivity() {

    private val scanner by lazy {
        val options = GmsDocumentScannerOptions.Builder()
            .setGalleryImportAllowed(true)
            .setPageLimit(15)
            .setResultFormats(
                GmsDocumentScannerOptions.RESULT_FORMAT_JPEG,
                GmsDocumentScannerOptions.RESULT_FORMAT_PDF
            )
            .build()
        GmsDocumentScanning.getClient(options)
    }

    private lateinit var viewModel: ScannerViewModel

    private val scanLauncher =
        registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
            viewModel.updateLoadingState(false)
            if (result.resultCode == RESULT_OK) {
                val scanResult = GmsDocumentScanningResult.fromActivityResultIntent(result.data)
                viewModel.updateScanResult(
                    pdf = scanResult?.pdf,
                    pages = scanResult?.pages?.map { it.imageUri } ?: emptyList()
                )

                val pages = scanResult?.pages
                val message = when {
                    scanResult?.pdf != null -> "PDF document scanned successfully! ✓"
                    pages != null && pages.isNotEmpty() -> "${pages.size} page${if (pages.size > 1) "s" else ""} scanned successfully! ✓"
                    else -> "Scan cancelled"
                }

                showEnhancedToast(message, isSuccess = scanResult != null)
            } else {
                showEnhancedToast("Scan cancelled", isSuccess = false)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(transparent, transparent),
            navigationBarStyle = SystemBarStyle.light(transparent, transparent)
        )

        setContent {
            viewModel = viewModel<ScannerViewModel>()

            ScannerAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize().navigationBarsPadding().statusBarsPadding(),
                    color = Color.White
                ) {
                    ScannerScreen(
                        onScanClick = { startScanning() },
                        onSaveClick = { saveScannedDocument() },
                        onShareClick = { shareScannedDocument() },
                        viewModel = viewModel
                    )
                }
            }
        }
    }

    private fun startScanning() {
        viewModel.updateLoadingState(true)
        scanner.getStartScanIntent(this)
            .addOnSuccessListener { intentSender ->
                scanLauncher.launch(IntentSenderRequest.Builder(intentSender).build())
            }
            .addOnFailureListener { exception ->
                viewModel.updateLoadingState(false)
                showEnhancedToast("❌ Error starting scanner: ${exception.message}", isSuccess = false)
            }
    }

    private fun saveScannedDocument() {
        lifecycleScope.launch {
            viewModel.updateLoadingState(true)
            try {
                val success = withContext(Dispatchers.IO) {
                    when {
                        viewModel.scannedPdf != null -> {
                            FileUtil.saveFileToDownloads(
                                context = applicationContext,
                                sourceUri = viewModel.scannedPdf!!.uri,
                                baseFileName = viewModel.fileNameInput,
                                fileType = FileUtil.FileType.PDF
                            )
                        }
                        viewModel.scannedPages.isNotEmpty() -> {
                            val pdfFile = FileUtil.createMultiPagePdf(
                                context = applicationContext,
                                imageUriList = viewModel.scannedPages,
                                baseFileName = viewModel.fileNameInput
                            )
                            pdfFile != null
                        }
                        else -> false
                    }
                }

                showEnhancedToast(
                    if (success) "📄 Document '${viewModel.fileNameInput}' saved successfully to Downloads!"
                    else "❌ Failed to save document",
                    isSuccess = success
                )
            } catch (e: Exception) {
                showEnhancedToast("❌ Error saving document: ${e.message}", isSuccess = false)
            } finally {
                viewModel.updateLoadingState(false)
            }
        }
    }

    private fun shareScannedDocument() {
        lifecycleScope.launch {
            viewModel.updateLoadingState(true)
            try {
                val fileToShare: File? = withContext(Dispatchers.IO) {
                    when {
                        viewModel.scannedPdf != null -> {
                            copyUriToAppFiles(viewModel.scannedPdf!!.uri, "${viewModel.fileNameInput}.pdf")
                        }
                        viewModel.scannedPages.isNotEmpty() -> {
                            FileUtil.createMultiPagePdf(
                                applicationContext,
                                viewModel.scannedPages,
                                viewModel.fileNameInput
                            )
                        }
                        else -> null
                    }
                }

                if (fileToShare != null) {
                    val shareIntent = FileUtil.shareFile(this@MainActivity, fileToShare, "application/pdf")
                    startActivity(Intent.createChooser(shareIntent, "Share Scanned Document"))
                    showEnhancedToast("📤 Opening share options...", isSuccess = true)
                } else {
                    showEnhancedToast("❌ No document to share", isSuccess = false)
                }
            } catch (e: Exception) {
                showEnhancedToast("❌ Error sharing document: ${e.message}", isSuccess = false)
            } finally {
                viewModel.updateLoadingState(false)
            }
        }
    }

    private fun copyUriToAppFiles(uri: Uri, fileName: String): File? {
        return try {
            val file = File(filesDir, fileName)
            contentResolver.openInputStream(uri)?.use { input ->
                file.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            file
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

    private fun showEnhancedToast(message: String, isSuccess: Boolean = true) {
        Toast.makeText(
            this,
            message,
            if (isSuccess) Toast.LENGTH_SHORT else Toast.LENGTH_LONG
        ).show()
    }
}


fun ComponentActivity.setEdgeToEdge() {
    return when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
        in 6..20 -> setEdgeToEdgeWithDarkIcons()
        else -> setEdgeToEdgeWithLightIcons()
    }
}

@ColorInt
private val transparent: Int = Color.Transparent.toArgb()

fun ComponentActivity.setEdgeToEdgeWithDarkIcons() {
    enableEdgeToEdge(
        statusBarStyle = SystemBarStyle.dark(transparent),
        navigationBarStyle = SystemBarStyle.dark(transparent)
    )
}


fun ComponentActivity.setEdgeToEdgeWithLightIcons() {
    enableEdgeToEdge(
        statusBarStyle = SystemBarStyle.light(transparent, transparent),
        navigationBarStyle = SystemBarStyle.light(transparent, transparent)
    )
}

