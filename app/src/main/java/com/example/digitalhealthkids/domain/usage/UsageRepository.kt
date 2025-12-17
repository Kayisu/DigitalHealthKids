package com.example.digitalhealthkids.domain.usage

data class AppUsageItem(
    val appName: String,
    val packageName: String,
    val minutes: Int
)

data class DailyStat(
    val date: String,
    val totalMinutes: Int,
    val apps: List<AppUsageItem>
)

data class DashboardData(
    val userName: String,
    val todayTotalMinutes: Int,
    val weeklyBreakdown: List<DailyStat>,
    val bedtimeStart: String?,
    val bedtimeEnd: String?,
    val dailyLimitMinutes: Int? = null
)

interface UsageRepository {
    suspend fun getDashboard(userId: String): DashboardData
}