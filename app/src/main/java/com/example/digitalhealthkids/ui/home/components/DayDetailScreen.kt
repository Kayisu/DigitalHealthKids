package com.example.digitalhealthkids.ui.home.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
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
import androidx.navigation.NavController
import com.example.digitalhealthkids.core.util.AppUtils
import com.example.digitalhealthkids.core.util.formatDuration
import com.example.digitalhealthkids.domain.usage.DailyStat
import com.example.digitalhealthkids.ui.home.HomeViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun DailyDetailScreen(
    userId: String,   // 🔥 GÜNCELLENDİ: childId -> userId
    deviceId: String, // 🔥 EKLENDİ
    initialDayIndex: Int,
    navController: NavController,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    // 🔥 Sayfa açılınca veriyi yeniden çek
    LaunchedEffect(userId) {
        if (state.data == null) {
            viewModel.syncUsageHistory(context, userId, deviceId)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Günlük Detay", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            if (state.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (state.data != null) {
                val dashboard = state.data!!

                // Güvenlik kontrolü: Index liste dışına çıkmasın
                val safeInitialPage = initialDayIndex.coerceIn(0, (dashboard.weeklyBreakdown.size - 1).coerceAtLeast(0))

                val pagerState = rememberPagerState(
                    initialPage = safeInitialPage,
                    pageCount = { dashboard.weeklyBreakdown.size }
                )

                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { pageIndex ->
                    val stat = dashboard.weeklyBreakdown[pageIndex]
                    DailyContentPage(stat = stat)
                }
            } else {
                Text(
                    "Veri yüklenemedi",
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
fun DailyContentPage(stat: DailyStat) {
    val context = LocalContext.current

    // Tarih Formatlama (2 Aralık Salı)
    val dateLabel = try {
        val date = LocalDate.parse(stat.date)
        date.format(DateTimeFormatter.ofPattern("d MMMM EEEE", Locale("tr")))
    } catch (e: Exception) { stat.date }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. HEADER
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = dateLabel,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = formatDuration(stat.totalMinutes),
                        style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    val progress = (stat.totalMinutes / 240f).coerceIn(0f, 1f)
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(CircleShape),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.3f),
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Günlük hedef (4 sa) kullanımı", style = MaterialTheme.typography.labelSmall)
                }
            }
        }

        // 2. LİSTE BAŞLIĞI
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AccessTime, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text(
                    "Uygulama Geçmişi",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        }

        // 3. LİSTE
        items(stat.apps) { app ->
            val cleanName = AppUtils.getAppName(context, app.packageName, app.appName)
            AppUsageRowItem(
                name = cleanName,
                packageName = app.packageName,
                minutes = app.minutes
            )
        }

        item {
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
fun AppUsageRowItem(name: String, packageName: String, minutes: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AppIcon(packageName = packageName, modifier = Modifier.size(48.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Surface(
            color = MaterialTheme.colorScheme.secondaryContainer,
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                text = formatDuration(minutes),
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}