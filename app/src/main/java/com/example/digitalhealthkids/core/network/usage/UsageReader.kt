package com.example.digitalhealthkids.core.network.usage

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Process
import androidx.collection.ArrayMap
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Calendar
import kotlin.math.max
import kotlin.math.min

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
        val pm: PackageManager = context.packageManager
        val appInfo = pm.getApplicationInfo(packageName, 0)
        pm.getApplicationLabel(appInfo).toString()
    } catch (e: Exception) {
        packageName.substringAfterLast('.')
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }
}

fun isUserApp(context: Context, packageName: String): Boolean {
    if (packageName == context.packageName) return false
    val junkApps = setOf(
        "com.android.systemui", "com.android.settings", "android",
        "com.google.android.inputmethod.latin", "com.google.android.gms",
        "com.google.android.apps.nexuslauncher", "com.android.launcher3",
        "com.miui.home", "com.sec.android.app.launcher"
    )
    if (junkApps.contains(packageName)) return false

    val pm = context.packageManager
    if (pm.getLaunchIntentForPackage(packageName) != null) return true

    return try {
        val appInfo = pm.getApplicationInfo(packageName, 0)
        val isSystemApp = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
        val isUpdatedSystemApp = (appInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
        !isSystemApp || isUpdatedSystemApp
    } catch (e: Exception) {
        false
    }
}

/**
 * 🔥 TIMELINE ALGORİTMASI (GELİŞMİŞ)
 */
fun calculateUsageForDay(context: Context, targetStartTime: Long, targetEndTime: Long): List<UsageEventDto> {
    if (!hasUsagePermission(context)) return emptyList()

    val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

    // Warm-up: 2 saat öncesinden başla ki gece yarısı geçişlerini yakala
    val queryStartTime = targetStartTime - (2 * 60 * 60 * 1000)
    val events = usm.queryEvents(queryStartTime, targetEndTime)
    val event = UsageEvents.Event()

    val appTotalTimes = ArrayMap<String, Long>()
    // İlk ve Son timestamp'leri doğru raporlamak için
    val appFirstStamps = ArrayMap<String, Long>()
    val appLastStamps = ArrayMap<String, Long>()

    var currentPkg: String? = null
    var lastEventTime: Long = queryStartTime

    while (events.hasNextEvent()) {
        events.getNextEvent(event)
        val eventTime = event.timeStamp

        // 1. Süre Ekleme: Eğer bir paket açıksa süresini ekle
        if (currentPkg != null) {
            val validStart = max(lastEventTime, targetStartTime)
            val validEnd = min(eventTime, targetEndTime)
            val duration = validEnd - validStart

            if (duration > 0) {
                val total = appTotalTimes[currentPkg] ?: 0L
                appTotalTimes[currentPkg!!] = total + duration
                if (eventTime >= targetStartTime) {
                    appLastStamps[currentPkg!!] = eventTime
                }
            }
        }

        // 2. İlk Görülme Zamanı
        if (eventTime >= targetStartTime && !appFirstStamps.containsKey(event.packageName)) {
            appFirstStamps[event.packageName] = eventTime
        }

        // 3. Durum Değişimi
        when (event.eventType) {
            UsageEvents.Event.MOVE_TO_FOREGROUND -> currentPkg = event.packageName
            UsageEvents.Event.MOVE_TO_BACKGROUND -> if (currentPkg == event.packageName) currentPkg = null
            UsageEvents.Event.KEYGUARD_SHOWN,
            UsageEvents.Event.SCREEN_NON_INTERACTIVE,
            UsageEvents.Event.DEVICE_SHUTDOWN -> currentPkg = null
        }

        lastEventTime = eventTime
    }

    // 4. Bitiş Kontrolü (Hala açık olanlar)
    if (currentPkg != null) {
        val validStart = max(lastEventTime, targetStartTime)
        val validEnd = targetEndTime
        val duration = validEnd - validStart

        if (duration > 0) {
            val total = appTotalTimes[currentPkg] ?: 0L
            appTotalTimes[currentPkg!!] = total + duration
            appLastStamps[currentPkg!!] = validEnd
        }
    }

    return appTotalTimes.mapNotNull { (pkg, totalMillis) ->
        if (totalMillis > 0 && isUserApp(context, pkg)) {
            UsageEventDto(
                appPackage = pkg,
                appName = resolveAppName(context, pkg),
                startTime = toIso(appFirstStamps[pkg] ?: targetStartTime),
                endTime = toIso(appLastStamps[pkg] ?: targetEndTime),
                totalSeconds = (totalMillis / 1000).toInt()
            )
        } else {
            null
        }
    }
}

// -----------------------------------------------------
// DÜZELTİLMİŞ ANA FONKSİYONLAR
// -----------------------------------------------------

fun readUsageEventsForRange(context: Context, daysBack: Int): List<UsageEventDto> {
    val allEvents = mutableListOf<UsageEventDto>()
    for (i in 0..daysBack) {
        val currentCal = Calendar.getInstance()
        currentCal.add(Calendar.DAY_OF_YEAR, -i)

        currentCal.set(Calendar.HOUR_OF_DAY, 0)
        currentCal.set(Calendar.MINUTE, 0)
        currentCal.set(Calendar.SECOND, 0)
        currentCal.set(Calendar.MILLISECOND, 0)
        val startOfDay = currentCal.timeInMillis

        val endOfDay = if (i == 0) System.currentTimeMillis() else {
            currentCal.set(Calendar.HOUR_OF_DAY, 23)
            currentCal.set(Calendar.MINUTE, 59)
            currentCal.set(Calendar.SECOND, 59)
            currentCal.set(Calendar.MILLISECOND, 999)
            currentCal.timeInMillis
        }

        allEvents.addAll(calculateUsageForDay(context, startOfDay, endOfDay))
    }
    return allEvents
}

fun readUsageEventsSince(context: Context, lastSyncTimestamp: Long): List<UsageEventDto> {
    val now = System.currentTimeMillis()
    val calendar = Calendar.getInstance()

    // Eğer hiç sync yoksa dünden başla, varsa son sync zamanını al
    val effectiveLastSync = if (lastSyncTimestamp == 0L) {
        now - (24 * 60 * 60 * 1000)
    } else {
        lastSyncTimestamp
    }

    // 1. Son sync yapılan GÜNÜN 00:00'ını bul
    calendar.timeInMillis = effectiveLastSync
    calendar.set(Calendar.HOUR_OF_DAY, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)

    var currentStart = calendar.timeInMillis

    // 2. Bugünün 00:00'ını bul (Döngü bitişi için)
    val todayCal = Calendar.getInstance()
    todayCal.set(Calendar.HOUR_OF_DAY, 0)
    todayCal.set(Calendar.MINUTE, 0)
    todayCal.set(Calendar.SECOND, 0)
    todayCal.set(Calendar.MILLISECOND, 0)
    val startOfToday = todayCal.timeInMillis

    val allEvents = mutableListOf<UsageEventDto>()

    // 3. Döngü: O günden bugüne kadar olan HER GÜNÜ tam baştan (00:00) hesapla
    while (currentStart <= startOfToday) {
        val isToday = (currentStart == startOfToday)

        val endOfInterval = if (isToday) {
            now // Bugünse şu ana kadar
        } else {
            val c = Calendar.getInstance()
            c.timeInMillis = currentStart
            c.set(Calendar.HOUR_OF_DAY, 23)
            c.set(Calendar.MINUTE, 59)
            c.set(Calendar.SECOND, 59)
            c.timeInMillis // Geçmişse gün sonuna kadar
        }

        allEvents.addAll(calculateUsageForDay(context, currentStart, endOfInterval))

        // Bir sonraki güne geç
        val c = Calendar.getInstance()
        c.timeInMillis = currentStart
        c.add(Calendar.DAY_OF_YEAR, 1)
        currentStart = c.timeInMillis
    }

    return allEvents
}