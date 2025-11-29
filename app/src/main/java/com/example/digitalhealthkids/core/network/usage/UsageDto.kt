package com.example.digitalhealthkids.core.network.usage
import com.example.digitalhealthkids.domain.usage.AppUsageItem
import com.example.digitalhealthkids.domain.usage.DashboardData
import com.google.gson.annotations.SerializedName
data class UsageEventDto(
    @SerializedName("app_package")
    val appPackage: String,
    @SerializedName("app_name")
    val appName: String?,
    @SerializedName("start_time")
    val startTime: String, // ISO8601: "2025-11-19T10:00:00Z"
    @SerializedName("end_time")
    val endTime: String,
    @SerializedName("total_seconds")
    val totalSeconds: Int
)

data class UsageReportRequestDto(
    @SerializedName("child_id")
    val childId: String,
    @SerializedName("device_id")
    val deviceId: String,
    @SerializedName("events")
    val events: List<UsageEventDto>
)

data class UsageReportResponseDto(
    val status: String,
    val inserted: Int
)

data class AppUsageDto(
    @SerializedName("app_name") val appName: String,
    @SerializedName("package_name") val packageName: String,
    val category: String?,
    val minutes: Int
)

data class DashboardDto(
    @SerializedName("child_name") val childName: String,
    @SerializedName("today_total_minutes") val todayTotalMinutes: Int,
    @SerializedName("today_remaining_minutes") val todayRemainingMinutes: Int,
    @SerializedName("weekly_trend") val weeklyTrend: List<Int>,
    @SerializedName("top_apps") val topApps: List<AppUsageDto>,
    @SerializedName("bedtime_start") val bedtimeStart: String?,
    @SerializedName("bedtime_end") val bedtimeEnd: String?,
)

// DTO → domain mapping
fun AppUsageDto.toDomain() = AppUsageItem(
    appName = appName,
    packageName = packageName,
    category = category,
    minutes = minutes
)

fun DashboardDto.toDomain() = DashboardData(
    childName = childName,
    todayTotalMinutes = todayTotalMinutes,
    todayRemainingMinutes = todayRemainingMinutes,
    weeklyTrend = weeklyTrend,
    topApps = topApps.map { it.toDomain() },
    bedtimeStart = bedtimeStart,
    bedtimeEnd = bedtimeEnd
)