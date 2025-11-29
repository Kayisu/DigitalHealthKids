package com.example.digitalhealthkids.domain.auth

import com.example.digitalhealthkids.core.network.LoginResponse

interface AuthRepository {
    suspend fun login(email: String, password: String): Result<LoginResponse>
}
