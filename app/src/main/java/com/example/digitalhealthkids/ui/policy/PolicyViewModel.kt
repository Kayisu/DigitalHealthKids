package com.example.digitalhealthkids.ui.policy

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.digitalhealthkids.core.network.policy.PolicyResponseDto
import com.example.digitalhealthkids.core.network.policy.PolicySettingsRequestDto
import com.example.digitalhealthkids.domain.policy.PolicyRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PolicyViewModel @Inject constructor(
    private val repository: PolicyRepository
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
                _state.value = PolicyState(isLoading = false, policy = repository.getCachedPolicy())
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
        limitMinutes: Int,
        startTime: String,
        endTime: String
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
                _state.value = PolicyState(isLoading = false, policy = repository.getCachedPolicy())
            } else {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = result.exceptionOrNull()?.message
                )
            }
        }
    }
}