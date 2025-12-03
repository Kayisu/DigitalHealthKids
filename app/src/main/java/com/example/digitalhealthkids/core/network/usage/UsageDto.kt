package com.example.digitalhealthkids.core.network.usage

import com.google.gson.annotations.SerializedName
import com.example.digitalhealthkids.domain.usage.*

data class UsageEventDto(
    @SerializedName("app_package") val appPackage: String,
    @SerializedName("app_name") val appName: String?,
    @SerializedName("date_str") val dateStr: String, // 🔥 YENİ
    @SerializedName("total_seconds") val totalSeconds: Int
)

data class UsageReportRequestDto(
    @SerializedName("user_id") val userId: String,
    @SerializedName("device_id") val deviceId: String,
    val events: List<UsageEventDto>
)

data class UsageReportResponseDto(val status: String, val inserted: Int)

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
    @SerializedName("user_name") val userName: String,
    @SerializedName("today_total_minutes") val todayTotalMinutes: Int,
    @SerializedName("weekly_breakdown") val weeklyBreakdown: List<DailyStatDto>,
    @SerializedName("bedtime_start") val bedtimeStart: String?,
    @SerializedName("bedtime_end") val bedtimeEnd: String?
)

// Mappers
fun AppUsageDto.toDomain() = AppUsageItem(appName, packageName, minutes)
fun DailyStatDto.toDomain() = DailyStat(date, totalMinutes, apps.map { it.toDomain() })
fun DashboardDto.toDomain() = DashboardData(
    userName = userName,
    todayTotalMinutes = todayTotalMinutes,
    weeklyBreakdown = weeklyBreakdown.map { it.toDomain() },
    bedtimeStart = bedtimeStart,
    bedtimeEnd = bedtimeEnd
)