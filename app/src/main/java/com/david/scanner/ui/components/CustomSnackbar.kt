package com.david.scanner.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

enum class MessageType {
    SUCCESS, ERROR, INFO
}

@Composable
fun CustomSnackbar(
    message: String,
    messageType: MessageType = MessageType.INFO,
    isVisible: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(isVisible) {
        if (isVisible) {
            delay(3000)
            onDismiss()
        }
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        modifier = modifier
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = when (messageType) {
                    MessageType.SUCCESS -> MaterialTheme.colorScheme.secondaryContainer
                    MessageType.ERROR -> MaterialTheme.colorScheme.errorContainer
                    MessageType.INFO -> MaterialTheme.colorScheme.primaryContainer
                }
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = when (messageType) {
                        MessageType.SUCCESS -> Icons.Default.CheckCircle
                        MessageType.ERROR -> Icons.Default.Warning
                        MessageType.INFO -> Icons.Default.Info
                    },
                    contentDescription = null,
                    tint = when (messageType) {
                        MessageType.SUCCESS -> MaterialTheme.colorScheme.onSecondaryContainer
                        MessageType.ERROR -> MaterialTheme.colorScheme.onErrorContainer
                        MessageType.INFO -> MaterialTheme.colorScheme.onPrimaryContainer
                    }
                )

                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = when (messageType) {
                        MessageType.SUCCESS -> MaterialTheme.colorScheme.onSecondaryContainer
                        MessageType.ERROR -> MaterialTheme.colorScheme.onErrorContainer
                        MessageType.INFO -> MaterialTheme.colorScheme.onPrimaryContainer
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
