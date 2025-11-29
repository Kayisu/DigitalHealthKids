package com.example.digitalhealthkids.core.network.policy

import com.google.gson.annotations.SerializedName

data class BedtimeDto(
    val start: String,
    val end: String
)

data class PolicyResponseDto(
    @SerializedName("child_id")
    val childId: String,
    @SerializedName("daily_limit_minutes")
    val dailyLimitMinutes: Int,
    @SerializedName("blocked_apps")
    val blockedApps: List<String>,
    val bedtime: BedtimeDto?
)
