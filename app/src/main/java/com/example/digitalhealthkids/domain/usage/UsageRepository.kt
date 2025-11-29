package com.example.digitalhealthkids.domain.usage

data class AppUsageItem(
    val packageName: String,
    val appName: String,
    val minutes: Int,
    val category: String?
)

data class DashboardData(
    val childName: String,
    val todayTotalMinutes: Int,
    val todayRemainingMinutes: Int,
    val weeklyTrend: List<Int>,
    val topApps: List<AppUsageItem>,
    val bedtimeStart: String?,
    val bedtimeEnd: String?
)

interface UsageRepository {
    suspend fun getDashboard(childId: String): DashboardData
}
