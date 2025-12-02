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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.digitalhealthkids.domain.usage.DashboardData
import com.example.digitalhealthkids.ui.home.components.*
import kotlinx.coroutines.launch

data class BottomNavItem(
    val label: String,
    val icon: ImageVector
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    childId: String,
    deviceId: String,
    onNavigateToDetail: (Int) -> Unit, // 🔥 Yeni eklenen parametre
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val pagerState = rememberPagerState(pageCount = { 3 })
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    LaunchedEffect(childId, deviceId) {
        viewModel.syncUsageHistory(context, childId, deviceId)
        viewModel.scheduleBackgroundSync(context)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Digital Health Kids",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
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
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            Modifier.padding(innerPadding).fillMaxSize()
        ) {
            when {
                state.isLoading -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
                state.error != null -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    Text(state.error!!, color = MaterialTheme.colorScheme.error)
                }
                state.data != null -> {
                    val dashboard = state.data!!

                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize()
                    ) { page ->
                        when (page) {
                            0 -> WeeklyDashboardContent(
                                dashboard = dashboard,
                                selectedDay = viewModel.selectedDay,
                                onDaySelected = viewModel::selectDay,
                                onViewDetailsClick = onNavigateToDetail // 🔥 Bağlantı burada
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

@Composable
private fun TopAppsPage(dashboard: DashboardData) {
    val allApps = dashboard.weeklyBreakdown.flatMap { it.apps }
    val mergedApps = allApps
        .groupBy { it.packageName }
        .map { (_, list) -> list.first().copy(minutes = list.sumOf { it.minutes }) }
        .sortedByDescending { it.minutes }
        .take(15)

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("En Çok Kullanılanlar", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
        WeeklyTopAppsCompactCard(mergedApps)
    }
}

@Composable
private fun RulesPage(dashboard: DashboardData) {
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Limitler ve Kurallar", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
        WeeklyRulesCompactCard(dashboard)
    }
}