package com.example.digitalhealthkids.ui.home

import android.content.Context
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.*
import com.example.digitalhealthkids.core.network.usage.UsageApi
import com.example.digitalhealthkids.core.network.usage.UsageReportRequestDto
import com.example.digitalhealthkids.core.network.usage.readUsageEventsForRange
import com.example.digitalhealthkids.data.worker.UsageSyncWorker
import com.example.digitalhealthkids.domain.usage.DashboardData
import com.example.digitalhealthkids.domain.usage.UsageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
            // Sadece ilk başta loading göster, veri varsa gösterme (kullanıcıyı rahatsız etme)
            if (_state.value.data == null) {
                _state.value = State(isLoading = true)
            }

            try {
                withContext(Dispatchers.IO) {
                    // 1. ÖNCE DATABASE'DEKİ MEVCUT VERİYİ ÇEK (Hızlı Gösterim)
                    var currentDashboard = try {
                        usageRepository.getDashboard(userId)
                    } catch (e: Exception) { null }

                    // Eğer data geldiyse hemen ekrana bas
                    if (currentDashboard != null) {
                        _state.value = State(isLoading = false, data = currentDashboard)
                    }

                    // 2. "İLK AÇILIŞ" KONTROLÜ
                    // Eğer dashboard boş geldiyse veya haftalık veri eksikse -> İLK KURULUM DEMEKTİR.
                    // O zaman son 7 günü tara. Değilse sadece bugünü (0) tara.
                    val daysToSync = if (currentDashboard == null || currentDashboard.weeklyBreakdown.isEmpty()) {
                        Log.d("UsageSync", "İlk kurulum veya boş veri: Son 7 gün taranıyor...")
                        6 // 0..6 toplam 7 gün
                    } else {
                        Log.d("UsageSync", "Rutin güncelleme: Sadece bugün taranıyor...")
                        0 // Sadece bugün
                    }

                    // 3. ANDROID'DEN VERİYİ OKU
                    val events = readUsageEventsForRange(context, daysToSync)

                    // 4. BACKEND'E GÖNDER (Varsa)
                    if (events.isNotEmpty()) {
                        val body = UsageReportRequestDto(
                            userId = userId,
                            deviceId = deviceId,
                            events = events
                        )
                        usageApi.reportUsage(body)
                        Log.d("UsageSync", "Backend güncellendi.")

                        // 5. GÜNCEL VERİYİ TEKRAR ÇEK (Senkronizasyon bitti, son hali al)
                        currentDashboard = usageRepository.getDashboard(userId)
                        _state.value = State(isLoading = false, data = currentDashboard)
                    } else {
                        // Gönderilecek veri yoksa ve elimizde dashboard varsa yüklemeyi bitir
                        if (_state.value.data != null) {
                            _state.value = _state.value.copy(isLoading = false)
                        } else {
                            // Hem veri yok hem dashboard yoksa (yeni gün, hiç kullanım yok)
                            _state.value = State(isLoading = false, data = null) // veya boş dashboard
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("UsageSync", "Hata: ${e.message}")
                if (_state.value.data == null) {
                    _state.value = State(isLoading = false, error = e.message)
                }
            }
        }
    }

    // ... scheduleBackgroundSync aynı kalabilir ...
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