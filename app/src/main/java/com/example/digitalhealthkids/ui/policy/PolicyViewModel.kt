package com.example.digitalhealthkids.ui.policy

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.digitalhealthkids.core.network.policy.PolicyApi
import com.example.digitalhealthkids.core.network.policy.PolicyResponseDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PolicyViewModel @Inject constructor(
    private val policyApi: PolicyApi
) : ViewModel() {

    // UI State
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
            try {
                // Backend'den policy çek
                val response = policyApi.getCurrentPolicy(childId)
                _state.value = PolicyState(isLoading = false, policy = response)
            } catch (e: Exception) {
                _state.value = PolicyState(isLoading = false, error = e.message)
            }
        }
    }
}