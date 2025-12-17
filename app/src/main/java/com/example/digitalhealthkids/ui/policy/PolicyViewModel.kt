package com.example.digitalhealthkids.ui.policy

import android.content.Context
import androidx.core.content.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
        val policy: PolicyResponseDto? = null,
        val error: String? = null
    )

    private val _state = MutableStateFlow(PolicyState())
    val state: StateFlow<PolicyState> = _state

    fun loadPolicy(childId: String) {
        viewModelScope.launch {
            _state.value = PolicyState(isLoading = true)
            val cached = repository.getCachedPolicy()
            if (cached != null) {
                _state.value = PolicyState(policy = cached, isLoading = true)
            }

            val result = repository.refreshPolicy(childId)
            if (result.isSuccess) {
                val freshPolicy = repository.getCachedPolicy()
                _state.value = PolicyState(isLoading = false, policy = freshPolicy)
                if (freshPolicy != null) updateServicePrefs(freshPolicy)
            } else {
                _state.value = PolicyState(
                    isLoading = false,
                    policy = cached,
                    error = result.exceptionOrNull()?.message
                )
            }
        }
    }

    // 🔥 EKSİK OLAN FONKSİYON BURASI 🔥
    // NavGraph ve AppDetailScreen tarafından çağrılıyor
    fun addPolicy(packageName: String, limitMinutes: Int) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)

            // Mevcut politikayı al
            val currentPolicy = _state.value.policy ?: return@launch

            // Mantık: Eğer süre 0 ise engelle, değilse (şimdilik) engeli kaldır.
            // İleride "AppSpecificLimit" endpointi yazılırsa oraya dakika göndeririz.
            val currentBlocked = currentPolicy.blockedApps.toMutableSet()

            if (limitMinutes <= 0) {
                currentBlocked.add(packageName)
            } else {
                currentBlocked.remove(packageName)
            }

            // Backend'e göndereceğimiz request (Sadece blocked listesini güncelliyoruz şimdilik)
            val request = PolicySettingsRequestDto(
                dailyLimitMinutes = currentPolicy.dailyLimitMinutes,
                bedtimeStart = currentPolicy.bedtime?.start,
                bedtimeEnd = currentPolicy.bedtime?.end,
                weekendRelaxPct = 0,
                blockedPackages = currentBlocked.toList() // Yeni liste
            )

            // API Çağrısı
            // Not: updateSettings fonksiyonunu repository'de blockedPackages alacak şekilde güncellemiş varsayıyoruz.
            // Eğer repository sadece settings alıyorsa, backend tarafı bu veriyi merge etmeli.
            val result = repository.updateSettings(currentPolicy.userId ?: "", request)

            if (result.isSuccess) {
                // Başarılıysa cache'i güncelle
                val updatedPolicy = repository.getCachedPolicy()
                _state.value = PolicyState(isLoading = false, policy = updatedPolicy)
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
        endTime: String?
    ) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)

            // Mevcut blocked listesini korumak için state'den alıyoruz
            val currentBlocked = _state.value.policy?.blockedApps ?: emptyList()

            val request = PolicySettingsRequestDto(
                dailyLimitMinutes = limitMinutes,
                bedtimeStart = startTime,
                bedtimeEnd = endTime,
                weekendRelaxPct = 0,
                blockedPackages = currentBlocked // Mevcut listeyi koru
            )

            val result = repository.updateSettings(userId, request)

            if (result.isSuccess) {
                val updatedPolicy = repository.getCachedPolicy()
                _state.value = PolicyState(isLoading = false, policy = updatedPolicy)
                if (updatedPolicy != null) updateServicePrefs(updatedPolicy)
            } else {
                _state.value = _state.value.copy(
                    isLoading = false,
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