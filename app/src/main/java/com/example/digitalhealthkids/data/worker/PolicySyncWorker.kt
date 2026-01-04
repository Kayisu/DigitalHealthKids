package com.example.digitalhealthkids.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.digitalhealthkids.domain.policy.PolicyRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@HiltWorker
class PolicySyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val policyRepository: PolicyRepository
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val prefs = applicationContext.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            val userId = prefs.getString("user_id", null)

            if (userId == null) {
                return@withContext Result.failure()
            }

            // Repository üzerinden güncelleme isteği at
            val result = policyRepository.refreshPolicy(userId)

            if (result.isSuccess) {
                // Cache'i app_prefs'e yazarak engelleme servisini güncel tut
                policyRepository.getCachedPolicy()?.let { policy ->
                    val appPrefs = applicationContext.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                    appPrefs.edit().apply {
                        putStringSet("blocked_packages", policy.blockedApps.toSet())
                        putInt("daily_limit", policy.dailyLimitMinutes ?: -1)
                        putString("bedtime_start", policy.bedtime?.start)
                        putString("bedtime_end", policy.bedtime?.end)
                        apply()
                    }
                }
                Result.success()
            } else {
                // İnternet yoksa vs. sonra tekrar dene
                Result.retry()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }
}