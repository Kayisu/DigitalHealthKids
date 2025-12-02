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
            // 1. SharedPreferences'dan son senkronizasyon zamanını, childId ve deviceId'yi al
            val prefs = applicationContext.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            val lastSync = prefs.getLong("last_sync_time", 0L)
            val childId = prefs.getString("child_id", null)
            val deviceId = prefs.getString("device_id", null)

            if (childId == null || deviceId == null) {
                return@withContext Result.failure() // Login olmamış
            }

            // 2. Verileri oku
            val events = readUsageEventsSince(applicationContext, lastSync)

            if (events.isNotEmpty()) {
                // 3. Backend'e gönder
                usageApi.reportUsage(
                    UsageReportRequestDto(childId, deviceId, events)
                )

                // 4. Başarılı olursa zamanı güncelle
                // (events içindeki en son zaman damgasını alabiliriz veya şimdiki zamanı)
                prefs.edit {
                    putLong("last_sync_time", System.currentTimeMillis())
                }
            }

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry() // Hata olursa (internet yoksa) sonra tekrar dene
        }
    }
}