package com.example.digitalhealthkids.data.auth

import com.example.digitalhealthkids.core.network.AuthApi
import com.example.digitalhealthkids.core.network.LoginRequest
import com.example.digitalhealthkids.core.network.LoginResponse
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
}
