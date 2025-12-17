package com.example.digitalhealthkids.core

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.digitalhealthkids.data.worker.PolicySyncWorker
import com.example.digitalhealthkids.data.worker.UsageSyncWorker
import dagger.hilt.android.HiltAndroidApp
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltAndroidApp
class DigitalHealthKidsApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    // Hilt'in Worker'ları tanıması için bu konfigürasyon şart
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        setupBackgroundWorkers()
    }

    private fun setupBackgroundWorkers() {
        // WorkManager'ı başlat
        val workManager = WorkManager.getInstance(this)

        // 1. KURAL: Sadece internet varsa çalışsın
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        // 2. GÖREV: Politika Senkronizasyonu (15 dk arayla)
        // Ebeveyn bir kural koyduğunda en geç 15 dk sonra telefona iner.
        val policyRequest = PeriodicWorkRequestBuilder<PolicySyncWorker>(15, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .build()

        // 3. GÖREV: Kullanım Raporlama (15 dk arayla)
        // Çocuğun verisi 15 dk'da bir sunucuya gider.
        val usageRequest = PeriodicWorkRequestBuilder<UsageSyncWorker>(15, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .build()

        // 4. KUYRUKLAMA: "KEEP" diyerek, eğer zaten kuruluysa yenisini ekleme, bozulmasın.
        workManager.enqueueUniquePeriodicWork(
            "sync_policy_work",
            ExistingPeriodicWorkPolicy.KEEP,
            policyRequest
        )

        workManager.enqueueUniquePeriodicWork(
            "sync_usage_work",
            ExistingPeriodicWorkPolicy.KEEP,
            usageRequest
        )
    }
}