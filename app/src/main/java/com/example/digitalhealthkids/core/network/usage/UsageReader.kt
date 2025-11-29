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

fun readTodayUsageEvents(context: Context): List<UsageEventDto> {
    val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    val end = System.currentTimeMillis()

    // Bugün 00:00'dan şimdiye kadar
    val startOfDay = java.time.LocalDate.now()
        .atStartOfDay(ZoneOffset.systemDefault())
        .toInstant()
        .toEpochMilli()

    val stats: List<UsageStats> = usm.queryUsageStats(
        UsageStatsManager.INTERVAL_DAILY,
        startOfDay,
        end
    )

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
