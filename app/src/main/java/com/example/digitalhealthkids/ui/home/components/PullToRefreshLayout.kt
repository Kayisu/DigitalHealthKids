package com.example.digitalhealthkids.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PullToRefreshLayout(
    modifier: Modifier = Modifier,
    isLoading: Boolean,
    errorMessage: String? = null,
    onRefresh: suspend () -> Unit, // Suspend fonksiyon bekliyoruz
    content: @Composable () -> Unit
) {
    val scope = rememberCoroutineScope()
    var isRefreshing by remember { mutableStateOf(false) }

    fun doRefresh() {
        scope.launch {
            isRefreshing = true
            onRefresh() // ViewModel'daki load fonksiyonunu çağırır
            delay(500) // UX gecikmesi
            isRefreshing = false
        }
    }

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = { doRefresh() },
        modifier = modifier
    ) {
        // Hata ve Yükleme mantığını da burada yönetebiliriz
        when {
            isLoading && !isRefreshing -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            // Hata varsa bile içeriği gösterelim ki refresh yapılabilsin
            // İstersen hata mesajını burada toast veya snackbar olarak da gösterebilirsin
            else -> {
                content()
            }
        }
    }
}