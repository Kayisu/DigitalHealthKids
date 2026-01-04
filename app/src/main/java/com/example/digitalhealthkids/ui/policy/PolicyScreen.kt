package com.example.digitalhealthkids.ui.policy

import android.app.TimePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.digitalhealthkids.core.network.policy.PolicyResponseDto
import com.example.digitalhealthkids.ui.components.PullToRefreshLayout // <-- Wrapper Import
import com.example.digitalhealthkids.core.util.AppUtils

@Composable
fun PolicyScreen(
    userId: String,
    onNavigateToAutoPolicy: () -> Unit,
    viewModel: PolicyViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(userId) {
        viewModel.loadPolicy(userId)
    }

    Scaffold { padding ->
        // PullToRefreshLayout ile sarmaladık
        PullToRefreshLayout(
            modifier = Modifier.padding(padding),
            isLoading = state.isLoading,
            errorMessage = state.error, // ViewModel'da error alanı varsa buraya bağla
            onRefresh = {
                viewModel.loadPolicy(userId)
            }
        ) {
            // Veri varsa içeriği göster
            if (state.policy != null) {
                PolicyContent(
                    policy = state.policy!!,
                    isLoading = state.isLoading,
                    onSave = { limit, start, end, weekendRelax ->
                        viewModel.updateSettings(userId, limit, start, end, weekendRelax)
                    },
                    onNavigateToAutoPolicy = onNavigateToAutoPolicy
                )
            }
        }
    }
}

@Composable
fun PolicyContent(
    policy: PolicyResponseDto,
    isLoading: Boolean,
    onSave: (Int?, String?, String?, Int) -> Unit,
    onNavigateToAutoPolicy: () -> Unit
) {
    // ÖNEMLİ DÜZELTME: remember(policy) kullanıldı.
    // Böylece refresh yapıldığında form değerleri sunucudan gelen yeni veriyle sıfırlanır.

    // 1. LIMIT STATE
    var isLimitEnabled by remember(policy) { mutableStateOf(policy.dailyLimitMinutes != null) }
    var limitMinutes by remember(policy) { mutableFloatStateOf(policy.dailyLimitMinutes?.toFloat() ?: 120f) }

    // 2. BEDTIME STATE
    var isBedtimeEnabled by remember(policy) { mutableStateOf(policy.bedtime != null) }
    var bedtimeStart by remember(policy) { mutableStateOf(policy.bedtime?.start ?: "21:30") }
    var bedtimeEnd by remember(policy) { mutableStateOf(policy.bedtime?.end ?: "07:00") }

    var weekendRelaxPct by remember(policy) { mutableStateOf(policy.weekendExtraMinutes) }

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
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Lightbulb, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Column {
                            Text("Önerilen Politika", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                    FilledTonalButton(
                        onClick = onNavigateToAutoPolicy,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.18f),
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text("Öneriyi gör")
                    }
                }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "Kısıtlamalar",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    "Günlük limit, uyku saati ve hafta sonu esnekliğini buradan yönet.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // --- GÜNLÜK LİMİT KARTI ---
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Timer, null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(8.dp))
                            Text("Günlük Limit", style = MaterialTheme.typography.titleMedium)
                        }
                        Switch(
                            checked = isLimitEnabled,
                            onCheckedChange = { isLimitEnabled = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.primary,
                                checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                            )
                        )
                    }

                    if (isLimitEnabled) {
                        Spacer(Modifier.height(10.dp))
                        Text(
                            "${limitMinutes.toInt()} dakika",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Slider(
                            value = limitMinutes,
                            onValueChange = { limitMinutes = it },
                            valueRange = 15f..240f,
                            steps = 44,
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary,
                                inactiveTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                            )
                        )
                    } else {
                        Spacer(Modifier.height(6.dp))
                        Text("Süre sınırı yok (Sınırsız)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        // --- UYKU VAKTİ KARTI ---
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Bedtime, null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(8.dp))
                            Text("Uyku Vakti", style = MaterialTheme.typography.titleMedium)
                        }
                        Switch(
                            checked = isBedtimeEnabled,
                            onCheckedChange = { isBedtimeEnabled = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.primary,
                                checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                            )
                        )
                    }

                    if (isBedtimeEnabled) {
                        Spacer(Modifier.height(10.dp))
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
                    } else {
                        Spacer(Modifier.height(6.dp))
                        Text("Uyku saati kısıtlaması yok", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        // --- HAFTA SONU ESNEKLİĞİ ---
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Timer, null, tint = MaterialTheme.colorScheme.primary)
                        Text("Hafta Sonu / Tatil Esnekliği", style = MaterialTheme.typography.titleMedium)
                    }
                    Text(
                        "Hafta sonu limitini yumuşat: %$weekendRelaxPct",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Slider(
                        value = weekendRelaxPct.toFloat(),
                        onValueChange = { weekendRelaxPct = it.toInt() },
                        valueRange = 0f..50f,
                        steps = 10,
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary,
                            inactiveTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                        )
                    )
                    Text("0-50% arası önerilir. Tatil günleri için de uygulanır.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        // --- ENGELLENENLER ---
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Block, null, tint = MaterialTheme.colorScheme.error)
                        Text("Engellenenler", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.weight(1f))
                        Text("${policy.blockedApps.size} adet", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (policy.blockedApps.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            policy.blockedApps.forEach { pkg ->
                                val displayName = AppUtils.getAppName(context, pkg, pkg.substringAfterLast('.').replaceFirstChar { it.uppercase() })
                                Surface(
                                    color = MaterialTheme.colorScheme.surface,
                                    shape = MaterialTheme.shapes.small
                                ) {
                                    Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Text(displayName, style = MaterialTheme.typography.bodyMedium)
                                    }
                                }
                            }
                        }
                    } else {
                        Text("Yok", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        // --- KAYDET BUTONU ---
        item {
            Button(
                onClick = {
                    val finalLimit = if (isLimitEnabled) limitMinutes.toInt() else null
                    val finalStart = if (isBedtimeEnabled) bedtimeStart else null
                    val finalEnd = if (isBedtimeEnabled) bedtimeEnd else null

                    onSave(finalLimit, finalStart, finalEnd, weekendRelaxPct)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp),
                enabled = !isLoading,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    contentColor = MaterialTheme.colorScheme.primary
                )
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