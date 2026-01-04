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
    // Grafik patlamasın diye tavan belirliyoruz
    val maxMinutes = dailyStats.maxOfOrNull { it.totalMinutes } ?: 1
    // Eğer tüm değerler 0 ise tavanı 60 yap (boş grafik düzgün görünsün)
    val chartMax = if (maxMinutes == 0) 60f else maxMinutes.toFloat()

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("Haftalık Aktivite", style = MaterialTheme.typography.titleMedium)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                dailyStats.forEachIndexed { index, stat ->
                    val isSelected = index == selectedDayIndex
                    val heightRatio = (stat.totalMinutes / chartMax).coerceIn(0f, 1f)

                    val dayLabel = try {
                        val date = LocalDate.parse(stat.date)
                        date.format(DateTimeFormatter.ofPattern("EEE", Locale("tr")))
                    } catch (e: Exception) { "?" }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom,
                        modifier = Modifier
                            .fillMaxHeight()
                            .weight(1f)
                            .clickable { onDaySelected(index) }
                    ) {
                        Spacer(modifier = Modifier.height(20.dp))
                        Box(
                            contentAlignment = Alignment.BottomCenter,
                            modifier = Modifier
                                .weight(1f)
                                .width(36.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(14.dp)
                                    .fillMaxHeight(heightRatio)
                                    .heightIn(min = if (stat.totalMinutes > 0) 6.dp else 2.dp)
                                    .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
                                    )
                                    .align(Alignment.BottomCenter)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

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