// Dosya: app/src/main/java/com/example/digitalhealthkids/core/network/usage/UsageReader.kt

package com.example.digitalhealthkids.core.network.usage

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Process
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Calendar

// Backend ISO formatı bekliyor (UTC)
private val isoFormatter: DateTimeFormatter =
    DateTimeFormatter.ISO_INSTANT.withZone(ZoneOffset.UTC)

fun toIso(millis: Long): String =
    isoFormatter.format(Instant.ofEpochMilli(millis))

fun hasUsagePermission(context: Context): Boolean {
    val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
    val mode = appOps.checkOpNoThrow(
        AppOpsManager.OPSTR_GET_USAGE_STATS,
        Process.myUid(),
        context.packageName
    )
    return mode == AppOpsManager.MODE_ALLOWED
}

fun resolveAppName(context: Context, packageName: String): String {
    return try {
        val pm = context.packageManager
        val appInfo = pm.getApplicationInfo(packageName, 0)
        pm.getApplicationLabel(appInfo).toString()
    } catch (e: Exception) {
        packageName
    }
}

/**
 * 🔥 FİLTRELEME (StatsHelper.kt'den Birebir Port)
 * Eski karmaşık filtreleri attık. Sadece "Launch Intent'i var mı?" diye bakıyoruz.
 * Sistem Başlatıcılar, System UI vb. genelde null döner ve elenir.
 */
fun isUserApp(context: Context, packageName: String): Boolean {
    if (packageName == context.packageName) return false // Kendimizi gösterme

    return try {
        val pm = context.packageManager
        // Demonun yaptığı tek ve geçerli kontrol bu:
        val intent = pm.getLaunchIntentForPackage(packageName)
        intent != null
    } catch (e: Exception) {
        false
    }
}

/**
 * 🔥 SÜRE HESAPLAMA (StatsHelper.kt -> getUsageSessions mantığı)
 * Aggregate yerine Event'leri tek tek işleyip oturum (Session) oluşturuyoruz.
 */
fun getDailyUsageStats(context: Context, startTime: Long, endTime: Long): List<UsageEventDto> {
    if (!hasUsagePermission(context)) return emptyList()

    val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

    // 1. Tüm olayları çek
    val events = usm.queryEvents(startTime, endTime)
    if (events == null) return emptyList()

    val appSessionStartTimes = mutableMapOf<String, Long>() // Hangi uygulama ne zaman açıldı?
    val usageDurations = mutableMapOf<String, Long>()     // Toplam süreler

    val event = UsageEvents.Event()
    while (events.hasNextEvent()) {
        events.getNextEvent(event)

        val pkg = event.packageName

        // StatsHelper satır 62: ACTIVITY_RESUMED -> Oturum Başladı
        if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED) {
            appSessionStartTimes[pkg] = event.timeStamp
        }
        // StatsHelper satır 64: ACTIVITY_PAUSED -> Oturum Bitti
        else if (event.eventType == UsageEvents.Event.ACTIVITY_PAUSED) {
            val start = appSessionStartTimes.remove(pkg)
            if (start != null) {
                val duration = event.timeStamp - start
                // Negatif veya 0 süreyi alma
                if (duration > 0) {
                    usageDurations[pkg] = (usageDurations[pkg] ?: 0L) + duration
                }
            }
        }
    }

    // 🔥 StatsHelper satır 73: Hala açık kalanları ekle (O AN KULLANILAN UYGULAMA)
    // Döngü bitti ama 'appSessionStartTimes' içinde kalanlar şu an ekranda olanlardır.
    // Onların süresini sorgu bitiş zamanına (endTime) kadar hesaplayıp ekliyoruz.
    appSessionStartTimes.forEach { (pkg, start) ->
        val duration = endTime - start
        if (duration > 0) {
            usageDurations[pkg] = (usageDurations[pkg] ?: 0L) + duration
        }
    }

    // DTO Çevrimi ve Filtreleme
    return usageDurations.mapNotNull { (pkg, totalMillis) ->
        // Filtreyi burada uyguluyoruz (1 saniyeden az olanları gürültü diye atıyoruz)
        if (totalMillis > 1000 && isUserApp(context, pkg)) {
            UsageEventDto(
                appPackage = pkg,
                appName = resolveAppName(context, pkg),
                startTime = toIso(startTime),
                endTime = toIso(endTime),
                totalSeconds = (totalMillis / 1000).toInt()
            )
        } else {
            null
        }
    }.sortedByDescending { it.totalSeconds }
}

// ViewModel'den çağrılan ana fonksiyon (Tarih aralığı oluşturup yukarıyı çağırır)
fun readUsageEventsForRange(context: Context, daysBack: Int): List<UsageEventDto> {
    val allEvents = mutableListOf<UsageEventDto>()

    for (i in 0..daysBack) {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -i)

        // Günün başı 00:00
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val startOfDay = cal.timeInMillis

        // Günün sonu
        val endOfDay = if (i == 0) {
            System.currentTimeMillis() // Bugünse şu ana kadar
        } else {
            cal.set(Calendar.HOUR_OF_DAY, 23)
            cal.set(Calendar.MINUTE, 59)
            cal.set(Calendar.SECOND, 59)
            cal.set(Calendar.MILLISECOND, 999)
            cal.timeInMillis
        }

        allEvents.addAll(getDailyUsageStats(context, startOfDay, endOfDay))
    }
    return allEvents
}