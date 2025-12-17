package com.example.digitalhealthkids.core.network.usage

import android.app.AppOpsManager
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Process
import android.util.Log
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.collections.mutableMapOf
import kotlin.collections.mutableListOf

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

fun resolveAppName(context: Context, packageName: String, appNameCache: MutableMap<String, String>): String {
    if (appNameCache.containsKey(packageName)) {
        return appNameCache[packageName]!!
    }
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
    // UYGULAMANIN KENDİNİ SAYMASINI ENGELLEYEN KESİN KURAL
    if (packageName == "com.example.digitalhealthkids" || packageName == context.packageName) {
        return false
    }

    val knownSystemPackages = listOf(
        "com.android.", "com.google.android.", "com.samsung.android.",
        "com.miui.", "com.huawei.android.", "com.oppo.", "net.oneplus.",
        "com.bbk."
    )
    if (knownSystemPackages.any { packageName.startsWith(it) }) return false
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
    val resultList = mutableListOf<UsageEventDto>()
    val appNameCache = mutableMapOf<String, String>()
    val calendar = Calendar.getInstance()

    for (i in 0..daysBack) {
        calendar.timeInMillis = System.currentTimeMillis()
        calendar.add(Calendar.DAY_OF_YEAR, -i)

        calendar.set(Calendar.HOUR_OF_DAY, 0); calendar.set(Calendar.MINUTE, 0); calendar.set(Calendar.SECOND, 0); calendar.set(Calendar.MILLISECOND, 0)
        val startTime = calendar.timeInMillis
        calendar.set(Calendar.HOUR_OF_DAY, 23); calendar.set(Calendar.MINUTE, 59); calendar.set(Calendar.SECOND, 59); calendar.set(Calendar.MILLISECOND, 999)
        val endTime = calendar.timeInMillis

        val stats = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, startTime, endTime)

        val dailyAggregatedMillis = mutableMapOf<String, Long>()
        if (stats != null) {
            for (usageStat in stats) {
                if (usageStat.totalTimeInForeground > 0 && isUserApp(context, usageStat.packageName)) {
                    val currentMillis = dailyAggregatedMillis.getOrDefault(usageStat.packageName, 0L)
                    dailyAggregatedMillis[usageStat.packageName] = currentMillis + usageStat.totalTimeInForeground
                }
            }
        }

        for ((packageName, totalMillis) in dailyAggregatedMillis) {
            val durationInSeconds = (totalMillis / 1000).toInt()
            if (durationInSeconds > 0) {
                resultList.add(
                    UsageEventDto(
                        packageName = packageName,
                        appName = resolveAppName(context, packageName, appNameCache),
                        timestampStart = endTime - totalMillis,
                        timestampEnd = endTime,
                        durationSeconds = durationInSeconds
                    )
                )
            }
        }
    }
    Log.d("UsageReader", "${resultList.size} adet TEMİZ ve TOPLANMIŞ event yollandı.")
    return resultList
}

fun getTodayTotalUsageMillis(context: Context): Long {
    if (!hasUsagePermission(context)) return 0L

    val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    val calendar = Calendar.getInstance()
    calendar.set(Calendar.HOUR_OF_DAY, 0); calendar.set(Calendar.MINUTE, 0); calendar.set(Calendar.SECOND, 0)
    val startTime = calendar.timeInMillis
    val endTime = System.currentTimeMillis()

    val stats = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, startTime, endTime)

    var totalMillis = 0L
    if (stats != null) {
        val aggregatedMap = mutableMapOf<String, Long>()
        for (usageStat in stats) {
            if (usageStat.totalTimeInForeground > 0 && isUserApp(context, usageStat.packageName)) {
                 val currentMillis = aggregatedMap.getOrDefault(usageStat.packageName, 0L)
                 aggregatedMap[usageStat.packageName] = currentMillis + usageStat.totalTimeInForeground
            }
        }
        totalMillis = aggregatedMap.values.sum()
    }
    return totalMillis
}