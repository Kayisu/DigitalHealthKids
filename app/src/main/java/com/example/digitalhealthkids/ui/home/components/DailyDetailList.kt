package com.example.digitalhealthkids.ui.home.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.digitalhealthkids.core.util.formatDuration
import com.example.digitalhealthkids.domain.usage.AppUsageItem
import com.example.digitalhealthkids.domain.usage.DailyStat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun DailyDetailSection(stat: DailyStat) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Başlık (Örn: "29 Kasım Cuma")
        val parsedDate = try {
            LocalDate.parse(stat.date)
        } catch (e: Exception) { LocalDate.now() }

        val formatter = DateTimeFormatter.ofPattern("d MMMM EEEE", Locale("tr"))
        val dateTitle = parsedDate.format(formatter)

        Text(
            text = "$dateTitle Özeti",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )

        // Liste
        if (stat.apps.isEmpty()) {
            Text(
                "Bu tarihte kullanım verisi yok.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            stat.apps.forEach { app ->
                AppUsageRow(app)
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
            }
        }
    }
}

@Composable
fun AppUsageRow(app: AppUsageItem) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // İkon
        AppIcon(packageName = app.packageName)

        Spacer(modifier = Modifier.width(16.dp))

        // İsim ve Süre
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = app.appName,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium)
            )
            Text(
                text = formatDuration(app.minutes),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary // Vurgulu renk
            )
        }

        // Sağ tarafa belki ileride "Kısıtla" butonu koyarız
    }
}