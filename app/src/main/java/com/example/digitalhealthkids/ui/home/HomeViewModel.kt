package com.example.digitalhealthkids.ui.home

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.digitalhealthkids.core.network.usage.UsageApi
import com.example.digitalhealthkids.core.network.usage.UsageEventDto
import com.example.digitalhealthkids.core.network.usage.UsageReportRequestDto
import com.example.digitalhealthkids.core.network.usage.readTodayUsageEvents
import com.example.digitalhealthkids.domain.usage.DashboardData
import com.example.digitalhealthkids.domain.usage.UsageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val usageRepository: UsageRepository,
    private val usageApi: UsageApi
) : ViewModel() {

    data class State(
        val isLoading: Boolean = true,
        val data: DashboardData? = null,
        val error: String? = null
    )

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state

    var selectedDay by mutableStateOf(0)
        private set

    fun selectDay(i: Int) {
        selectedDay = i
    }

    fun loadDashboard(childId: String) {
        viewModelScope.launch {
            try {
                _state.value = State(isLoading = true)
                val d = usageRepository.getDashboard(childId)
                _state.value = State(isLoading = false, data = d)
            } catch (e: Exception) {
                _state.value = State(isLoading = false, error = e.message)
            }
        }
    }

    // HomeViewModel.kt içine
    fun syncTodayUsage(
        context: Context,
        childId: String,
        deviceId: String
    ) {
        viewModelScope.launch {
            try {
                _state.value = State(isLoading = true)

                // 1) Android usage verisini oku
                val events: List<UsageEventDto> = readTodayUsageEvents(context)

                if (events.isNotEmpty()) {
                    // 2) Backend’e gönder
                    val body = UsageReportRequestDto(
                        childId = childId,
                        deviceId = deviceId,
                        events = events
                    )
                    usageApi.reportUsage(body)
                }

                // 3) Dashboard verisini çek
                val d = usageRepository.getDashboard(childId)
                _state.value = State(isLoading = false, data = d)

            } catch (e: Exception) {
                _state.value = State(isLoading = false, error = e.message)
            }
        }
    }


    fun sendUsage(childId: String, deviceId: String, events: List<UsageEventDto>) {
        viewModelScope.launch {
            try {
                val body = UsageReportRequestDto(
                    childId = childId,
                    deviceId = deviceId,
                    events = events
                )
                val response = usageApi.reportUsage(body)
                println("Usage gönderildi: $response")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }


}
