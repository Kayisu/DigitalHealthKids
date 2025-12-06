package com.example.digitalhealthkids.domain.policy

import com.example.digitalhealthkids.core.network.policy.PolicyResponseDto
import com.example.digitalhealthkids.core.network.policy.PolicySettingsRequestDto

interface PolicyRepository {
    suspend fun refreshPolicy(childId: String): Result<Unit>
    fun getCachedPolicy(): PolicyResponseDto?
    suspend fun updateSettings(userId: String, settings: PolicySettingsRequestDto): Result<Unit>
}