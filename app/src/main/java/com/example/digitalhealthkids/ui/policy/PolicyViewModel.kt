package com.example.digitalhealthkids.ui.policy

import android.content.Context
import androidx.core.content.edit // 🔥 Bunu eklemeyi unutma
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
    @ApplicationContext private val context: Context // 🔥 1. Context'i buraya al
) : ViewModel() {

    // ... (State tanımı aynı)
    data class PolicyState(
        val isLoading: Boolean = false,
        val policy: PolicyResponseDto? = null,
        val error: String? = null
    )

    private val _state = MutableStateFlow(PolicyState())
    val state: StateFlow<PolicyState> = _state

    // ... (loadPolicy aynı kalabilir)
    fun loadPolicy(childId: String) {
        viewModelScope.launch {
            _state.value = PolicyState(isLoading = true)
            // Önce cache göster
            val cached = repository.getCachedPolicy()
            if (cached != null) {
                _state.value = PolicyState(policy = cached, isLoading = true)
            }

            val result = repository.refreshPolicy(childId)
            if (result.isSuccess) {
                // Taze veri gelince Servisi güncelle
                val freshPolicy = repository.getCachedPolicy()
                _state.value = PolicyState(isLoading = false, policy = freshPolicy)
                if (freshPolicy != null) updateServicePrefs(freshPolicy) // 🔥 Servisi güncelle
            } else {
                _state.value = PolicyState(
                    isLoading = false,
                    policy = cached,
                    error = result.exceptionOrNull()?.message
                )
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

            val request = PolicySettingsRequestDto(
                dailyLimitMinutes = limitMinutes,
                bedtimeStart = startTime,
                bedtimeEnd = endTime,
                weekendRelaxPct = 0
            )

            val result = repository.updateSettings(userId, request)

            if (result.isSuccess) {
                // 🔥 2. Başarılıysa hemen servise haber ver!
                val updatedPolicy = repository.getCachedPolicy() // Repository güncel veriyi cache'lemiştir
                _state.value = PolicyState(isLoading = false, policy = updatedPolicy)

                if (updatedPolicy != null) {
                    updateServicePrefs(updatedPolicy)
                }
            } else {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = result.exceptionOrNull()?.message
                )
            }
        }
    }

    // 🔥 3. YENİ FONKSİYON: Servisin okuduğu dosyayı güncelle
    private fun updateServicePrefs(policy: PolicyResponseDto) {
        val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        prefs.edit {
            // Günlük Limit (Yoksa -1)
            putInt("daily_limit", policy.dailyLimitMinutes ?: -1)

            // Uyku Saatleri (Yoksa null - yani sil)
            putString("bedtime_start", policy.bedtime?.start)
            putString("bedtime_end", policy.bedtime?.end)

            // Yasaklılar
            putStringSet("blocked_packages", policy.blockedApps.toSet())
        }
    }
}