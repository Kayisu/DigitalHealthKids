package com.example.digitalhealthkids.core.network.usage

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Process
import android.util.Log
import java.util.Calendar

fun hasUsagePermission(context: Context): Boolean {
    val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
    val mode = appOps.checkOpNoThrow(
        AppOpsManager.OPSTR_GET_USAGE_STATS,
        Process.myUid(),
        context.packageName
    )
    return mode == AppOpsManager.MODE_ALLOWED
}

fun resolveAppName(context: Context, packageName: String, appNameCache: MutableMap<String, String>): String {
    if (appNameCache.containsKey(packageName)) return appNameCache[packageName]!!
    return try {
        val pm = context.packageManager
        val appInfo = pm.getApplicationInfo(packageName, 0)
        val appName = pm.getApplicationLabel(appInfo).toString()
        appNameCache[packageName] = appName
        appName
    } catch (e: Exception) {
        appNameCache[packageName] = packageName
        packageName
    }
}

fun isUserApp(context: Context, packageName: String): Boolean {
    if (packageName == context.packageName) return false
    val pm = context.packageManager
    return try {
        // Menüde ikonu olan VEYA sistem uygulaması olmayan her şeyi göster
        if (pm.getLaunchIntentForPackage(packageName) != null) return true
        val appInfo = pm.getApplicationInfo(packageName, 0)
        (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) == 0
    } catch (e: Exception) {
        false
    }
}

fun getTodayTotalUsageMillis(context: Context): Long {
    if (!hasUsagePermission(context)) return 0L
    val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

    val calendar = Calendar.getInstance()
    calendar.set(Calendar.HOUR_OF_DAY, 0); calendar.set(Calendar.MINUTE, 0); calendar.set(Calendar.SECOND, 0); calendar.set(Calendar.MILLISECOND, 0)

    // queryAndAggregateUsageStats: Kesin ve tekil süre verir
    val stats = usm.queryAndAggregateUsageStats(calendar.timeInMillis, System.currentTimeMillis())

    var total = 0L
    for ((pkg, stat) in stats) {
        if (stat.totalTimeInForeground > 0 && isUserApp(context, pkg)) {
            total += stat.totalTimeInForeground
        }
    }
    return total
}

fun readUsageEventsForRange(context: Context, daysBack: Int): List<UsageEventDto> {
    if (!hasUsagePermission(context)) return emptyList()

    val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    val resultList = mutableListOf<UsageEventDto>()
    val appNameCache = mutableMapOf<String, String>()
    val calendar = Calendar.getInstance()

    // Bilinen sistem/ayar uygulamalarını dışla
    val ignorePackages = setOf(
        "com.android.settings",
        "com.android.systemui",
        "com.coloros.alarmclock",
        "com.coloros.calculator",
        "com.google.android.apps.translate",
        "com.tailscale.ipn"
    )

    val targetDays = if (daysBack > 0) 0..daysBack else 0..0

    for (i in targetDays) {
        calendar.timeInMillis = System.currentTimeMillis()
        calendar.add(Calendar.DAY_OF_YEAR, -i)
        calendar.set(Calendar.HOUR_OF_DAY, 0); calendar.set(Calendar.MINUTE, 0); calendar.set(Calendar.SECOND, 0); calendar.set(Calendar.MILLISECOND, 0)
        val startTime = calendar.timeInMillis

        val endTime = if (i == 0) System.currentTimeMillis() else {
            calendar.set(Calendar.HOUR_OF_DAY, 23); calendar.set(Calendar.MINUTE, 59); calendar.set(Calendar.SECOND, 59)
            calendar.timeInMillis
        }

        val usageEvents = usm.queryEvents(startTime, endTime)
        val event = UsageEvents.Event()
        val activeSessions = mutableMapOf<String, Long>()

        while (usageEvents.hasNextEvent()) {
            usageEvents.getNextEvent(event)
            val packageName = event.packageName ?: continue
            if (ignorePackages.contains(packageName)) continue
            if (!isUserApp(context, packageName)) continue

            when (event.eventType) {
                UsageEvents.Event.MOVE_TO_FOREGROUND, UsageEvents.Event.ACTIVITY_RESUMED -> {
                    activeSessions[packageName] = event.timeStamp
                }

                UsageEvents.Event.MOVE_TO_BACKGROUND, UsageEvents.Event.ACTIVITY_PAUSED -> {
                    val start = activeSessions.remove(packageName) ?: continue
                    if (event.timeStamp <= start) continue

                    val durationSec = ((event.timeStamp - start) / 1000).toInt()
                    if (durationSec <= 0) continue
                    resultList.add(
                        UsageEventDto(
                            packageName = packageName,
                            appName = resolveAppName(context, packageName, appNameCache),
                            timestampStart = start,
                            timestampEnd = event.timeStamp,
                            durationSeconds = durationSec
                        )
                    )
                }
            }
        }
    }
    Log.d("UsageReader", "Raporlanan Uygulama Sayısı: ${resultList.size}")
    return resultList
}