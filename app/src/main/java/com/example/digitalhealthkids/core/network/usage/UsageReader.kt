// core/usage/UsageReader.kt gibi bir yere koyabilirsin
package com.example.digitalhealthkids.core.network.usage

import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.PackageManager
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import com.example.digitalhealthkids.core.network.usage.UsageEventDto
import java.util.Calendar

private val isoFormatter: DateTimeFormatter =
    DateTimeFormatter.ISO_INSTANT.withZone(ZoneOffset.UTC)

fun toIso(millis: Long): String =
    isoFormatter.format(Instant.ofEpochMilli(millis))

fun resolveAppName(context: Context, packageName: String): String? {
    return try {
        val pm: PackageManager = context.packageManager
        val appInfo = pm.getApplicationInfo(packageName, 0)
        pm.getApplicationLabel(appInfo).toString()
    } catch (e: Exception) {
        null
    }
}
fun readUsageEventsForRange(context: Context, daysBack: Int): List<UsageEventDto> {
    val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    val calendar = Calendar.getInstance()

    // Bitiş: Şu an
    val endTime = calendar.timeInMillis

    // Başlangıç: X gün öncesi
    calendar.add(Calendar.DAY_OF_YEAR, -daysBack)
    // Saati günün başlangıcına çekelim (Opsiyonel ama temiz olur)
    calendar.set(Calendar.HOUR_OF_DAY, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    val startTime = calendar.timeInMillis

    // Verileri çek (Daily interval en sağlıklısıdır)
    val stats: List<UsageStats> = usm.queryUsageStats(
        UsageStatsManager.INTERVAL_DAILY,
        startTime,
        endTime
    )

    if (stats.isNullOrEmpty()) return emptyList()

    return stats
        .filter { it.totalTimeInForeground > 0 }
        .map { stat ->
            UsageEventDto(
                appPackage = stat.packageName,
                appName = resolveAppName(context, stat.packageName),
                startTime = toIso(stat.firstTimeStamp),
                endTime = toIso(stat.lastTimeStamp),
                totalSeconds = (stat.totalTimeInForeground / 1000).toInt()
            )
        }
}

fun readUsageEventsSince(context: Context, lastSyncTimestamp: Long): List<UsageEventDto> {
    val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    val now = System.currentTimeMillis()

    // Eğer hiç senkronizasyon yapılmamışsa (lastSyncTimestamp = 0),
    // varsayılan olarak 7 gün öncesinden başla.
    // Daha geriye gitmek Android'de veri kaybı veya performans sorunu yaratır.
    val startTime = if (lastSyncTimestamp == 0L) {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -7)
        calendar.timeInMillis
    } else {
        lastSyncTimestamp
    }

    // Android Daily Logları kullanır, bu en performanslısıdır.
    val stats = usm.queryUsageStats(
        UsageStatsManager.INTERVAL_DAILY,
        startTime,
        now
    )

    if (stats.isNullOrEmpty()) return emptyList()

    // Aggregate edilmiş veriyi (aynı app birden fazla satır olabilir) paketleyelim
    // Ancak backend unique constraint ile korunduğu için ham haliyle de gönderebiliriz.
    // Performans için "TotalTime > 0" olanları filtreliyoruz.

    return stats
        .filter { it.totalTimeInForeground > 0 }
        .map { stat ->
            UsageEventDto(
                appPackage = stat.packageName,
                appName = resolveAppName(context, stat.packageName), // Cache mekanizması eklenebilir
                startTime = toIso(stat.firstTimeStamp),
                endTime = toIso(stat.lastTimeStamp),
                totalSeconds = (stat.totalTimeInForeground / 1000).toInt()
            )
        }
}
