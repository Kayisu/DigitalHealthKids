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
    val bedtime: BedtimeDto?, // Zaten nullable idi, sorun yok
    @SerializedName("weekend_extra_minutes") val weekendExtraMinutes: Int,
    @SerializedName("holiday_relax_pct") val holidayRelaxPct: Int = 0
)

data class PolicyRecommendationsDto(
    val suggestions: List<String>
)

data class AutoPolicyAppLimitDto(
    @SerializedName("package_name") val packageName: String,
    @SerializedName("limit_minutes") val limitMinutes: Int,
    val category: String?,
    val share: Double
)

data class AutoPolicyResponseDto(
    @SerializedName("user_id") val userId: String,
    @SerializedName("window_days") val windowDays: Int,
    @SerializedName("stage1_daily_limit") val stage1DailyLimit: Int,
    @SerializedName("stage2_daily_limit") val stage2DailyLimit: Int,
    @SerializedName("weekend_relax_pct") val weekendRelaxPct: Int,
    @SerializedName("app_limits") val appLimits: List<AutoPolicyAppLimitDto>,
    @SerializedName("bedtime_start") val bedtimeStart: String?,
    @SerializedName("bedtime_end") val bedtimeEnd: String?,
    @SerializedName("fallback_used") val fallbackUsed: Boolean,
    val message: String?
)