package com.example.digitalhealthkids.core.network.usage

import android.app.AppOpsManager
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

// 🔥 Launcher ve Sistem Uygulamalarını Temizleme
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

/**
 * 🔥 JOHNNY SILVERHAND METHOD
 * Tek tek gün hesaplamak yerine, son 7 günü komple çekip,
 * "Timestamp -> Tarih Stringi" dönüşümünü burada yaparak
 * kesin ve net bir gruplama yapıyoruz.
 */
fun readUsageEventsForRange(context: Context, daysBack: Int): List<UsageEventDto> {
    if (!hasUsagePermission(context)) return emptyList()

    val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

    // 1. Sorgu Aralığını Belirle
    // daysBack = 6 ise, son 7 günü (Bugün dahil) çek.
    // daysBack = 0 ise, sadece bugünü (Gece 00:00'dan şu ana) çek.
    val cal = Calendar.getInstance()
    val endTime = System.currentTimeMillis() // Şu an

    // Başlangıç: daysBack kadar geriye git, o günün 00:00'ına in
    cal.add(Calendar.DAY_OF_YEAR, -daysBack)
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    val startTime = cal.timeInMillis

    // 2. Google'dan INTERVAL_DAILY olarak iste
    // Bu, bize o aralıktaki tüm günlerin parçalarını verir.
    val usageStatsList = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, startTime, endTime)

    if (usageStatsList.isNullOrEmpty()) return emptyList()

    // 3. Veriyi İşle: (Tarih Stringi -> Paket Adı -> Toplam Süre)
    val dailyMap = mutableMapOf<String, MutableMap<String, Long>>()

    for (stat in usageStatsList) {
        // Eğer süre 0 ise hiç uğraşma
        if (stat.totalTimeInForeground < 1000) continue

        // 🔥 KRİTİK NOKTA: Google'ın verdiği zaman damgasını, TELEFONUN yerel saatine göre tarihe çevir.
        // Böylece telefon ne gösteriyorsa biz de onu görürüz.
        val dateString = dateFormat.format(Date(stat.firstTimeStamp))

        // Gelecek tarihli hatalı verileri ele (System saati kaymaları)
        if (stat.firstTimeStamp > System.currentTimeMillis()) continue

        // Bu tarih için map'i hazırla
        val packageMap = dailyMap.getOrPut(dateString) { mutableMapOf() }

        // Paketi bul ve süreyi ekle (Eğer aynı gün için birden fazla parça varsa topla)
        val currentTotal = packageMap.getOrDefault(stat.packageName, 0L)
        packageMap[stat.packageName] = currentTotal + stat.totalTimeInForeground
    }

    // 4. DTO'ya Dönüştür
    val resultList = mutableListOf<UsageEventDto>()

    dailyMap.forEach { (dateStr, pkgMap) ->
        pkgMap.forEach { (pkgName, totalTimeMillis) ->
            if (isUserApp(context, pkgName)) {
                resultList.add(
                    UsageEventDto(
                        appPackage = pkgName,
                        appName = resolveAppName(context, pkgName),
                        dateStr = dateStr, // "2025-12-03"
                        totalSeconds = (totalTimeMillis / 1000).toInt()
                    )
                )
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