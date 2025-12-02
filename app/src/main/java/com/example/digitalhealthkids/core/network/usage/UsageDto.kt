package com.example.digitalhealthkids.core.network.usage
import com.example.digitalhealthkids.domain.usage.AppUsageItem
import com.example.digitalhealthkids.domain.usage.DashboardData
import com.example.digitalhealthkids.domain.usage.*
import com.google.gson.annotations.SerializedName
data class UsageEventDto(
    @SerializedName("app_package") val appPackage: String,
    @SerializedName("app_name") val appName: String?,
    @SerializedName("start_time") val startTime: String,
    @SerializedName("end_time") val endTime: String,
    @SerializedName("total_seconds") val totalSeconds: Int
)

data class UsageReportRequestDto(
    @SerializedName("child_id") val childId: String,
    @SerializedName("device_id") val deviceId: String,
    val events: List<UsageEventDto>
)

data class UsageReportResponseDto(val status: String, val inserted: Int)

// 🔥 YENİ YAPILAR
data class AppUsageDto(
    @SerializedName("app_name") val appName: String,
    @SerializedName("package_name") val packageName: String,
    val minutes: Int
)

data class DailyStatDto(
    val date: String,
    @SerializedName("total_minutes") val totalMinutes: Int,
    val apps: List<AppUsageDto>
)

data class DashboardDto(
    @SerializedName("child_name") val childName: String,
    @SerializedName("today_total_minutes") val todayTotalMinutes: Int,
    @SerializedName("weekly_breakdown") val weeklyBreakdown: List<DailyStatDto>,
    @SerializedName("bedtime_start") val bedtimeStart: String?,
    @SerializedName("bedtime_end") val bedtimeEnd: String?
)

// Mappers
fun AppUsageDto.toDomain() = AppUsageItem(appName, packageName, minutes)
fun DailyStatDto.toDomain() = DailyStat(date, totalMinutes, apps.map { it.toDomain() })
fun DashboardDto.toDomain() = DashboardData(
    childName = childName,
    todayTotalMinutes = todayTotalMinutes,
    weeklyBreakdown = weeklyBreakdown.map { it.toDomain() }, // 🔥 Çeviri burada
    bedtimeStart = bedtimeStart,
    bedtimeEnd = bedtimeEnd
)