package com.example.digitalhealthkids.ui.home.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.digitalhealthkids.core.util.formatDuration
import com.example.digitalhealthkids.domain.usage.DashboardData

@Composable
fun WeeklyHeroCard(data: DashboardData) {
    val weeklyTotal = data.weeklyBreakdown.sumOf { it.totalMinutes }
    val weeklyAverage = if (data.weeklyBreakdown.isNotEmpty())
        weeklyTotal / data.weeklyBreakdown.size
    else 0

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Haftalık Özet",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = data.userName,
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatisticItem(
                    label = "Bugün",
                    value = formatDuration(data.todayTotalMinutes),
                    isHighlighted = true
                )
                StatisticItem(
                    label = "Ortalama",
                    value = formatDuration(weeklyAverage)
                )
                StatisticItem(
                    label = "Toplam",
                    value = formatDuration(weeklyTotal)
                )
            }
        }
    }
}

@Composable
private fun StatisticItem(label: String, value: String, isHighlighted: Boolean = false) {
    Column(horizontalAlignment = Alignment.Start) {
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = if (isHighlighted) FontWeight.Bold else FontWeight.Medium,
                fontSize = if (isHighlighted) 20.sp else 16.sp
            ),
            color = if (isHighlighted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
    }
}