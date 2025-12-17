package com.example.digitalhealthkids.core.network.policy

import com.google.gson.annotations.SerializedName

data class BedtimeDto(
    val start: String,
    val end: String
)

data class PolicySettingsRequestDto(
    @SerializedName("daily_limit_minutes") val dailyLimitMinutes: Int?,
    @SerializedName("bedtime_start") val bedtimeStart: String?,
    @SerializedName("bedtime_end") val bedtimeEnd: String?,
    @SerializedName("weekend_relax_pct") val weekendRelaxPct: Int,
    @SerializedName("blocked_packages") val blockedPackages: List<String>? = null
)

data class PolicyResponseDto(
    @SerializedName("user_id") val userId: String,
    @SerializedName("daily_limit_minutes") val dailyLimitMinutes: Int?,
    @SerializedName("blocked_apps") val blockedApps: List<String>,
    val bedtime: BedtimeDto? // Zaten nullable idi, sorun yok
)