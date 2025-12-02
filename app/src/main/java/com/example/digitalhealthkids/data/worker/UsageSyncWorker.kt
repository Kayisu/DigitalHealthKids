package com.example.digitalhealthkids.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.digitalhealthkids.core.network.usage.UsageApi
import com.example.digitalhealthkids.core.network.usage.UsageReportRequestDto
import com.example.digitalhealthkids.core.network.usage.readUsageEventsSince
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.core.content.edit

@HiltWorker
class UsageSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val usageApi: UsageApi
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val prefs = applicationContext.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            val lastSync = prefs.getLong("last_sync_time", 0L)

            // 🔥 Refactor: Key değişimi
            val userId = prefs.getString("user_id", null)
            val deviceId = prefs.getString("device_id", null)

            if (userId == null || deviceId == null) {
                return@withContext Result.failure()
            }

            val events = readUsageEventsSince(applicationContext, lastSync)

            if (events.isNotEmpty()) {
                usageApi.reportUsage(
                    UsageReportRequestDto(userId, deviceId, events) // 🔥 Refactor
                )

                prefs.edit {
                    putLong("last_sync_time", System.currentTimeMillis())
                }
            }

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }
}