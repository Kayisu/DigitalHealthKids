package com.example.digitalhealthkids.ui.home.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.digitalhealthkids.core.util.AppUtils
import com.example.digitalhealthkids.core.util.formatDuration
import com.example.digitalhealthkids.domain.usage.DashboardData
import androidx.compose.ui.platform.LocalContext

@Composable
fun AppsPage(dashboard: DashboardData) {
    val context = LocalContext.current

    // Tüm haftanın uygulama verilerini birleştir ve sırala
    val allApps = dashboard.weeklyBreakdown.flatMap { it.apps }
    val mergedApps = allApps
        .groupBy { it.packageName }
        .map { (_, list) ->
            list.first().copy(minutes = list.sumOf { it.minutes })
        }
        .sortedByDescending { it.minutes }
    // İstersen limit koyabilirsin .take(20) gibi

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                "En Çok Kullanılanlar",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
            Text(
                "Bu hafta toplam kullanım istatistikleri",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        items(mergedApps) { app ->
            val cleanName = AppUtils.getAppName(context, app.packageName, app.appName)

            // Daha önce DayDetailScreen için yazdığımız satır tasarımını burada da kullanabiliriz
            // Veya buraya özel basit bir kart yapabiliriz. Şimdilik AppUsageRowItem kullanalım (Reuse!)
            AppUsageRowItem(
                name = cleanName,
                packageName = app.packageName,
                minutes = app.minutes
            )
        }

        if (mergedApps.isEmpty()) {
            item {
                Text("Henüz kullanım verisi yok.")
            }
        }
    }
}