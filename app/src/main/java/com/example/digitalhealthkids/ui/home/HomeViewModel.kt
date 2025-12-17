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

    private val _blockedPackages = MutableStateFlow<Set<String>>(emptySet())
    val blockedPackages = _blockedPackages.asStateFlow()

    private val _appList = MutableStateFlow<List<AppUiModel>>(emptyList())
    val appList = _appList.asStateFlow()

    fun calculateAppStats() {
        val dashboard = state.value.data
        if (dashboard == null) {
            _appList.value = emptyList()
            return
        }

        val currentBlocked = _blockedPackages.value
        val allUsage = dashboard.weeklyBreakdown.flatMap { it.apps }
        val grouped = allUsage.groupBy { it.packageName }

        val uiModels = grouped.map { (pkg, usages) ->
            val totalMinutes = usages.sumOf { it.minutes }
            // Ortalama için 7'ye bölmek yerine, verinin olduğu gün sayısına bölmek daha doğru olabilir,
            // ama şimdilik basit tutalım.
            val avg = totalMinutes / 7
            val name = usages.firstOrNull()?.appName ?: "Bilinmeyen Uygulama"
            AppUiModel(
                packageName = pkg,
                appName = name,
                averageMinutes = avg,
                isBlocked = currentBlocked.contains(pkg)
            )
        }.sortedByDescending { it.averageMinutes }
        _appList.value = uiModels
    }

    fun toggleAppBlock(userId: String, packageName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val currentSet = _blockedPackages.value.toMutableSet()
            if (currentSet.contains(packageName)) currentSet.remove(packageName) else currentSet.add(packageName)
            _blockedPackages.value = currentSet
            calculateAppStats()

            context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                .edit { putStringSet("blocked_packages", currentSet) }

            try {
                policyApi.toggleBlock(ToggleBlockRequest(userId, packageName))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun syncUsageHistory(context: Context, userId: String, deviceId: String) {
        viewModelScope.launch {
            if (_state.value.data == null) _state.value = State(isLoading = true)

            // Politikaları ve verileri eş zamanlı olarak, birbirini beklemeden başlat
            launch { fetchAndSavePolicy(userId) }

            try {
                withContext(Dispatchers.IO) {
                    // 1. Önce lokal veriyi göster (varsa)
                    var currentDashboard = usageRepository.getDashboard(userId)
                    if (currentDashboard != null) {
                        _state.value = State(isLoading = false, data = currentDashboard)
                        calculateAppStats()
                    }

                    // 2. Android'den temiz ve toplanmış veriyi oku
                    val daysToSync = if (currentDashboard == null || currentDashboard.weeklyBreakdown.size < 7) 6 else 0
                    val events = readUsageEventsForRange(context, daysToSync)

                    // 3. Temiz veriyi sunucuya gönder
                    if (events.isNotEmpty()) {
                        val body = UsageReportRequestDto(userId, deviceId, events)
                        usageApi.reportUsage(body)

                        // 4. Sunucudan gelen son halini çek ve UI'ı güncelle
                        currentDashboard = usageRepository.getDashboard(userId)
                        _state.value = State(isLoading = false, data = currentDashboard)
                        calculateAppStats() // EN ÖNEMLİ ÇAĞRI: Liste burada dolacak
                    } else {
                         // Eğer gönderilecek yeni bir şey yoksa bile yükleniyor durumunu kapat
                        _state.value = _state.value.copy(isLoading = false)
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

    private val _currentLimit = MutableStateFlow<Int?>(null)
    val currentLimit = _currentLimit.asStateFlow()

    private suspend fun fetchAndSavePolicy(userId: String) {
        try {
            val policy = policyApi.getCurrentPolicy(userId)
            _currentLimit.value = policy.dailyLimitMinutes
            _blockedPackages.value = policy.blockedApps.toSet()
            calculateAppStats()

            context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE).edit {
                putStringSet("blocked_packages", policy.blockedApps.toSet())
                putInt("daily_limit", policy.dailyLimitMinutes ?: -1)
                putString("bedtime_start", policy.bedtime?.start)
                putString("bedtime_end", policy.bedtime?.end)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun scheduleBackgroundSync(context: Context) {
        val constraints = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
        val usageRequest = PeriodicWorkRequestBuilder<UsageSyncWorker>(15, TimeUnit.MINUTES).setConstraints(constraints).build()
        val policyRequest = PeriodicWorkRequestBuilder<PolicySyncWorker>(15, TimeUnit.MINUTES).setConstraints(constraints).build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork("UsageSyncWork", ExistingPeriodicWorkPolicy.KEEP, usageRequest)
        WorkManager.getInstance(context).enqueueUniquePeriodicWork("PolicySyncWork", ExistingPeriodicWorkPolicy.KEEP, policyRequest)
    }
}