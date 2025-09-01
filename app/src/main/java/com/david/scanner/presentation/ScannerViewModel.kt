package com.david.scanner.presentation

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult

class ScannerViewModel : ViewModel() {

    var scannedPdf by mutableStateOf<GmsDocumentScanningResult.Pdf?>(null)
        private set

    var scannedPages by mutableStateOf<List<Uri>>(emptyList())
        private set

    var previewImageUri by mutableStateOf<Uri?>(null)
        private set

    var fileNameInput by mutableStateOf("ScannedDocument")
        private set

    var isLoading by mutableStateOf(false)
        private set

    fun updateFileName(newName: String) {
        fileNameInput = newName
    }

    fun updateScanResult(
        pdf: GmsDocumentScanningResult.Pdf?,
        pages: List<Uri>
    ) {
        scannedPdf = pdf
        scannedPages = pages
        previewImageUri = pages.firstOrNull()
    }

    fun updateLoadingState(loading: Boolean) {
        isLoading = loading
    }

    fun clearScanResult() {
        scannedPdf = null
        scannedPages = emptyList()
        previewImageUri = null
    }
}
