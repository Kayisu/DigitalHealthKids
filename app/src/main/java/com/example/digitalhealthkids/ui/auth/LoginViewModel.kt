package com.example.digitalhealthkids.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.digitalhealthkids.domain.auth.LoginUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val loginUseCase: LoginUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(LoginUiState())
    val state: StateFlow<LoginUiState> = _state

    fun onEmailChange(value: String) {
        _state.value = _state.value.copy(email = value)
    }

    fun onPasswordChange(value: String) {
        _state.value = _state.value.copy(password = value)
    }

    /**
     * onSuccess artık childId döndürüyor.
     */
    fun login(
        onSuccess: (String, String) -> Unit // (childId, deviceId)
    ) {
        val current = _state.value
        if (current.email.isBlank() || current.password.isBlank()) {
            _state.value = current.copy(error = "E-posta ve şifre zorunlu")
            return
        }

        viewModelScope.launch {
            _state.value = current.copy(isLoading = true, error = null)
            val result = loginUseCase(current.email, current.password)
            _state.value = current.copy(isLoading = false)

            result.onSuccess { resp ->
                // 🔥 Artık resp.deviceId asla null olmayacak (en kötü "")
                onSuccess(resp.childId, resp.deviceId)
            }.onFailure { e ->
                _state.value = current.copy(
                    error = e.message ?: "Giriş başarısız"
                )
            }
        }
    }
}

