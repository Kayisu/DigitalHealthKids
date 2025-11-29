package com.example.digitalhealthkids.core.network.policy

import retrofit2.http.GET
import retrofit2.http.Query

interface PolicyApi {

    @GET("api/v1/policy/current")
    suspend fun getCurrentPolicy(
        @Query("child_id") childId: String
    ): PolicyResponseDto
}
