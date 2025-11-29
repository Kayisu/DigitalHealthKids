package com.example.digitalhealthkids.ui.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.digitalhealthkids.ui.home.components.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    childId: String,
    deviceId: String,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val pagerState = rememberPagerState(pageCount = { 3 })
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    LaunchedEffect(childId, deviceId) {
        // Tüm süreci (Oku -> Gönder -> Dashboard'u Getir) bu fonksiyon yönetir.
        viewModel.syncUsageHistory(context, childId, deviceId)
        viewModel.scheduleBackgroundSync(context)
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Çocuk Cihaz – Dashboard") }
            )
        }
    ) { innerPadding ->
        Box(
            Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            when {
                state.isLoading -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    CircularProgressIndicator()
                }

                state.error != null -> Text(
                    modifier = Modifier.padding(16.dp),
                    text = state.error!!,
                    color = MaterialTheme.colorScheme.error
                )

                state.data != null -> {
                    val dashboard = state.data!!

                    Column(Modifier.fillMaxSize()) {
                        val tabs = listOf("Kullanım", "Uygulamalar", "Odak & Kurallar")

                        TabRow(selectedTabIndex = pagerState.currentPage) {
                            tabs.forEachIndexed { index, title ->
                                Tab(
                                    selected = pagerState.currentPage == index,
                                    onClick = {
                                        scope.launch {
                                            pagerState.animateScrollToPage(index)
                                        }
                                    },
                                    text = { Text(title) }
                                )
                            }
                        }

                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier.fillMaxSize()
                        ) { page ->
                            when (page) {
                                0 -> WeeklyUsagePage(
                                    dashboard = dashboard,
                                    selectedDay = viewModel.selectedDay,
                                    onDaySelected = viewModel::selectDay
                                )

                                1 -> TopAppsPage(dashboard)

                                2 -> RulesPage(dashboard)
                            }
                        }
                    }
                }
            }
        }
    }
}


@Composable
private fun WeeklyUsagePage(
    dashboard: com.example.digitalhealthkids.domain.usage.DashboardData,
    selectedDay: Int,
    onDaySelected: (Int) -> Unit
) {
    // Hafif dikey scroll – ama tek sayfada kalıyor
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        WeeklyHeroCard(dashboard)

        WeeklyBarChart(
            values = dashboard.weeklyTrend,
            selectedDay = selectedDay,
            onDaySelected = onDaySelected
        )

        WeeklyDayDetailCard(
            dashboard = dashboard,
            dayIndex = selectedDay
        )
    }
}

@Composable
private fun TopAppsPage(
    dashboard: com.example.digitalhealthkids.domain.usage.DashboardData
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Bu Haftanın Uygulamaları",
            style = MaterialTheme.typography.titleLarge
        )

        WeeklyTopAppsCompactCard(dashboard.topApps)
    }
}

@Composable
private fun RulesPage(
    dashboard: com.example.digitalhealthkids.domain.usage.DashboardData
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Odak & Kurallar",
            style = MaterialTheme.typography.titleLarge
        )

        WeeklyRulesCompactCard(dashboard)

        // İleride burada AI analiz kartları, “bu hafta öneriler” vs. ekleriz
    }
}
