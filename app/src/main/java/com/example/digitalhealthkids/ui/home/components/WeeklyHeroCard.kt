package com.example.digitalhealthkids.ui.home.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.digitalhealthkids.domain.usage.DashboardData

@Composable
fun WeeklyHeroCard(data: DashboardData) {
    Card {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Bu Hafta – ${data.childName}",
                style = MaterialTheme.typography.titleLarge
            )

            Text(
                text = "Şu ana kadar toplam: ${data.weeklyTrend.sum()} dk",
                style = MaterialTheme.typography.bodyLarge
            )

            Text(
                text = "Bugün: ${data.todayTotalMinutes} dk",
                color = MaterialTheme.colorScheme.primary
            )

            Text(
                text = "Günlük ortalama: ${data.weeklyTrend.average().toInt()} dk",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
