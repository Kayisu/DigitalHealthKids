package com.example.digitalhealthkids.ui.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun WeeklyBarChart(
    values: List<Int>,
    selectedDay: Int,
    onDaySelected: (Int) -> Unit
) {
    // 🔥 DÜZELTME BURADA:
    // Eğer liste boşsa veya max değer 0 ise, böleni en az 1 yapıyoruz.
    // Böylece 0/0 = NaN hatasından kurtuluyoruz.
    val maxValueInList = values.maxOrNull() ?: 0
    val max = if (maxValueInList == 0) 1f else maxValueInList.toFloat()

    val labels = listOf("Pzt", "Sal", "Çar", "Per", "Cum", "Cmt", "Paz")

    Card {
        Column(
            Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Haftalık Kullanım", style = MaterialTheme.typography.titleMedium)

            Row(
                Modifier
                    .fillMaxWidth()
                    .height(150.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                // Eğer values listesi 7 günden az gelirse patlamasın diye take/pad yapılabilir
                // ama şimdilik backend 7 dönüyor varsayıyoruz.
                values.forEachIndexed { index, value ->
                    val isSelected = index == selectedDay

                    // Oran hesabı
                    val barHeightRatio = value / max

                    Box(
                        modifier = Modifier
                            .width(28.dp)
                            .fillMaxHeight(barHeightRatio) // Artık burası NaN olamaz
                            .background(
                                if (isSelected)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                            )
                            .clickable { onDaySelected(index) }
                    )
                }
            }

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                labels.forEachIndexed { i, label ->
                    Text(
                        text = label,
                        color = if (i == selectedDay)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    }
}