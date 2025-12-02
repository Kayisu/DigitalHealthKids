package com.example.digitalhealthkids.ui.home.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.digitalhealthkids.domain.usage.DashboardData
import com.example.digitalhealthkids.core.util.formatDuration

@Composable
fun WeeklyDayDetailCard(
    dashboard: DashboardData,
    dayIndex: Int
) {
    // Güvenli erişim
    val stat = dashboard.weeklyBreakdown.getOrNull(dayIndex)

    if (stat == null) return

    Card {
        Column(
            Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("${stat.date} – ${formatDuration(stat.totalMinutes)}", style = MaterialTheme.typography.titleMedium)

            Spacer(Modifier.height(8.dp))

            // 🔥 ARTIK HER GÜNÜN KENDİ APP LİSTESİ VAR
            stat.apps.take(5).forEach { app ->
                Text("${app.appName}: ${formatDuration(app.minutes)}")
            }
        }
    }
}