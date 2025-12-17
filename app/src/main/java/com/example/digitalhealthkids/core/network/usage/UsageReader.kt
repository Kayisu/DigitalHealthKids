package com.example.digitalhealthkids.core.network.usage

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Process
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

// Backend'e yollanacak sade tarih formatı
val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

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
fun isUserApp(context: Context, packageName: String): Boolean {
    if (packageName == context.packageName) return false

    val knownLaunchers = listOf(
        "com.android.launcher",
        "com.google.android.apps.nexuslauncher",
        "com.samsung.android.app.home.ui",
        "com.miui.home",
        "com.huawei.android.launcher",
        "com.oppo.launcher",
        "net.oneplus.launcher",
        "com.bbk.launcher2" // Realme/Oppo ek
    )
    if (knownLaunchers.any { packageName.contains(it) }) return false

    val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
    val resolveInfo = context.packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
    if (resolveInfo?.activityInfo?.packageName == packageName) return false

    return try {
        val pm = context.packageManager
        pm.getLaunchIntentForPackage(packageName) != null
    } catch (e: Exception) {
        false
    }
}

fun readUsageEventsForRange(context: Context, daysBack: Int): List<UsageEventDto> {
    if (!hasUsagePermission(context)) return emptyList()

    val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

    val endTime = System.currentTimeMillis()
    val cal = Calendar.getInstance()
    cal.add(Calendar.DAY_OF_YEAR, -daysBack)
    cal.set(Calendar.HOUR_OF_DAY, 0)
    val startTime = cal.timeInMillis

    val events = usm.queryEvents(startTime, endTime)
    val event = UsageEvents.Event()

    val resultList = mutableListOf<UsageEventDto>()
    val startMap = mutableMapOf<String, Long>()

    while (events.hasNextEvent()) {
        events.getNextEvent(event)

        val pkg = event.packageName
        if (!isUserApp(context, pkg)) continue

        when (event.eventType) {
            UsageEvents.Event.MOVE_TO_FOREGROUND -> {
                startMap[pkg] = event.timeStamp
            }
            UsageEvents.Event.MOVE_TO_BACKGROUND -> {
                val start = startMap.remove(pkg)
                if (start != null) {
                    val end = event.timeStamp
                    val duration = (end - start) / 1000L

                    if (duration > 0) {
                        resultList.add(
                            UsageEventDto(
                                packageName = pkg,
                                appName = resolveAppName(context, pkg), // 🔥 BURADA EKLİYORUZ
                                timestampStart = start,
                                timestampEnd = end,
                                durationSeconds = duration.toInt()
                            )
                        )
                    }
                }
            }
        }
    }
    return resultList
}

fun getTodayTotalUsageMillis(context: Context): Long {
    if (!hasUsagePermission(context)) return 0L

    val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    val calendar = Calendar.getInstance()

    // Bugünü belirle (Gece 00:00)
    calendar.set(Calendar.HOUR_OF_DAY, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    val startTime = calendar.timeInMillis
    val endTime = System.currentTimeMillis()

    // Günlük veriyi çek
    val stats = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, startTime, endTime)

    var totalMillis = 0L
    if (stats != null) {
        for (usageStat in stats) {
            // Sadece kullanıcı uygulamalarını topla (Launcher, Sistem UI hariç)
            if (usageStat.totalTimeInForeground > 0 && isUserApp(context, usageStat.packageName)) {
                totalMillis += usageStat.totalTimeInForeground
            }
        }
    }
    return totalMillis
}