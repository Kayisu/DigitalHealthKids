package com.example.digitalhealthkids.ui.home

import android.content.Context
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.*
import com.example.digitalhealthkids.core.network.policy.PolicyApi
import com.example.digitalhealthkids.core.network.usage.UsageApi
import com.example.digitalhealthkids.core.network.usage.UsageReportRequestDto
import com.example.digitalhealthkids.core.network.usage.readUsageEventsForRange
import com.example.digitalhealthkids.data.worker.UsageSyncWorker
import com.example.digitalhealthkids.domain.usage.DashboardData
import com.example.digitalhealthkids.domain.usage.UsageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import androidx.core.content.edit
import com.example.digitalhealthkids.core.network.policy.ToggleBlockRequest
import com.example.digitalhealthkids.data.worker.PolicySyncWorker


@HiltViewModel
class HomeViewModel @Inject constructor(
    private val usageRepository: UsageRepository,
    private val usageApi: UsageApi,
    private val policyApi: PolicyApi,
    @ApplicationContext private val context: Context
) : ViewModel() {

    data class AppUiModel(
        val packageName: String,
        val appName: String,
        val averageMinutes: Int,
        val isBlocked: Boolean
    )
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

    // Yasaklı Uygulamalar Listesi (Anlık Takip İçin)
    private val _blockedPackages = MutableStateFlow<Set<String>>(emptySet())
    val blockedPackages = _blockedPackages.asStateFlow()

    // Uygulama Listesi (UI için hazır veri)
    private val _appList = MutableStateFlow<List<AppUiModel>>(emptyList())
    val appList = _appList.asStateFlow()

    // Veriyi analiz edip listeyi hazırlar
    fun calculateAppStats() {
        val dashboard = state.value.data ?: return
        val currentBlocked = _blockedPackages.value

        // 1. Tüm haftanın verilerini düzleştir
        val allUsage = dashboard.weeklyBreakdown.flatMap { it.apps }

        // 2. Pakete göre grupla ve ortalama al
        val grouped = allUsage.groupBy { it.packageName }

        val uiModels = grouped.map { (pkg, usages) ->
            val totalMinutes = usages.sumOf { it.minutes }
            // Veri kaç günlükse ona böl (basitçe 7 diyelim veya gün sayısına böl)
            val avg = totalMinutes / 7
            val name = usages.firstOrNull()?.appName ?: "Bilinmeyen Uygulama"

            AppUiModel(
                packageName = pkg,
                appName = name,
                averageMinutes = avg,
                isBlocked = currentBlocked.contains(pkg)
            )
        }.sortedByDescending { it.averageMinutes } // En çok kullanılan en üstte

        _appList.value = uiModels
    }

    // Bloklama Butonuna Basılınca
    fun toggleAppBlock(userId: String, packageName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            // 1. ÖNCE LOCAL: Anında UI tepkisi ve Servis güncellemesi (Optimistic Update)
            val currentSet = _blockedPackages.value.toMutableSet()
            if (currentSet.contains(packageName)) {
                currentSet.remove(packageName)
            } else {
                currentSet.add(packageName)
            }

            // State güncelle (UI tetiklenir)
            _blockedPackages.value = currentSet
            // Listeyi tekrar hesapla ki butonun rengi değişsin
            calculateAppStats()

            // Servis için kaydet
            context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                .edit {
                    putStringSet("blocked_packages", currentSet)
                }

            // 2. SONRA BACKEND: Kalıcı hale getir
            try {
                policyApi.toggleBlock(ToggleBlockRequest(userId, packageName))
            } catch (e: Exception) {
                e.printStackTrace()
                // Hata olursa rollback yapılabilir (MVP için pas geçiyoruz)
            }
        }
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
                    val daysToSync = if (currentDashboard == null || currentDashboard.weeklyBreakdown.size < 7) {
                        Log.d("UsageSync", "Haftalık veri eksik (${currentDashboard?.weeklyBreakdown?.size ?: 0} gün var). Geçmiş taranıyor...")
                        6 // Son 7 günü (0..6) tara
                    } else {
                        Log.d("UsageSync", "Haftalık veri tam. Sadece bugün taranıyor...")
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
            fetchAndSavePolicy(userId)
        }
    }
    private val _currentLimit = MutableStateFlow<Int?>(null)
    val currentLimit = _currentLimit.asStateFlow()

    private suspend fun fetchAndSavePolicy(userId: String) {
        try {
            // 1. Backend'den kuralları çek
            val policy = policyApi.getCurrentPolicy(userId)
            _currentLimit.value = policy.dailyLimitMinutes

            // 2. Yasaklı listesini al ve State'i güncelle
            val blockedApps = policy.blockedApps.toSet()
            _blockedPackages.value = blockedApps
            calculateAppStats()

            val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            prefs.edit {
                // Bloklananlar
                putStringSet("blocked_packages", blockedApps)

                // Günlük Limit (Null ise -1 kaydedelim ki "yok" olduğunu anlayalım)
                putInt("daily_limit", policy.dailyLimitMinutes ?: -1)

                // Uyku Saatleri (Null ise sil)
                putString("bedtime_start", policy.bedtime?.start)
                putString("bedtime_end", policy.bedtime?.end)
            }

        } catch (e: Exception) {
            e.printStackTrace()
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

        val policyRequest = PeriodicWorkRequestBuilder<PolicySyncWorker>(
            15, TimeUnit.MINUTES
        ).setConstraints(
            Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
        ).build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "PolicySyncWork",
            ExistingPeriodicWorkPolicy.KEEP,
            policyRequest
        )
    }
}