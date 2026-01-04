package com.example.digitalhealthkids.domain.auth

import com.example.digitalhealthkids.core.network.LoginResponse
import com.example.digitalhealthkids.core.network.RegisterResponse
import com.example.digitalhealthkids.core.network.ProfileResponse

interface AuthRepository {
    suspend fun login(email: String, password: String): Result<LoginResponse>
    suspend fun register(email: String, password: String, fullName: String?, birthDate: String?): Result<RegisterResponse>
    suspend fun getProfile(userId: String): Result<ProfileResponse>
    suspend fun updateProfile(userId: String, fullName: String?, birthDate: String?): Result<ProfileResponse>
}
