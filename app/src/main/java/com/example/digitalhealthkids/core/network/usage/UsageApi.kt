package com.example.digitalhealthkids.core.network.usage
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface UsageApi {

    @POST("usage/report")
    suspend fun reportUsage(
        @Body body: UsageReportRequestDto
    ): UsageReportResponseDto

    @GET("usage/dashboard")
    suspend fun getDashboard(
        @Query("user_id") userId: String // 🔥 Refactor
    ): DashboardDto

    @GET("usage/app_detail")
    suspend fun getAppDetail(
        @Query("user_id") userId: String,
        @Query("package_name") packageName: String,
        @Query("target_date") targetDate: String
    ): AppDetailDto
}