package com.example.digitalhealthkids.core.network.usage

import com.google.gson.annotations.SerializedName
import com.example.digitalhealthkids.domain.usage.*

data class UsageEventDto(
    @SerializedName("package_name") val packageName: String,
    @SerializedName("app_name") val appName: String,
    @SerializedName("timestamp_start") val timestampStart: Long,
    @SerializedName("timestamp_end") val timestampEnd: Long,
    @SerializedName("duration_seconds") val durationSeconds: Int
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
    val apps: List<AppUsageDto>? // Null gelebilir
)

data class DashboardDto(
    @SerializedName("user_name") val userName: String,
    @SerializedName("today_total_minutes") val todayTotalMinutes: Int,
    @SerializedName("weekly_breakdown") val weeklyBreakdown: List<DailyStatDto>?, // Null gelebilir
    @SerializedName("bedtime_start") val bedtimeStart: String?,
    @SerializedName("bedtime_end") val bedtimeEnd: String?
)

data class HourlyUsageDto(
    val hour: Int,
    val minutes: Int
)

data class SessionUsageDto(
    @SerializedName("started_at") val startedAt: String,
    @SerializedName("ended_at") val endedAt: String,
    val minutes: Int
)

data class AppDetailDto(
    val date: String,
    @SerializedName("package_name") val packageName: String,
    @SerializedName("app_name") val appName: String?,
    @SerializedName("total_minutes") val totalMinutes: Int,
    @SerializedName("night_minutes") val nightMinutes: Int,
    val hourly: List<HourlyUsageDto>,
    val sessions: List<SessionUsageDto>
)

// Mappers
fun AppUsageDto.toDomain() = AppUsageItem(appName, packageName, minutes)

fun DailyStatDto.toDomain() = DailyStat(
    date,
    totalMinutes,
    apps?.map { it.toDomain() } ?: emptyList() // Eğer apps null ise boş liste kullan
)

fun DashboardDto.toDomain() = DashboardData(
    userName = userName,
    todayTotalMinutes = todayTotalMinutes,
    weeklyBreakdown = weeklyBreakdown?.map { it.toDomain() } ?: emptyList(), // Eğer weeklyBreakdown null ise boş liste kullan
    bedtimeStart = bedtimeStart,
    bedtimeEnd = bedtimeEnd
)

fun AppDetailDto.toDomain() = AppDetail(
    date = date,
    packageName = packageName,
    appName = appName,
    totalMinutes = totalMinutes,
    nightMinutes = nightMinutes,
    hourly = hourly.map { HourlyUsageDomain(it.hour, it.minutes) },
    sessions = sessions.map { SessionUsageDomain(it.startedAt, it.endedAt, it.minutes) }
)