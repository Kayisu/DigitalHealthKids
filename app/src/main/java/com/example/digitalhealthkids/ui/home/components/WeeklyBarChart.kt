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
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Haftalık Aktivite", style = MaterialTheme.typography.titleMedium)

            // Çubukların olduğu alan
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                horizontalArrangement = Arrangement.SpaceEvenly, // Eşit aralıklarla dağıt
                verticalAlignment = Alignment.Bottom // Row içindekileri alta hizala
            ) {
                dailyStats.forEachIndexed { index, stat ->
                    val isSelected = index == selectedDayIndex

                    // Oran (0.0 ile 1.0 arası)
                    val heightRatio = (stat.totalMinutes / chartMax).coerceIn(0f, 1f)

                    // Gün İsmi (Pzt, Sal...)
                    val dayLabel = try {
                        val date = LocalDate.parse(stat.date)
                        date.format(DateTimeFormatter.ofPattern("EEE", Locale("tr")))
                    } catch (e: Exception) { "?" }

                    // Tek bir gün sütunu
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        // ESKİSİ (Hatalı): verticalArrangement = Arrangement.End
                        // YENİSİ (Doğru):
                        verticalArrangement = Arrangement.Bottom,
                        modifier = Modifier
                            .fillMaxHeight()
                            .weight(1f)
                            .clickable { onDaySelected(index) }
                    ) {
                        // ÇUBUK KUTUSU
                        // Box kullanarak çubuğun boyutunu ve hizalamasını garantiye alıyoruz
                        Box(
                            contentAlignment = Alignment.BottomCenter, // İçerik alttan büyüsün
                            modifier = Modifier
                                .weight(1f) // Üstteki boşluğu doldur
                                .width(30.dp) // Tıklanabilir alanı geniş tutmak için
                        ) {
                            // Asıl Renkli Çubuk
                            Box(
                                modifier = Modifier
                                    .width(12.dp) // Çubuk kalınlığı
                                    .fillMaxHeight(heightRatio) // Yükseklik orana göre
                                    // Veri yoksa bile (0 dk) minik bir nokta koy (4dp)
                                    .heightIn(min = if (stat.totalMinutes > 0) 4.dp else 2.dp)
                                    .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.primaryContainer
                                    )
                                    .align(Alignment.BottomCenter) // KESİN HİZALAMA
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

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