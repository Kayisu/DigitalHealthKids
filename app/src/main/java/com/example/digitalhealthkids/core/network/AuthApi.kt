package com.example.digitalhealthkids.core.network

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

data class LoginRequest(val email: String, val password: String)

data class LoginResponse(
    val token: String,
    val refreshToken: String?,
    val userId: String,
    val deviceId: String? = null
)

data class RegisterRequest(
    val email: String,
    val password: String,
    val full_name: String? = null,
    val birth_date: String? = null
)

data class RegisterResponse(
    val userId: String,
    val deviceId: String?
)

data class ProfileResponse(
    val userId: String,
    val email: String,
    val full_name: String?,
    val birth_date: String?
)

data class UpdateProfileRequest(
    val full_name: String?,
    val birth_date: String?
)

interface AuthApi {
    @POST("auth/login")
    suspend fun login(@Body body: LoginRequest): LoginResponse

    @POST("auth/register")
    suspend fun register(@Body body: RegisterRequest): RegisterResponse

    @GET("auth/profile/{userId}")
    suspend fun getProfile(@Path("userId") userId: String): ProfileResponse

    @PUT("auth/profile/{userId}")
    suspend fun updateProfile(
        @Path("userId") userId: String,
        @Body body: UpdateProfileRequest
    ): ProfileResponse
}