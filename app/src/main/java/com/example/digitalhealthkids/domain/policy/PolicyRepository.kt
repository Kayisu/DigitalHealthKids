package com.example.digitalhealthkids.domain.policy

import com.example.digitalhealthkids.core.network.policy.PolicyResponseDto
import com.example.digitalhealthkids.core.network.policy.PolicySettingsRequestDto
import com.example.digitalhealthkids.core.network.policy.AutoPolicyResponseDto

interface PolicyRepository {
    suspend fun refreshPolicy(childId: String): Result<Unit>
    fun getCachedPolicy(): PolicyResponseDto?
    suspend fun updateSettings(userId: String, settings: PolicySettingsRequestDto): Result<Unit>
    suspend fun autoApplyPolicy(userId: String): Result<AutoPolicyResponseDto>
    suspend fun autoPreviewPolicy(userId: String): Result<AutoPolicyResponseDto>
}