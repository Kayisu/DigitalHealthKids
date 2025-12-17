package com.example.digitalhealthkids.ui.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.digitalhealthkids.ui.home.components.*
import com.example.digitalhealthkids.ui.policy.PolicyScreen
import kotlinx.coroutines.launch

data class BottomNavItem(
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    userId: String,
    deviceId: String,
    onNavigateToDetail: (Int) -> Unit,
    onNavigateToAppDetail: (String, String) -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    // 1. Senin ViewModel'indeki 'state' değişkenini kullanıyoruz (uiState değil)
    val state by viewModel.state.collectAsState()

    // 2. Senin ViewModel'indeki 'appList' değişkenini ayrıca dinliyoruz
    val appList by viewModel.appList.collectAsState()

    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val pagerState = rememberPagerState(pageCount = { 3 })

    // 3. fetchDashboard yerine senin syncUsageHistory fonksiyonunu çağırıyoruz
    LaunchedEffect(userId, deviceId) {
        viewModel.syncUsageHistory(context, userId, deviceId)
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                val items = listOf(
                    BottomNavItem("Özet", Icons.Default.BarChart),
                    BottomNavItem("Uygulamalar", Icons.Default.Apps),
                    BottomNavItem("Kurallar", Icons.Default.Shield)
                )
                items.forEachIndexed { index, item ->
                    NavigationBarItem(
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label) },
                        selected = pagerState.currentPage == index,
                        onClick = {
                            scope.launch { pagerState.animateScrollToPage(index) }
                        }
                    )
                }
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            if (state.isLoading) {
                Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
            } else if (state.error != null) {
                Box(Modifier.fillMaxSize(), Alignment.Center) {
                    Text(text = state.error!!, color = MaterialTheme.colorScheme.error)
                }
            } else if (state.data != null) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    when (page) {
                        0 -> WeeklyDashboardContent(
                            dashboard = state.data!!,
                            selectedDay = viewModel.selectedDay, // Bu sende var
                            onDaySelected = { viewModel.selectDay(it) }, // Bu da var
                            dailyLimit = 120,
                            onViewDetailsClick = onNavigateToDetail
                        )
                        1 -> AppsPage(
                            // 4. Senin hazırladığın appList'i veriyoruz
                            appList = appList,
                            onAppClick = { app ->
                                // AppUiModel'inde category alanı yoksa varsayılan gönderelim
                                onNavigateToAppDetail(app.packageName, app.appName)
                            },
                            onToggleBlock = { pkg ->
                                viewModel.toggleAppBlock(userId, pkg)
                            }
                        )
                        2 -> PolicyScreen(userId = userId)
                    }
                }
            }
        }
    }
}