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
import com.example.digitalhealthkids.data.worker.PolicySyncWorker
import com.example.digitalhealthkids.core.network.policy.PolicySettingsRequestDto
import com.example.digitalhealthkids.domain.policy.PolicyRepository
import com.example.digitalhealthkids.core.util.CategoryLabels


@HiltViewModel
class HomeViewModel @Inject constructor(
    private val usageRepository: UsageRepository,
    private val usageApi: UsageApi,
    private val policyRepository: PolicyRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    data class AppUiModel(
        val packageName: String,
        val appName: String,
        val averageMinutes: Int,
        val isBlocked: Boolean,
        val category: String? = null
    )

    data class CategoryHighlight(
        val name: String,
        val totalMinutes: Int,
        val appCount: Int
    )

    enum class SortOption { WEEKLY_DESC, WEEKLY_ASC, NAME, CATEGORY }

    enum class ListMode { ALL, CATEGORY }

    enum class ViewMode { OVERVIEW, ALL_APPS, CATEGORIES, CATEGORY_DETAIL }

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

    private val _visibleApps = MutableStateFlow<List<AppUiModel>>(emptyList())
    val visibleApps = _visibleApps.asStateFlow()

    private val _topApps = MutableStateFlow<List<AppUiModel>>(emptyList())
    val topApps = _topApps.asStateFlow()

    private val _topCategories = MutableStateFlow<List<CategoryHighlight>>(emptyList())
    val topCategories = _topCategories.asStateFlow()

    private val _allCategories = MutableStateFlow<List<CategoryHighlight>>(emptyList())
    val allCategories = _allCategories.asStateFlow()

    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory = _selectedCategory.asStateFlow()

    private val _listMode = MutableStateFlow(ListMode.ALL)
    val listMode = _listMode.asStateFlow()

    private val _viewMode = MutableStateFlow(ViewMode.OVERVIEW)
    val viewMode = _viewMode.asStateFlow()

    private val _pageIndex = MutableStateFlow(0)
    val pageIndex = _pageIndex.asStateFlow()

    private val _hasMore = MutableStateFlow(false)
    val hasMore = _hasMore.asStateFlow()

    private val pageSize = 30

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _sortOption = MutableStateFlow(SortOption.WEEKLY_DESC)
    val sortOption = _sortOption.asStateFlow()

    fun calculateAppStats() {
        val dashboard = state.value.data
        if (dashboard == null) {
            _appList.value = emptyList()
            recomputeDerived()
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
            val category = CategoryLabels.labelFor(usages.firstOrNull()?.category)
            AppUiModel(
                packageName = pkg,
                appName = name,
                averageMinutes = avg,
                isBlocked = currentBlocked.contains(pkg),
                category = category
            )
        }.sortedByDescending { it.averageMinutes }
        _appList.value = uiModels
        recomputeDerived()
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
                val cached = policyRepository.getCachedPolicy()
                val request = PolicySettingsRequestDto(
                    dailyLimitMinutes = cached?.dailyLimitMinutes ?: _currentLimit.value,
                    bedtimeStart = cached?.bedtime?.start,
                    bedtimeEnd = cached?.bedtime?.end,
                    weekendRelaxPct = 0,
                    blockedPackages = currentSet.toList()
                )

                policyRepository.updateSettings(userId, request)

                // Güncel veriyi çekip cache + prefs'i senkronla
                policyRepository.refreshPolicy(userId)
                policyRepository.getCachedPolicy()?.let { policy ->
                    _currentLimit.value = policy.dailyLimitMinutes
                    _blockedPackages.value = policy.blockedApps.toSet()
                    context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE).edit {
                        putStringSet("blocked_packages", policy.blockedApps.toSet())
                        putInt("daily_limit", policy.dailyLimitMinutes ?: -1)
                        putString("bedtime_start", policy.bedtime?.start)
                        putString("bedtime_end", policy.bedtime?.end)
                    }
                }
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
            policyRepository.refreshPolicy(userId)
            policyRepository.getCachedPolicy()?.let { policy ->
                _currentLimit.value = policy.dailyLimitMinutes
                _blockedPackages.value = policy.blockedApps.toSet()
                calculateAppStats()

                context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE).edit {
                    putStringSet("blocked_packages", policy.blockedApps.toSet())
                    putInt("daily_limit", policy.dailyLimitMinutes ?: -1)
                    putString("bedtime_start", policy.bedtime?.start)
                    putString("bedtime_end", policy.bedtime?.end)
                }
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

    fun updateSearchQuery(value: String) {
        _searchQuery.value = value
        resetPaging()
        recomputeDerived()
    }

    fun updateSortOption(option: SortOption) {
        _sortOption.value = option
        resetPaging()
        recomputeDerived()
    }

    fun loadMore() {
        _pageIndex.value = _pageIndex.value + 1
        recomputeDerived()
    }

    fun resetPaging() {
        _pageIndex.value = 0
    }

    fun showAllApps() {
        _listMode.value = ListMode.ALL
        _selectedCategory.value = null
        _viewMode.value = ViewMode.ALL_APPS
        resetPaging()
        recomputeDerived()
    }

    fun showCategory(key: String) {
        _listMode.value = ListMode.CATEGORY
        _selectedCategory.value = key
        _viewMode.value = ViewMode.CATEGORY_DETAIL
        resetPaging()
        recomputeDerived()
    }

    fun showCategoriesGrid() {
        _viewMode.value = ViewMode.CATEGORIES
    }

    fun showOverview() {
        _viewMode.value = ViewMode.OVERVIEW
        _listMode.value = ListMode.ALL
        _selectedCategory.value = null
        resetPaging()
        recomputeDerived()
    }

    private fun recomputeDerived() {
        val baseAll = _appList.value
        val query = _searchQuery.value.trim().lowercase()
        val sort = _sortOption.value
        val mode = _listMode.value
        val selectedCat = _selectedCategory.value

        val base = when (mode) {
            ListMode.ALL -> baseAll
            ListMode.CATEGORY -> baseAll.filter { (it.category ?: CategoryLabels.defaultLabel) == (selectedCat ?: "") }
        }

        var list = if (query.isEmpty()) base else base.filter {
            it.appName.lowercase().contains(query) || it.packageName.lowercase().contains(query)
        }

        list = when (sort) {
            SortOption.WEEKLY_DESC -> list.sortedByDescending { it.averageMinutes }
            SortOption.WEEKLY_ASC -> list.sortedBy { it.averageMinutes }
            SortOption.NAME -> list.sortedBy { it.appName.lowercase() }
            SortOption.CATEGORY -> list.sortedWith(compareBy({ it.category ?: "zzz" }, { it.appName.lowercase() }))
        }

        val end = pageSize * (_pageIndex.value + 1)
        _visibleApps.value = list.take(end)
        _hasMore.value = list.size > end

        _topApps.value = baseAll.sortedByDescending { it.averageMinutes }.take(3)

        val categoryTotals = baseAll.groupBy { it.category ?: CategoryLabels.defaultLabel }.map { (cat, items) ->
            CategoryHighlight(cat, items.sumOf { it.averageMinutes }, items.size)
        }.sortedByDescending { it.totalMinutes }
        _allCategories.value = categoryTotals
        _topCategories.value = categoryTotals.take(4)
    }
}