package com.example.digitalhealthkids.ui.home

import android.content.Context
import androidx.compose.runtime.getValue
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

    var selectedDay by mutableStateOf(0)
        private set

    fun selectDay(i: Int) {
        selectedDay = i
    }

    // 1. Manuel / İlk Açılış Senkronizasyonu
    fun syncUsageHistory(
        context: Context,
        childId: String,
        deviceId: String
    ) {
        viewModelScope.launch {
            _state.value = State(isLoading = true)

            // ADIM 1: Veri Göndermeyi Dene
            try {
                // Artık UsageReader dosyasında tanımlı, hata vermemeli
                val events = readUsageEventsForRange(context, 7)

                if (events.isNotEmpty()) {
                    val body = UsageReportRequestDto(
                        childId = childId,
                        deviceId = deviceId,
                        events = events
                    )
                    usageApi.reportUsage(body)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                // Hata olsa da devam et
            }

            // ADIM 2: Dashboard'u Çek
            try {
                val d = usageRepository.getDashboard(childId)
                _state.value = State(isLoading = false, data = d)
            } catch (e: Exception) {
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

    // sendUsage fonksiyonunu SİLDİM (Artık gereksiz)
}