package com.example.digitalhealthkids.ui.policy

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

@Composable
fun AddPolicyDialog(
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var textState by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Günlük Süre Sınırı") },
        text = {
            Column {
                Text("Bu uygulama için kaç dakika izin verilsin?")
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = textState,
                    onValueChange = {
                        textState = it
                        isError = false
                    },
                    label = { Text("Dakika") },
                    isError = isError,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
                if (isError) {
                    Text("Lütfen geçerli bir sayı girin", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val minutes = textState.toIntOrNull()
                    if (minutes != null && minutes > 0) {
                        onConfirm(minutes)
                    } else {
                        isError = true
                    }
                }
            ) {
                Text("Kaydet")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("İptal")
            }
        }
    )
}