package com.example.digitalhealthkids.ui.auth

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.digitalhealthkids.domain.auth.LoginUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import androidx.core.content.edit
import retrofit2.HttpException

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val loginUseCase: LoginUseCase,
    @ApplicationContext private val context: Context // Prefs için context aldık
) : ViewModel() {

    private val _state = MutableStateFlow(LoginUiState())
    val state: StateFlow<LoginUiState> = _state

    fun onEmailChange(value: String) {
        _state.value = _state.value.copy(email = value)
    }

    fun onPasswordChange(value: String) {
        _state.value = _state.value.copy(password = value)
    }

    fun login(
        onSuccess: (String, String) -> Unit
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
                val safeDeviceId = resp.deviceId ?: "unknown_device"

                // 🔥 Login başarılı olunca Prefs'e kaydet (Worker için gerekli)
                val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                prefs.edit {
                    putString("user_id", resp.userId)
                    putString("device_id", safeDeviceId)
                    putString("token", resp.token)
                }

                onSuccess(resp.userId, safeDeviceId)
            }.onFailure { e ->
                val message = when (e) {
                    is HttpException -> when (e.code()) {
                        403 -> "E-postanı doğruladıktan sonra giriş yapabilirsin."
                        401 -> "E-posta veya şifre hatalı."
                        else -> "Giriş başarısız (HTTP ${e.code()})"
                    }
                    else -> e.message ?: "Giriş başarısız"
                }
                _state.value = current.copy(
                    error = message
                )
            }
        }
    }
}