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

// 🔥 İYİLEŞTİRME 1: İsim Çözücü
fun resolveAppName(context: Context, packageName: String): String {
    return try {
        val pm: PackageManager = context.packageManager
        val appInfo = pm.getApplicationInfo(packageName, 0)
        pm.getApplicationLabel(appInfo).toString()
    } catch (e: Exception) {
        // Bulamazsa paket isminin son kısmını (örn: com.whatsapp -> Whatsapp) yap
        packageName.substringAfterLast('.')
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }
}

// 🔥 İYİLEŞTİRME 2: Filtreleme Mantığı (Google Digital Wellbeing Tarzı)
fun isUserApp(context: Context, packageName: String): Boolean {
    // 1. Kendi uygulamamızı gizle (Bizim app hariç demiştin)
    if (packageName == context.packageName) return false

    // 2. Yasaklı Liste (Kesinlikle görmek istemediklerimiz)
    val junkApps = setOf(
        "com.android.systemui",       // Bildirim çubuğu
        "com.android.settings",       // Ayarlar
        "com.google.android.gms",     // Google Play Hizmetleri
        "com.google.android.googlequicksearchbox", // Google Arama Widget'ı
        "android"                     // Android System Çekirdeği
    )
    if (junkApps.contains(packageName)) return false

    // Launcher'ları (Ana Ekran) gizle (Pixel Launcher, OneUI Home vb.)
    if (packageName.contains("launcher", ignoreCase = true)) return false

    // 3. ALTIN KURAL: Bu uygulama ana ekrandan açılabilir mi?
    // Instagram, WhatsApp, Twitter, Oyunlar -> Açılabilir.
    // Klavye, Bluetooth, Arka plan servisleri -> Açılamaz.
    val pm = context.packageManager
    val intent = pm.getLaunchIntentForPackage(packageName)

    return intent != null
}

fun readUsageEventsForRange(context: Context, daysBack: Int): List<UsageEventDto> {
    val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    val calendar = Calendar.getInstance()
    val endTime = calendar.timeInMillis

    calendar.add(Calendar.DAY_OF_YEAR, -daysBack)
    calendar.set(Calendar.HOUR_OF_DAY, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    val startTime = calendar.timeInMillis

    // QUERY_AND_AGGREGATE değil queryUsageStats kullanıyoruz ki detay alabilelim
    val stats: List<UsageStats> = usm.queryUsageStats(
        UsageStatsManager.INTERVAL_DAILY,
        startTime,
        endTime
    )

    if (stats.isNullOrEmpty()) return emptyList()

    // Aynı paketten birden fazla kayıt gelebilir (farklı günlerde), bunları birleştirmeliyiz
    // Ama şimdilik ham gönderiyoruz, backend hallediyor.
    return stats
        .filter { it.totalTimeInForeground > 0 }
        .filter { isUserApp(context, it.packageName) }
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
        .filter { isUserApp(context, it.packageName) }
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