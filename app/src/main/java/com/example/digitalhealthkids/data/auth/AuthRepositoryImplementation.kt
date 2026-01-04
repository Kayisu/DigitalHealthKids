package com.example.digitalhealthkids.data.auth

import com.example.digitalhealthkids.core.network.AuthApi
import com.example.digitalhealthkids.core.network.LoginRequest
import com.example.digitalhealthkids.core.network.LoginResponse
import com.example.digitalhealthkids.core.network.ProfileResponse
import com.example.digitalhealthkids.core.network.RegisterRequest
import com.example.digitalhealthkids.core.network.RegisterResponse
import com.example.digitalhealthkids.core.network.UpdateProfileRequest
import com.example.digitalhealthkids.domain.auth.AuthRepository

class AuthRepositoryImplementation(
    private val api: AuthApi
) : AuthRepository {

    override suspend fun login(email: String, password: String): Result<LoginResponse> = try {
        val resp = api.login(LoginRequest(email, password))
        Result.success(resp)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun register(email: String, password: String, fullName: String?, birthDate: String?): Result<RegisterResponse> = try {
        val resp = api.register(RegisterRequest(email = email, password = password, full_name = fullName, birth_date = birthDate))
        Result.success(resp)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun getProfile(userId: String): Result<ProfileResponse> = try {
        Result.success(api.getProfile(userId))
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun updateProfile(userId: String, fullName: String?, birthDate: String?): Result<ProfileResponse> = try {
        val resp = api.updateProfile(userId, UpdateProfileRequest(full_name = fullName, birth_date = birthDate))
        Result.success(resp)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
