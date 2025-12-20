package com.example.digitalhealthkids.ui.home.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.NavigateBefore
import androidx.compose.material.icons.filled.NavigateNext
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.digitalhealthkids.core.util.AppUtils
import com.example.digitalhealthkids.domain.usage.AppDetail
import com.example.digitalhealthkids.ui.home.AppDetailViewModel
import com.example.digitalhealthkids.ui.policy.AddPolicyDialog
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.max

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDetailScreen(
    userId: String,
    packageName: String,
    appName: String,
    category: String,
    onBackClick: () -> Unit,
    onAddPolicy: (String, Int) -> Unit,
    viewModel: AppDetailViewModel = hiltViewModel()
) {
    var showPolicyDialog by remember { mutableStateOf(false) }
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(userId, packageName) {
        viewModel.load(userId, packageName, LocalDate.now())
    }

    val title = AppUtils.getAppName(context, packageName, state.data?.appName)
    val icon = AppUtils.getAppIcon(context, packageName)

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
                            Text(text = category, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
        when {
            state.isLoading -> {
                Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            state.error != null -> {
                Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(state.error ?: "Hata", color = MaterialTheme.colorScheme.error)
                }
            }
            state.data != null -> {
                val detailDate = runCatching { LocalDate.parse(state.data!!.date) }.getOrNull()
                val canGoNext = detailDate?.isBefore(LocalDate.now()) == true
                AppDetailContent(
                    detail = state.data!!,
                    onPrevDay = { viewModel.changeDay(-1) },
                    onNextDay = { if (canGoNext) viewModel.changeDay(1) },
                    onAddPolicy = { limit -> onAddPolicy(packageName, limit) },
                    showPolicyDialog = showPolicyDialog,
                    onTogglePolicyDialog = { showPolicyDialog = it },
                    paddingValues = padding,
                    canGoNext = canGoNext
                )
            }
        }
    }
}

@Composable
private fun AppDetailContent(
    detail: AppDetail,
    onPrevDay: () -> Unit,
    onNextDay: () -> Unit,
    onAddPolicy: (Int) -> Unit,
    showPolicyDialog: Boolean,
    onTogglePolicyDialog: (Boolean) -> Unit,
    paddingValues: PaddingValues,
    canGoNext: Boolean
) {
    val parsedDate = runCatching { LocalDate.parse(detail.date) }.getOrNull()
    val dateLabel = parsedDate?.format(DateTimeFormatter.ofPattern("d MMMM EEEE")) ?: detail.date
    val visibleHours = remember(detail.hourly) { trimHours(detail.hourly) }
    var selectedHour by remember { mutableStateOf<com.example.digitalhealthkids.domain.usage.HourlyUsageDomain?>(null) }

    LazyColumn(
        modifier = Modifier
            .padding(paddingValues)
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { Spacer(Modifier.height(8.dp)) }

        item {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(text = dateLabel, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onPrevDay) { Icon(Icons.Filled.NavigateBefore, contentDescription = "Önceki") }
                IconButton(onClick = onNextDay, enabled = canGoNext) { Icon(Icons.Filled.NavigateNext, contentDescription = "Sonraki") }
            }
        }

        item {
            UsageSummaryCards(detail)
        }

        item {
            HourlyChart(
                data = visibleHours,
                selectedHour = selectedHour,
                onHourSelected = { selectedHour = it }
            )
        }
        
        selectedHour?.let { hour ->
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Saat ${hour.hour}:00 Detayları", style = MaterialTheme.typography.titleMedium)
                        Text("${hour.minutes} dakika kullanım", style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }

        item {
            Button(
                onClick = { onTogglePolicyDialog(true) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Süre Sınırı Koy")
            }
        }

        item { Spacer(Modifier.height(24.dp)) }
    }

    if (showPolicyDialog) {
        AddPolicyDialog(
            onDismiss = { onTogglePolicyDialog(false) },
            onConfirm = { limit ->
                onAddPolicy(limit)
                onTogglePolicyDialog(false)
            }
        )
    }
}

@Composable
private fun UsageSummaryCards(detail: AppDetail) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(modifier = Modifier.weight(1f)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Toplam Kullanım", style = MaterialTheme.typography.titleMedium)
                Text("${detail.totalMinutes} dk", style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold))
            }
        }
        Card(modifier = Modifier.weight(1f)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("En Uzun Oturum", style = MaterialTheme.typography.titleMedium)
                Text("${detail.sessions.maxOfOrNull { it.minutes } ?: 0} dk", style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold))
            }
        }
    }
}


@Composable
private fun HourlyChart(
    data: List<com.example.digitalhealthkids.domain.usage.HourlyUsageDomain>,
    selectedHour: com.example.digitalhealthkids.domain.usage.HourlyUsageDomain?,
    onHourSelected: (com.example.digitalhealthkids.domain.usage.HourlyUsageDomain) -> Unit
) {
    if (data.isEmpty()) {
        Text("Saatlik veri yok", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        return
    }
    val maxMinutes = remember(data) { max(data.maxOfOrNull { it.minutes } ?: 0, 1) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text("Saatlik Dağılım", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(16.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            data.forEach { item ->
                val isSelected = selectedHour?.hour == item.hour
                val color = if (isSelected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
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
                        fontWeight = if(isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

private fun formatTime(iso: String): String = try {
    OffsetDateTime.parse(iso).toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm"))
} catch (_: Exception) { iso }

private fun trimHours(data: List<com.example.digitalhealthkids.domain.usage.HourlyUsageDomain>): List<com.example.digitalhealthkids.domain.usage.HourlyUsageDomain> {
    if (data.isEmpty()) return emptyList()
    val first = data.indexOfFirst { it.minutes > 0 }
    val last = data.indexOfLast { it.minutes > 0 }
    if (first == -1 || last == -1) return data.take(8)
    val start = max(0, first - 2)
    val end = minOf(23, last + 2)
    return data.subList(start, end + 1)
}
