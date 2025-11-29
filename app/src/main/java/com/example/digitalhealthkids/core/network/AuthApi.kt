package com.example.digitalhealthkids.core.network

import retrofit2.http.Body
import retrofit2.http.POST

data class LoginRequest(val email: String, val password: String)

data class LoginResponse(
    val token: String,
    val refreshToken: String?,
    val childId: String,
    val deviceId: String? = null
)

interface AuthApi {
    @POST("auth/login")
    suspend fun login(@Body body: LoginRequest): LoginResponse
}