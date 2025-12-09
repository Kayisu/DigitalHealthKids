package com.example.digitalhealthkids.ui.policy

import android.app.TimePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.digitalhealthkids.core.network.policy.PolicyResponseDto
import java.util.Calendar

@Composable
fun PolicyScreen(
    userId: String,
    viewModel: PolicyViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(userId) {
        viewModel.loadPolicy(userId)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (state.isLoading && state.policy == null) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        } else if (state.policy != null) {
            PolicyContent(
                policy = state.policy!!,
                isLoading = state.isLoading,
                onSave = { limit, start, end ->
                    viewModel.updateSettings(userId, limit, start, end)
                }
            )
        }
    }
}

@Composable
fun PolicyContent(
    policy: PolicyResponseDto,
    isLoading: Boolean,
    onSave: (Int, String, String) -> Unit
) {
    var limitMinutes by remember { mutableFloatStateOf(policy.dailyLimitMinutes.toFloat()) }
    var bedtimeStart by remember { mutableStateOf(policy.bedtime?.start ?: "21:30") }
    var bedtimeEnd by remember { mutableStateOf(policy.bedtime?.end ?: "07:00") }

    val context = LocalContext.current

    fun showTimePicker(current: String, onTimeSelected: (String) -> Unit) {
        val parts = current.split(":")
        val h = parts.getOrNull(0)?.toIntOrNull() ?: 12
        val m = parts.getOrNull(1)?.toIntOrNull() ?: 0

        TimePickerDialog(context, { _, hour, minute ->
            onTimeSelected(String.format("%02d:%02d", hour, minute))
        }, h, m, true).show()
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                "Kısıtlamalar",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
            )
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Timer, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Text("Günlük Limit", style = MaterialTheme.typography.titleMedium)
                    }
                    Spacer(Modifier.height(16.dp))

                    Text(
                        "${limitMinutes.toInt()} dakika",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Slider(
                        value = limitMinutes,
                        onValueChange = { limitMinutes = it },
                        valueRange = 0f..240f,
                        steps = 23
                    )
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Bedtime, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Text("Uyku Vakti", style = MaterialTheme.typography.titleMedium)
                    }
                    Spacer(Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TimeSelector("Başlangıç", bedtimeStart) {
                            showTimePicker(bedtimeStart) { bedtimeStart = it }
                        }
                        TimeSelector("Bitiş", bedtimeEnd) {
                            showTimePicker(bedtimeEnd) { bedtimeEnd = it }
                        }
                    }
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Block, null, tint = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.width(8.dp))
                        Text("Engellenenler", style = MaterialTheme.typography.titleMedium)
                    }
                    Spacer(Modifier.height(8.dp))
                    if (policy.blockedApps.isNotEmpty()) {
                        policy.blockedApps.forEach { pkg ->
                            Text("• $pkg", style = MaterialTheme.typography.bodyMedium)
                        }
                    } else {
                        Text("Yok", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }

        item {
            Button(
                onClick = { onSave(limitMinutes.toInt(), bedtimeStart, bedtimeEnd) },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                enabled = !isLoading,
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(24.dp))
                } else {
                    Text("Kaydet")
                }
            }
        }
    }
}

@Composable
fun TimeSelector(label: String, time: String, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .clickable { onClick() }
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium)
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.AccessTime, null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text(
                time,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}