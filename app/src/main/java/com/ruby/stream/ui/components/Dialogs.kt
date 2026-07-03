package com.ruby.stream.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ruby.stream.ui.theme.Crimson
import com.ruby.stream.ui.theme.RubyRadius
import com.ruby.stream.ui.theme.RubySpacing
import com.ruby.stream.ui.theme.RubyType
import com.ruby.stream.ui.theme.StatusError
import com.ruby.stream.ui.theme.StatusWarning
import com.ruby.stream.ui.theme.Surface3
import com.ruby.stream.ui.theme.TextPrimary
import com.ruby.stream.ui.theme.TextSecondary

// ── PIN Dialog ─────────────────────────────────────────────
@Composable
fun PinDialog(
    title: String,
    pinLength: Int = 4,
    currentInput: String,
    onDigitPress: (String) -> Unit,
    onBackspace: () -> Unit,
    onDismiss: () -> Unit,
    onForgotPin: (() -> Unit)? = null,
    errorMessage: String? = null
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Surface3,
        confirmButton = {},
        title = null,
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = title, style = RubyType.H3, color = TextPrimary)
                Spacer(Modifier.height(RubySpacing.md))

                Row(horizontalArrangement = Arrangement.spacedBy(RubySpacing.sm)) {
                    repeat(pinLength) { index ->
                        val filled = index < currentInput.length
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(if (filled) Crimson else TextSecondary)
                        )
                    }
                }

                if (errorMessage != null) {
                    Spacer(Modifier.height(RubySpacing.sm))
                    Text(text = errorMessage, style = RubyType.Caption, color = StatusError)
                }

                Spacer(Modifier.height(RubySpacing.lg))

                val keys = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "", "0", "⌫")
                Column(verticalArrangement = Arrangement.spacedBy(RubySpacing.sm)) {
                    keys.chunked(3).forEach { rowKeys ->
                        Row(horizontalArrangement = Arrangement.spacedBy(RubySpacing.lg)) {
                            rowKeys.forEach { key ->
                                val interactionSource = remember { MutableInteractionSource() }
                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(CircleShape)
                                        .then(
                                            if (key.isNotEmpty()) {
                                                Modifier.clickable(
                                                    interactionSource = interactionSource,
                                                    indication = null,
                                                    onClick = {
                                                        when (key) {
                                                            "⌫" -> onBackspace()
                                                            else -> onDigitPress(key)
                                                        }
                                                    }
                                                )
                                            } else Modifier
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    when (key) {
                                        "⌫" -> Icon(
                                            imageVector = Icons.Filled.Backspace,
                                            contentDescription = "Backspace",
                                            tint = TextPrimary
                                        )
                                        "" -> {}
                                        else -> Text(text = key, style = RubyType.H2, color = TextPrimary)
                                    }
                                }
                            }
                        }
                    }
                }

                if (onForgotPin != null) {
                    Spacer(Modifier.height(RubySpacing.md))
                    Text(
                        text = "Forgot PIN?",
                        style = RubyType.Caption,
                        color = TextSecondary,
                        modifier = Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onForgotPin
                        )
                    )
                }
            }
        }
    )
}

// ── Delete Confirmation Dialog ──────────────────────────────
@Composable
fun DeleteDialog(
    title: String,
    message: String,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    confirmLabel: String = "Delete"
) {
    AlertDialog(
        onDismissRequest = onCancel,
        containerColor = Surface3,
        title = { Text(text = title, style = RubyType.H3, color = TextPrimary) },
        text = { Text(text = message, style = RubyType.Body, color = TextSecondary) },
        confirmButton = {
            RubyButton(
                text = confirmLabel,
                onClick = onConfirm,
                variant = RubyButtonVariant.Primary
            )
        },
        dismissButton = {
            RubyButton(
                text = "Cancel",
                onClick = onCancel,
                variant = RubyButtonVariant.Tertiary
            )
        }
    )
}

// ── Network Error Dialog ────────────────────────────────────
@Composable
fun NetworkErrorDialog(
    title: String,
    message: String,
    onRetry: (() -> Unit)? = null,
    onDismiss: () -> Unit,
    severity: NetworkErrorSeverity = NetworkErrorSeverity.Error
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Surface3,
        icon = {
            Icon(
                imageVector = if (severity == NetworkErrorSeverity.Error) Icons.Filled.Error else Icons.Filled.Warning,
                contentDescription = null,
                tint = if (severity == NetworkErrorSeverity.Error) StatusError else StatusWarning
            )
        },
        title = { Text(text = title, style = RubyType.H3, color = TextPrimary, textAlign = TextAlign.Center) },
        text = { Text(text = message, style = RubyType.Body, color = TextSecondary, textAlign = TextAlign.Center) },
        confirmButton = {
            if (onRetry != null) {
                RubyButton(text = "Retry", onClick = onRetry, variant = RubyButtonVariant.Primary)
            } else {
                RubyButton(text = "Dismiss", onClick = onDismiss, variant = RubyButtonVariant.Primary)
            }
        },
        dismissButton = if (onRetry != null) {
            { RubyButton(text = "Dismiss", onClick = onDismiss, variant = RubyButtonVariant.Tertiary) }
        } else null
    )
}

enum class NetworkErrorSeverity { Warning, Error }
