package com.example.digitalhealthkids.ui.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.example.digitalhealthkids.domain.usage.DailyStat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun WeeklyBarChart(
    dailyStats: List<DailyStat>,
    selectedDayIndex: Int,
    onDaySelected: (Int) -> Unit
) {
    val maxMinutes = dailyStats.maxOfOrNull { it.totalMinutes } ?: 1
    val max = if (maxMinutes == 0) 1f else maxMinutes.toFloat()

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Haftalık Aktivite", style = MaterialTheme.typography.titleMedium)

            Row(
                Modifier.fillMaxWidth().height(160.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                dailyStats.forEachIndexed { index, stat ->
                    val isSelected = index == selectedDayIndex
                    val ratio = stat.totalMinutes / max

                    // Tarih Formatı (Örn: "Pzt")
                    val dayLabel = try {
                        val date = LocalDate.parse(stat.date)
                        date.format(DateTimeFormatter.ofPattern("EEE", Locale("tr")))
                    } catch (e: Exception) { "?" }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onDaySelected(index) } // 🔥 TIKLAMA
                    ) {
                        // Çubuk
                        Box(
                            modifier = Modifier
                                .width(16.dp) // Daha ince, zarif çubuklar
                                .fillMaxHeight(ratio.coerceAtLeast(0.05f)) // En azından minik bir çizgi görünsün
                                .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.primaryContainer
                                )
                        )

                        // Gün Etiketi
                        Text(
                            text = dayLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}