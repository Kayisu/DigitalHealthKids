package com.example.digitalhealthkids.domain.usage

data class AppUsageItem(
    val appName: String,
    val packageName: String,
    val minutes: Int
)

data class DailyStat(
    val date: String,      // Örn: "2025-11-29"
    val totalMinutes: Int,
    val apps: List<AppUsageItem>
)

data class DashboardData(
    val childName: String,
    val todayTotalMinutes: Int,
    val weeklyBreakdown: List<DailyStat>,
    val bedtimeStart: String?,
    val bedtimeEnd: String?
)

interface UsageRepository {
    suspend fun getDashboard(childId: String): DashboardData
}