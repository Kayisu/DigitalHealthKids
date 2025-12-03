package com.example.digitalhealthkids.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.digitalhealthkids.core.network.usage.UsageApi
import com.example.digitalhealthkids.core.network.usage.UsageReportRequestDto
// 🔥 DEĞİŞEN IMPORT BURASI:
import com.example.digitalhealthkids.core.network.usage.readUsageEventsForRange
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@HiltWorker
class UsageSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val usageApi: UsageApi
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val prefs = applicationContext.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

            // User ID ve Device ID yoksa çalışamayız
            val userId = prefs.getString("user_id", null)
            val deviceId = prefs.getString("device_id", null)

            if (userId == null || deviceId == null) {
                return@withContext Result.failure()
            }

            // 🔥 ESKİSİ: readUsageEventsSince(applicationContext, lastSync)
            // 🔥 YENİSİ: readUsageEventsForRange(..., 1) -> 0=Bugün, 1=Bugün+Dün
            // Worker her çalıştığında son 48 saati günceller, böylece veri kaybı olmaz.
            val events = readUsageEventsForRange(applicationContext, 1)

            if (events.isNotEmpty()) {
                usageApi.reportUsage(
                    UsageReportRequestDto(userId, deviceId, events)
                )

                // Artık last_sync_time'a mantık olarak ihtiyacımız kalmadı ama
                // debug/log amaçlı tutmaya devam edebilirsin.
                prefs.edit().putLong("last_sync_time", System.currentTimeMillis()).apply()
            }

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }
}