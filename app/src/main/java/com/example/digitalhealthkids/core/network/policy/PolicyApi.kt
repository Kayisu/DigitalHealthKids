package com.example.digitalhealthkids.core.network.policy

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Query

// Request DTO
data class ToggleBlockRequest(
    @com.google.gson.annotations.SerializedName("user_id") val userId: String,
    @com.google.gson.annotations.SerializedName("package_name") val packageName: String
)

interface PolicyApi {
    @GET("policy/current")
    suspend fun getCurrentPolicy(@Query("user_id") userId: String): PolicyResponseDto

    @POST("policy/toggle-block")
    suspend fun toggleBlock(@Body body: ToggleBlockRequest): PolicyResponseDto

    @PUT("policy/settings")
    suspend fun updatePolicySettings(
        @Query("user_id") userId: String,
        @Body body: PolicySettingsRequestDto
    ): PolicyResponseDto
}