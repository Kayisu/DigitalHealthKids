package com.example.digitalhealthkids.ui.home.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.digitalhealthkids.domain.usage.DashboardData
import com.example.digitalhealthkids.core.util.formatDuration
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun WeeklyDashboardContent(
    dashboard: DashboardData,
    dailyLimit: Int?,
    selectedDay: Int,
    onDaySelected: (Int) -> Unit,
    onViewDetailsClick: (Int) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Hero Kartı
        item { WeeklyHeroCard(dashboard, dailyLimit) }

        // 2. Grafik
        item {
            Box(modifier = Modifier.height(260.dp)) {
                WeeklyBarChart(
                    dailyStats = dashboard.weeklyBreakdown,
                    selectedDayIndex = selectedDay,
                    onDaySelected = onDaySelected
                )
            }
        }

        // 3. Küçük Özet ve Buton
        item {
            val stat = dashboard.weeklyBreakdown.getOrNull(selectedDay)
            if (stat != null) {
                val dateLabel = try {
                    LocalDate.parse(stat.date).format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
                } catch (e: Exception) { stat.date }

                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(1.dp)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            text = "$dateLabel Özeti",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "Toplam Süre: ${formatDuration(stat.totalMinutes)}",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.height(16.dp))
                        FilledTonalButton(
                            onClick = { onViewDetailsClick(selectedDay) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                        ) {
                            Text("Detaylı Raporu Gör", color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }
                }
            }
        }
    }
}