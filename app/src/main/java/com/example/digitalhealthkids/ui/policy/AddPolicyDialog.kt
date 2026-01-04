package com.example.digitalhealthkids.ui.policy

import androidx.compose.foundation.layout.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPolicyDialog(
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    val hours = (0..6).toList()
    val minutes = (0..55 step 5).toList()
    var selectedHour by remember { mutableStateOf(0) }
    var selectedMinute by remember { mutableStateOf(30) }
    var showHourMenu by remember { mutableStateOf(false) }
    var showMinuteMenu by remember { mutableStateOf(false) }
    var isError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Günlük Süre Sınırı") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Bu uygulama için günlük süre sınırı belirle.")
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ExposedDropdownMenuBox(
                        expanded = showHourMenu,
                        onExpandedChange = { showHourMenu = !showHourMenu },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = "${selectedHour} saat",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Saat") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showHourMenu) },
                            modifier = Modifier.menuAnchor()
                        )
                        DropdownMenu(expanded = showHourMenu, onDismissRequest = { showHourMenu = false }) {
                            hours.forEach { h ->
                                DropdownMenuItem(
                                    text = { Text("$h saat") },
                                    onClick = {
                                        selectedHour = h
                                        showHourMenu = false
                                        isError = false
                                    }
                                )
                            }
                        }
                    }

                    ExposedDropdownMenuBox(
                        expanded = showMinuteMenu,
                        onExpandedChange = { showMinuteMenu = !showMinuteMenu },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = "${selectedMinute} dk",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Dakika") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showMinuteMenu) },
                            modifier = Modifier.menuAnchor()
                        )
                        DropdownMenu(expanded = showMinuteMenu, onDismissRequest = { showMinuteMenu = false }) {
                            minutes.forEach { m ->
                                DropdownMenuItem(
                                    text = { Text("$m dk") },
                                    onClick = {
                                        selectedMinute = m
                                        showMinuteMenu = false
                                        isError = false
                                    }
                                )
                            }
                        }
                    }
                }
                if (isError) {
                    Text("Lütfen 0'dan büyük bir süre seçin", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val total = selectedHour * 60 + selectedMinute
                    if (total > 0) onConfirm(total) else isError = true
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