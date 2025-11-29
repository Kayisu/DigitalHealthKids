package com.example.digitalhealthkids.ui.home

import android.content.Context
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.digitalhealthkids.core.network.usage.UsageApi
import com.example.digitalhealthkids.core.network.usage.UsageReportRequestDto
import com.example.digitalhealthkids.core.network.usage.readUsageEventsForRange // <-- İMPORT ETTİK
import com.example.digitalhealthkids.data.worker.UsageSyncWorker
import com.example.digitalhealthkids.domain.usage.DashboardData
import com.example.digitalhealthkids.domain.usage.UsageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val usageRepository: UsageRepository,
    private val usageApi: UsageApi
) : ViewModel() {

    data class State(
        val isLoading: Boolean = true,
        val data: DashboardData? = null,
        val error: String? = null
    )

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state

    var selectedDay by mutableIntStateOf(0)
        private set

    fun selectDay(i: Int) {
        selectedDay = i
    }

    // 1. Manuel / İlk Açılış Senkronizasyonu
    fun syncUsageHistory(context: Context, childId: String, deviceId: String) {
        viewModelScope.launch {
            _state.value = State(isLoading = true)
            Log.d("UsageSync", "🚀 Senkronizasyon başladı. Child: $childId, Device: $deviceId")

            // ADIM 1: Veri Gönderme
            try {
                // 7 günlük veriyi iste
                val events = readUsageEventsForRange(context, 7)
                Log.d("UsageSync", "📦 Android'den okunan ham veri sayısı: ${events.size}")

                if (events.isNotEmpty()) {
                    events.take(3).forEach { Log.d("UsageSync", "   -> Örnek paket: ${it.appPackage} (${it.totalSeconds} sn)") }

                    val body = UsageReportRequestDto(
                        childId = childId,
                        deviceId = deviceId,
                        events = events
                    )
                    Log.d("UsageSync", "📤 Backend'e gönderiliyor...")

                    val response = usageApi.reportUsage(body)
                    Log.d("UsageSync", "✅ Backend Yanıtı: Status=${response.status}, Inserted=${response.inserted}")
                } else {
                    Log.w("UsageSync", "⚠️ Okunacak veri bulunamadı! Liste boş.")
                }
            } catch (e: Exception) {
                Log.e("UsageSync", "❌ Veri gönderme hatası: ${e.message}")
                e.printStackTrace()
            }

            // ADIM 2: Dashboard Çekme
            try {
                Log.d("UsageSync", "📥 Dashboard verisi çekiliyor...")
                val d = usageRepository.getDashboard(childId)
                Log.d("UsageSync", "📊 Dashboard alındı. Toplam süre: ${d.todayTotalMinutes} dk")
                _state.value = State(isLoading = false, data = d)
            } catch (e: Exception) {
                Log.e("UsageSync", "❌ Dashboard çekme hatası: ${e.message}")
                _state.value = State(isLoading = false, error = e.message)
            }
        }
    }

    // 2. Arka Plan Senkronizasyonunu Başlatma (Schedule)
    fun scheduleBackgroundSync(context: Context) {
        val syncRequest = PeriodicWorkRequestBuilder<UsageSyncWorker>(
            15, TimeUnit.MINUTES
        )
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "UsageSyncWork",
            ExistingPeriodicWorkPolicy.KEEP, // Varsa eskisini koru, tekrar başlatma
            syncRequest
        )
    }
}