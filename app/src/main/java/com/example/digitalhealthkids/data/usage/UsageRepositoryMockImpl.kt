package com.example.digitalhealthkids.data.usage

import com.example.digitalhealthkids.domain.usage.AppUsageItem
import com.example.digitalhealthkids.domain.usage.DashboardData
import com.example.digitalhealthkids.domain.usage.UsageRepository
import kotlinx.coroutines.delay

class UsageRepositoryMockImpl : UsageRepository {
    override suspend fun getDashboard(childId: String): DashboardData {
        delay(500) // hafif loading efekti

        return DashboardData(
            childName = "Kaan",
            todayTotalMinutes = 95,
            todayRemainingMinutes = 25,
            weeklyTrend = listOf(60, 120, 90, 80, 100, 75, 95),
            topApps = listOf(
                AppUsageItem("com.youtube", "YouTube", 45, "Video"),
                AppUsageItem("com.roblox", "Roblox", 30, "Oyun"),
                AppUsageItem("com.whatsapp", "WhatsApp", 20, "Mesajlaşma")
            ),
            bedtimeStart = "21:30",
            bedtimeEnd = "07:00"
        )
    }
}
