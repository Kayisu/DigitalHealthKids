package com.example.digitalhealthkids.data.policy

import com.example.digitalhealthkids.core.network.policy.AutoPolicyResponseDto
import com.example.digitalhealthkids.core.network.policy.PolicyApi
import com.example.digitalhealthkids.core.network.policy.PolicyResponseDto
import com.example.digitalhealthkids.core.network.policy.PolicySettingsRequestDto
import com.example.digitalhealthkids.data.local.PolicyManager
import com.example.digitalhealthkids.domain.policy.PolicyRepository
import javax.inject.Inject

class PolicyRepositoryImpl @Inject constructor(
    private val api: PolicyApi,
    private val policyManager: PolicyManager
) : PolicyRepository {

    override suspend fun refreshPolicy(childId: String): Result<Unit> {
        return try {
            // 1. Backend'den güncel veriyi iste
            val remotePolicy = api.getCurrentPolicy(childId)

            // 2. Başarılıysa yerel hafızaya yaz (Overwrite)
            policyManager.updatePolicy(remotePolicy)

            Result.success(Unit)
        } catch (e: Exception) {
            // Hata durumunda (internet yoksa vb.) eski veriye dokunma!
            // Sadece hata döndür, uygulama eski kurallarla devam eder.
            Result.failure(e)
        }
    }

    override fun getCachedPolicy(): PolicyResponseDto? {
        return policyManager.getPolicy()
    }

    override suspend fun updateSettings(
        userId: String,
        settings: PolicySettingsRequestDto
    ): Result<Unit> {
        return try {
            // Backend'e yolla
            val updatedPolicy = api.updatePolicySettings(userId, settings)

            // Dönen güncel veriyi hemen Cache'e yaz (Böylece UI anında güncellenir)
            policyManager.updatePolicy(updatedPolicy)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun autoApplyPolicy(userId: String): Result<AutoPolicyResponseDto> {
        return try {
            val autoPolicy = api.autoApplyPolicy(userId)

            // Auto-apply güncel kuralları yazdığı için hemen çekip cache'leyelim
            val refreshed = api.getCurrentPolicy(userId)
            policyManager.updatePolicy(refreshed)

            Result.success(autoPolicy)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun autoPreviewPolicy(userId: String): Result<AutoPolicyResponseDto> {
        return try {
            val autoPolicy = api.autoPreviewPolicy(userId)
            Result.success(autoPolicy)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}