package com.david.scanner.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.david.scanner.presentation.ScannerViewModel
import com.david.scanner.ui.components.ActionButtons
import com.david.scanner.ui.components.CustomSnackbar
import com.david.scanner.ui.components.DocumentPreview
import com.david.scanner.ui.components.DocumentStats
import com.david.scanner.ui.components.EmptyState
import com.david.scanner.ui.components.EnhancedScannerTopBar
import com.david.scanner.ui.components.FileNameInput
import com.david.scanner.ui.components.FloatingActionMenu
import com.david.scanner.ui.components.LoadingOverlay
import com.david.scanner.ui.components.MessageType
import com.david.scanner.ui.components.ScanButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScannerScreen(
    onScanClick: () -> Unit,
    onSaveClick: () -> Unit,
    onShareClick: () -> Unit,
    viewModel: ScannerViewModel = viewModel(),
) {
    val scrollState = rememberScrollState()
    var snackbarMessage by remember { mutableStateOf<String?>(null) }
    var messageType by remember { mutableStateOf(MessageType.INFO) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .statusBarsPadding(),
    ) {
        Scaffold(
            topBar = {
                EnhancedScannerTopBar(
                    onSettingsClick = {
                        snackbarMessage = "Settings coming soon!"
                        messageType = MessageType.INFO
                    },
                    onInfoClick = {
                        snackbarMessage = "Scanner App v1.0 - Scan, Save & Share documents easily!"
                        messageType = MessageType.INFO
                    }
                )
            },
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .verticalScroll(scrollState),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(26.dp)
            ) {

                if (viewModel.previewImageUri == null) {
                    Spacer(modifier = Modifier.height(40.dp))
                }


                // Document preview or empty state
                if (viewModel.previewImageUri != null) {
                    DocumentPreview(
                        previewImageUri = viewModel.previewImageUri,
                        scannedPages = viewModel.scannedPages
                    )

                    // Document statistics
                    DocumentStats(
                        pageCount = viewModel.scannedPages.size,
                        hasPages = viewModel.scannedPages.isNotEmpty(),
                        hasPdf = viewModel.scannedPdf != null
                    )
                } else {
                    EmptyState()
                }

                if (viewModel.previewImageUri != null) {
                    FileNameInput(
                        fileName = viewModel.fileNameInput,
                        onFileNameChange = viewModel::updateFileName
                    )
                }
                // Scan button
                ScanButton(
                    onClick = onScanClick,
                    isLoading = viewModel.isLoading,
                    viewModel.previewImageUri != null
                )
                // Action buttons (Save & Share)
                ActionButtons(
                    isEnabled = viewModel.scannedPages.isNotEmpty() && !viewModel.isLoading,
                    onSaveClick = onSaveClick,
                    onShareClick = onShareClick
                )

                // Add some bottom spacing for better scrolling
                Spacer(modifier = Modifier.height(80.dp)) // Extra space for FAB
            }
        }

        // Loading overlay
        LoadingOverlay(
            isLoading = viewModel.isLoading,
            loadingText = when {
                viewModel.isLoading && viewModel.scannedPages.isEmpty() -> "Scanning document..."
                viewModel.isLoading -> "Processing..."
                else -> "Loading..."
            }
        )

        // Custom snackbar at the bottom
        CustomSnackbar(
            message = snackbarMessage ?: "",
            messageType = messageType,
            isVisible = snackbarMessage != null,
            onDismiss = { snackbarMessage = null },
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}
