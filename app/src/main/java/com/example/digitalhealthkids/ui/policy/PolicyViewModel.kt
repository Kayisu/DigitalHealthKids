package com.example.digitalhealthkids.ui.policy

import android.content.Context
import androidx.core.content.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.digitalhealthkids.core.network.policy.AutoPolicyResponseDto
import com.example.digitalhealthkids.core.network.policy.PolicyResponseDto
import com.example.digitalhealthkids.core.network.policy.PolicySettingsRequestDto
import com.example.digitalhealthkids.domain.policy.PolicyRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PolicyViewModel @Inject constructor(
    private val repository: PolicyRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    data class PolicyState(
        val isLoading: Boolean = false,
        val isAutoLoading: Boolean = false,
        val isAutoApplying: Boolean = false,
        val isPolicyClearing: Boolean = false,
        val policy: PolicyResponseDto? = null,
        val autoPolicy: AutoPolicyResponseDto? = null,
        val autoApplied: Boolean = false,
        val error: String? = null
    )

    private val _state = MutableStateFlow(PolicyState())
    val state: StateFlow<PolicyState> = _state

    fun loadPolicy(childId: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            val cached = repository.getCachedPolicy()
            if (cached != null) {
                _state.value = _state.value.copy(policy = cached)
            }

            val result = repository.refreshPolicy(childId)
            if (result.isSuccess) {
                val freshPolicy = repository.getCachedPolicy()
                _state.value = _state.value.copy(isLoading = false, policy = freshPolicy)
                if (freshPolicy != null) updateServicePrefs(freshPolicy)
            } else {
                _state.value = _state.value.copy(
                    isLoading = false,
                    policy = cached,
                    error = result.exceptionOrNull()?.message
                )
            }
        }
    }

    // 🔥 EKSİK OLAN FONKSİYON BURASI 🔥
    // NavGraph ve AppDetailScreen tarafından çağrılıyor
    fun addPolicy(userId: String, packageName: String, limitMinutes: Int) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)

            // Mevcut politikayı al, yoksa sunucudan çekmeyi dene
            var currentPolicy = _state.value.policy
            if (currentPolicy == null) {
                repository.refreshPolicy(userId)
                currentPolicy = repository.getCachedPolicy()
            }

            if (currentPolicy == null) {
                _state.value = _state.value.copy(isLoading = false, error = "Policy not loaded")
                return@launch
            }

            val currentBlocked = currentPolicy.blockedApps.toMutableSet()

            // limitMinutes kullanım senaryosu:
            // >0  : günlük limit belirle (global) ve mevcut block listesini koru
            // 0   : ilgili app'i engelle
            // <0  : ilgili app'in engelini kaldır
            val newDailyLimit = when {
                limitMinutes == -2 -> null // özel sinyal: limiti kaldır
                limitMinutes > 0 -> limitMinutes
                else -> currentPolicy.dailyLimitMinutes
            }

            when {
                limitMinutes == 0 -> currentBlocked.add(packageName)
                limitMinutes < 0 -> currentBlocked.remove(packageName)
            }

            // Backend'e göndereceğimiz request (Sadece blocked listesini güncelliyoruz şimdilik)
            val request = PolicySettingsRequestDto(
                dailyLimitMinutes = newDailyLimit,
                bedtimeStart = currentPolicy.bedtime?.start,
                bedtimeEnd = currentPolicy.bedtime?.end,
                weekendRelaxPct = currentPolicy.weekendExtraMinutes,
                blockedPackages = currentBlocked.toList() // Yeni liste
            )

            // API Çağrısı
            // Not: updateSettings fonksiyonunu repository'de blockedPackages alacak şekilde güncellemiş varsayıyoruz.
            // Eğer repository sadece settings alıyorsa, backend tarafı bu veriyi merge etmeli.
            val result = repository.updateSettings(currentPolicy.userId ?: userId, request)

            if (result.isSuccess) {
                // Başarılıysa cache'i güncelle
                val updatedPolicy = repository.getCachedPolicy()
                _state.value = _state.value.copy(isLoading = false, policy = updatedPolicy)
                if (updatedPolicy != null) updateServicePrefs(updatedPolicy)
            } else {
                _state.value = _state.value.copy(isLoading = false, error = result.exceptionOrNull()?.message)
            }
        }
    }

    fun updateSettings(
        userId: String,
        limitMinutes: Int?,
        startTime: String?,
        endTime: String?,
        weekendRelaxPct: Int
    ) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)

            // Mevcut blocked listesini korumak için state'den alıyoruz
            val currentBlocked = _state.value.policy?.blockedApps ?: emptyList()

            val request = PolicySettingsRequestDto(
                dailyLimitMinutes = limitMinutes,
                bedtimeStart = startTime,
                bedtimeEnd = endTime,
                weekendRelaxPct = weekendRelaxPct,
                blockedPackages = currentBlocked // Mevcut listeyi koru
            )

            val result = repository.updateSettings(userId, request)

            if (result.isSuccess) {
                val updatedPolicy = repository.getCachedPolicy()
                _state.value = _state.value.copy(isLoading = false, policy = updatedPolicy)
                if (updatedPolicy != null) updateServicePrefs(updatedPolicy)
            } else {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = result.exceptionOrNull()?.message
                )
            }
        }
    }

    fun previewAutoPolicy(userId: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isAutoLoading = true, error = null, autoApplied = false)

            val result = repository.autoPreviewPolicy(userId)
            if (result.isSuccess) {
                _state.value = _state.value.copy(
                    isAutoLoading = false,
                    autoPolicy = result.getOrNull()
                )
            } else {
                _state.value = _state.value.copy(
                    isAutoLoading = false,
                    error = result.exceptionOrNull()?.message
                )
            }
        }
    }

    fun applyAutoPolicy(userId: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isAutoApplying = true, error = null)

            val result = repository.autoApplyPolicy(userId)
            if (result.isSuccess) {
                val updatedPolicy = repository.getCachedPolicy()
                _state.value = _state.value.copy(
                    isAutoApplying = false,
                    autoPolicy = result.getOrNull(),
                    policy = updatedPolicy,
                    autoApplied = true
                )
                if (updatedPolicy != null) updateServicePrefs(updatedPolicy)
            } else {
                _state.value = _state.value.copy(
                    isAutoApplying = false,
                    error = result.exceptionOrNull()?.message
                )
            }
        }
    }

    fun clearPolicy(userId: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isPolicyClearing = true, error = null)

            val request = PolicySettingsRequestDto(
                dailyLimitMinutes = null,
                bedtimeStart = null,
                bedtimeEnd = null,
                weekendRelaxPct = 0,
                blockedPackages = emptyList()
            )

            val result = repository.updateSettings(userId, request)

            if (result.isSuccess) {
                val clearedPolicy = repository.getCachedPolicy()
                _state.value = _state.value.copy(
                    isPolicyClearing = false,
                    policy = clearedPolicy,
                    autoApplied = false
                )
                if (clearedPolicy != null) updateServicePrefs(clearedPolicy)
            } else {
                _state.value = _state.value.copy(
                    isPolicyClearing = false,
                    error = result.exceptionOrNull()?.message
                )
            }
        }
    }

    private fun updateServicePrefs(policy: PolicyResponseDto) {
        val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        prefs.edit {
            putInt("daily_limit", policy.dailyLimitMinutes ?: -1)
            putString("bedtime_start", policy.bedtime?.start)
            putString("bedtime_end", policy.bedtime?.end)
            putStringSet("blocked_packages", policy.blockedApps.toSet())
        }
    }
}