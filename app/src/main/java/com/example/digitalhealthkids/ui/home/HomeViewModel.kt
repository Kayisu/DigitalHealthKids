package com.example.digitalhealthkids.ui.home

import android.content.Context
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.digitalhealthkids.core.network.usage.readUsageEventsForRange
import com.example.digitalhealthkids.data.worker.UsageSyncWorker
import com.example.digitalhealthkids.domain.usage.DashboardData
import com.example.digitalhealthkids.domain.usage.UsageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers // <-- Bunu eklemeyi unutma
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext // <-- Bunu eklemeyi unutma
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

    fun syncUsageHistory(context: Context, userId: String, deviceId: String) {
        viewModelScope.launch {
            _state.value = State(isLoading = true)
            Log.d("UsageSync", "Senkronizasyon başladı. User: $userId")

            try {
                // 🔥 DÜZELTME: Ağır işlemi IO thread'ine taşıdık
                withContext(Dispatchers.IO) {
                    // 1. Veriyi Oku (Çok ağır işlem)
                    val events = readUsageEventsForRange(context, 7) // 7 günlük veri

                    if (events.isNotEmpty()) {
                        Log.d("UsageSync", "${events.size} adet olay bulundu, gönderiliyor...")
                        val body = UsageReportRequestDto(
                            userId = userId,
                            deviceId = deviceId,
                            events = events
                        )
                        // 2. Sunucuya Gönder
                        usageApi.reportUsage(body)
                    } else {
                        Log.d("UsageSync", "Gönderilecek yeni olay bulunamadı.")
                    }

                    // 3. Güncel Dashboard'u Çek
                    val d = usageRepository.getDashboard(userId)

                    // UI güncellemesi için tekrar Main thread'e dönmemize gerek yok,
                    // postValue veya emit işlemleri thread-safe'dir,
                    // ama StateFlow direkt atama (value =) yapıyorsak Main'de olmalıydık.
                    // Ancak withContext bloğundan çıkan sonuçla aşağıda atama yapabiliriz.
                    d // withContext sonucu olarak d'yi döndür
                }.let { dashboardData ->
                    // Burası tekrar Main Thread (viewModelScope default)
                    _state.value = State(isLoading = false, data = dashboardData)
                }

            } catch (e: Exception) {
                Log.e("UsageSync", "Hata: ${e.message}")
                _state.value = State(isLoading = false, error = e.message)
            }
        }
    }

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
            ExistingPeriodicWorkPolicy.KEEP,
            syncRequest
        )
    }
}