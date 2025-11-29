package com.example.digitalhealthkids.ui.home.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.digitalhealthkids.domain.usage.DashboardData

@Composable
fun WeeklyDayDetailCard(
    dashboard: DashboardData,
    dayIndex: Int
) {
    val dayMinutes = dashboard.weeklyTrend[dayIndex]
    val dayLabel = listOf("Pzt", "Sal", "Çar", "Per", "Cum", "Cmt", "Paz")[dayIndex]

    Card {
        Column(
            Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("$dayLabel – $dayMinutes dk", style = MaterialTheme.typography.titleMedium)

            Text(
                "En yoğun saat aralığı: 18:00 – 21:00",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(8.dp))

            dashboard.topApps.take(3).forEach { app ->
                Text("${app.appName}: ${app.minutes} dk")
            }
        }
    }
}
