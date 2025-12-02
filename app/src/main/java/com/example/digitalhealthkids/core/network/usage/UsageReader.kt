package com.example.digitalhealthkids.core.network.usage

import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.PackageManager
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Calendar

private val isoFormatter: DateTimeFormatter =
    DateTimeFormatter.ISO_INSTANT.withZone(ZoneOffset.UTC)

fun toIso(millis: Long): String =
    isoFormatter.format(Instant.ofEpochMilli(millis))

// 🔥 İsim çözücü: Bulamazsa paket ismini süsleyerek döndürür
fun resolveAppName(context: Context, packageName: String): String {
    return try {
        val pm: PackageManager = context.packageManager
        val appInfo = pm.getApplicationInfo(packageName, 0)
        pm.getApplicationLabel(appInfo).toString()
    } catch (e: Exception) {
        // Eğer isim bulunamazsa "com.instagram.android" -> "Instagram" yapar
        packageName.substringAfterLast('.')
            .replaceFirstChar { it.uppercase() }
    }
}

// 🔥 Sistem uygulamalarını elemek için filtre
fun isUserApp(context: Context, packageName: String): Boolean {
    // 1. Yasaklı Liste (Blacklist) - Bunları kesinlikle gösterme
    val junkApps = setOf(
        "com.android.launcher",       // Ana ekran (Pixel Launcher vs)
        "com.google.android.launcher",
        "com.android.systemui",       // Üst bar, navigasyon
        "com.android.settings",       // Ayarlar menüsü
        "com.google.android.gms",     // Google Play Hizmetleri
        "com.google.android.googlequicksearchbox", // Google Arama çubuğu
        "com.android.vending",        // Play Store (İstersen kalsın, genelde gereksiz)
        "android"                     // Sistem çekirdeği
    )

    if (junkApps.contains(packageName)) return false
    if (packageName.contains("launcher", ignoreCase = true)) return false

    return true
}

// Manuel senkronizasyon için (Örn: Son 7 gün)
fun readUsageEventsForRange(context: Context, daysBack: Int): List<UsageEventDto> {
    val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    val calendar = Calendar.getInstance()
    val endTime = calendar.timeInMillis

    calendar.add(Calendar.DAY_OF_YEAR, -daysBack)
    calendar.set(Calendar.HOUR_OF_DAY, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    val startTime = calendar.timeInMillis

    val stats: List<UsageStats> = usm.queryUsageStats(
        UsageStatsManager.INTERVAL_DAILY,
        startTime,
        endTime
    )

    if (stats.isNullOrEmpty()) return emptyList()

    return stats
        .filter { it.totalTimeInForeground > 0 }
        .filter { isUserApp(context, it.packageName) } // 🔥 FİLTRE AKTİF
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

// 🔥 Background Worker (Delta Sync) için gerekli fonksiyon
fun readUsageEventsSince(context: Context, lastSyncTimestamp: Long): List<UsageEventDto> {
    val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    val now = System.currentTimeMillis()

    // Eğer hiç senkronizasyon yapılmamışsa (0), 7 gün öncesinden başla
    val startTime = if (lastSyncTimestamp == 0L) {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -7)
        calendar.timeInMillis
    } else {
        lastSyncTimestamp
    }

    val stats = usm.queryUsageStats(
        UsageStatsManager.INTERVAL_DAILY,
        startTime,
        now
    )

    if (stats.isNullOrEmpty()) return emptyList()

    return stats
        .filter { it.totalTimeInForeground > 0 }
        .filter { isUserApp(context, it.packageName) } // 🔥 FİLTRE BURADA DA AKTİF
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