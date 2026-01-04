package com.example.digitalhealthkids.ui.home.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.NavigateBefore
import androidx.compose.material.icons.filled.NavigateNext
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.digitalhealthkids.core.util.AppUtils
import com.example.digitalhealthkids.core.util.CategoryLabels
import com.example.digitalhealthkids.domain.usage.AppDetail
import com.example.digitalhealthkids.domain.usage.HourlyUsageDomain
import com.example.digitalhealthkids.ui.components.PullToRefreshLayout
import com.example.digitalhealthkids.ui.home.AppDetailViewModel
import com.example.digitalhealthkids.ui.policy.AddPolicyDialog
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.max

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDetailScreen(
    userId: String,
    packageName: String,
    appName: String,
    onBackClick: () -> Unit,
    onAddPolicy: (String, Int) -> Unit,
    viewModel: AppDetailViewModel = hiltViewModel()
) {
    var showPolicyDialog by remember { mutableStateOf(false) }
    var showBlockedDialog by remember { mutableStateOf(false) }
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    // İlk Yükleme
    LaunchedEffect(userId, packageName) {
        viewModel.load(userId, packageName, LocalDate.now())
    }

    val title = AppUtils.getAppName(context, packageName, state.data?.appName ?: appName)
    val icon = AppUtils.getAppIcon(context, packageName)

    // Kategori State'den geliyor (Backend verisi)
    val categoryText = CategoryLabels.labelFor(state.data?.category)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (icon != null) {
                            Image(
                                painter = rememberDrawablePainter(drawable = icon),
                                contentDescription = null,
                                modifier = Modifier.size(32.dp)
                            )
                        } else {
                            Icon(Icons.Default.PhoneAndroid, contentDescription = null, modifier = Modifier.size(24.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(text = title, style = MaterialTheme.typography.titleLarge)
                            Text(
                                text = categoryText,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Gray
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
                    }
                }
            )
        }
    ) { padding ->
        // PullToRefreshLayout Kullanımı
        // Manuel state yönetimi, scope, isRefreshing vb. hepsi wrapper içinde gizlendi.
        PullToRefreshLayout(
            modifier = Modifier.padding(padding),
            isLoading = state.isLoading,
            errorMessage = state.error,
            onRefresh = {
                // Aynı günü koruyarak yeniden çek
                viewModel.load(userId, packageName, state.date)
            }
        ) {
            // Sadece veri varsa içeriği gösteriyoruz
            if (state.data != null) {
                val detailDate = runCatching { LocalDate.parse(state.data!!.date) }.getOrNull()
                val canGoNext = detailDate?.isBefore(LocalDate.now()) == true

                AppDetailContent(
                    detail = state.data!!,
                    onPrevDay = { viewModel.changeDay(-1) },
                    onNextDay = { if (canGoNext) viewModel.changeDay(1) },
                    onAddPolicy = { limit -> onAddPolicy(packageName, limit) },
                    showPolicyDialog = showPolicyDialog,
                    onTogglePolicyDialog = { showPolicyDialog = it },
                    canGoNext = canGoNext,
                    appTitle = title
                )
            }
        }
    }
}

// ... AppDetailContent ve diğer yardımcı composable'lar AYNI KALACAK ...
// (Aşağıdaki kodların değişmesine gerek yok, olduğu gibi durabilir)

@Composable
private fun AppDetailContent(
    detail: AppDetail,
    onPrevDay: () -> Unit,
    onNextDay: () -> Unit,
    onAddPolicy: (Int) -> Unit,
    showPolicyDialog: Boolean,
    onTogglePolicyDialog: (Boolean) -> Unit,
    canGoNext: Boolean,
    appTitle: String
) {
    val parsedDate = runCatching { LocalDate.parse(detail.date) }.getOrNull()
    val dateLabel = parsedDate?.format(DateTimeFormatter.ofPattern("d MMMM EEEE")) ?: detail.date

    val visibleHours = remember(detail.hourly) { trimHours(detail.hourly) }
    var selectedHour by remember { mutableStateOf<HourlyUsageDomain?>(null) }
    val prefs = LocalContext.current.getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
    var isBlocked by remember(detail.packageName) {
        mutableStateOf(prefs.getStringSet("blocked_packages", emptySet())?.contains(detail.packageName) == true)
    }
    var currentLimit by remember { mutableStateOf<Int?>(prefs.getInt("daily_limit", -1).takeIf { it >= 0 }) }
    var showBlockedDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { Spacer(Modifier.height(6.dp)) }

        // Tarih Navigasyonu
        item {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(text = dateLabel, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onPrevDay) { Icon(Icons.Filled.NavigateBefore, contentDescription = "Önceki") }
                IconButton(onClick = onNextDay, enabled = canGoNext) { Icon(Icons.Filled.NavigateNext, contentDescription = "Sonraki") }
            }
        }

        // Özet Kartları
        item {
            UsageSummaryCards(detail)
        }

        // Grafik
        item {
            HourlyChart(
                data = visibleHours,
                selectedHour = selectedHour,
                onHourSelected = { selectedHour = it }
            )
        }

        // Seçilen Saat Detayı
        selectedHour?.let { hour ->
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.16f))
                ) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = appTitle,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            "Kullanım: ${formatDurationHhMm(hour.minutes)}",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }

        // Süre Sınırı Kartı
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Kısıtlamalar", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(
                        "Günlük süre sınırı koyabilir veya uygulamayı engelleyebilirsin.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (currentLimit != null) {
                        val remaining = (currentLimit!! - detail.totalMinutes).coerceAtLeast(0)
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.16f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(Icons.Default.Timer, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    Text("Günlük limit: ${formatDurationHhMm(currentLimit!!)}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                                }
                                Text("Kalan: ${formatDurationHhMm(remaining)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        OutlinedButton(
                            onClick = {
                                currentLimit = null
                                onAddPolicy(-2) // daily limit kaldır
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Süre sınırını kaldır")
                        }
                    }
                    FilledTonalButton(
                        onClick = { onTogglePolicyDialog(true) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.18f),
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text(if (currentLimit == null) "Süre Sınırı Koy" else "Süreyi Güncelle")
                    }
                    Button(
                        onClick = {
                            if (isBlocked) {
                                onAddPolicy(-1) // unblock
                                isBlocked = false
                                showBlockedDialog = false
                            } else {
                                onAddPolicy(0) // block
                                isBlocked = true
                                currentLimit = null
                                showBlockedDialog = true
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = if (isBlocked)
                            ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.18f),
                                contentColor = MaterialTheme.colorScheme.primary
                            )
                        else ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text(if (isBlocked) "Engeli Kaldır" else "Engelle")
                    }
                }
            }
        }

        item { Spacer(Modifier.height(24.dp)) }
    }

    if (showPolicyDialog) {
        AddPolicyDialog(
            onDismiss = { onTogglePolicyDialog(false) },
            onConfirm = { limit ->
                onAddPolicy(limit)
                currentLimit = limit
                isBlocked = false
                onTogglePolicyDialog(false)
            }
        )
    }

    if (showBlockedDialog) {
        BlockedAppDialog(
            appName = appTitle,
            onDismiss = { showBlockedDialog = false },
            onOpenPolicy = {
                showBlockedDialog = false
                onTogglePolicyDialog(true)
            }
        )
    }
}

@Composable
private fun BlockedAppDialog(
    appName: String,
    onDismiss: () -> Unit,
    onOpenPolicy: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Surface(
                tonalElevation = 2.dp,
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.errorContainer
            ) {
                Icon(
                    imageVector = Icons.Filled.Block,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(10.dp)
                )
            }
        },
        title = { Text("Uygulama Engellendi", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "$appName şu an engelli.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Ayarları düzenleyerek açabilir veya süre sınırı koyabilirsin.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(onClick = onOpenPolicy, shape = RoundedCornerShape(10.dp)) {
                Text("Süre Sınırı Koy")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Kapat")
            }
        },
        shape = RoundedCornerShape(20.dp)
    )
}

// ... UsageSummaryCards, HourlyChart, trimHours fonksiyonları aynı ...
@Composable
private fun UsageSummaryCards(detail: AppDetail) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(
            modifier = Modifier.weight(1f),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text("Toplam", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                Text("Kullanım", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(6.dp))
                Text(formatDurationHhMm(detail.totalMinutes), style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
            }
        }
        Card(
            modifier = Modifier.weight(1f),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text("Gece", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                Text("Kullanımı", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(6.dp))
                Text(formatDurationHhMm(detail.nightMinutes), style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

@Composable
private fun HourlyChart(
    data: List<HourlyUsageDomain>,
    selectedHour: HourlyUsageDomain?,
    onHourSelected: (HourlyUsageDomain) -> Unit
) {
    if (data.isEmpty()) {
        Text("Saatlik veri yok", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        return
    }
    val maxMinutes = remember(data) { max(data.maxOfOrNull { it.minutes } ?: 0, 1) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text("Saatlik Dağılım", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
        Text(
            "Tıklayarak saat detayını seç",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            data.forEach { item ->
                val isSelected = selectedHour?.hour == item.hour
                val color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)

                val ratio = item.minutes / maxMinutes.toFloat()
                val barHeight = max(8f, ratio * 160f)

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable { onHourSelected(item) }
                ) {
                    Box(
                        modifier = Modifier
                            .width(12.dp)
                            .height(barHeight.dp)
                            .background(color, shape = MaterialTheme.shapes.small)
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = item.hour.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

private fun trimHours(data: List<HourlyUsageDomain>): List<HourlyUsageDomain> {
    if (data.isEmpty()) return emptyList()
    val first = data.indexOfFirst { it.minutes > 0 }
    val last = data.indexOfLast { it.minutes > 0 }
    if (first == -1 || last == -1) return data.take(12)
    val start = max(0, first - 2)
    val end = kotlin.math.min(23, last + 2)
    return data.subList(start, end + 1)
}

private fun formatDurationHhMm(totalMinutes: Int): String {
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return when {
        hours > 0 && minutes > 0 -> "${hours} sa ${minutes} dk"
        hours > 0 -> "${hours} sa"
        else -> "${minutes} dk"
    }
}