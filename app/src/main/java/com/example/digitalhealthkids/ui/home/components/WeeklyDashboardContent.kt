package com.example.digitalhealthkids.ui.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.digitalhealthkids.domain.usage.DashboardData

@Composable
fun WeeklyDashboardContent(
    dashboard: DashboardData,
    selectedDay: Int,
    onDaySelected: (Int) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            WeeklyHeroCard(dashboard)
        }

        item {
            WeeklyBarChart(
                values = dashboard.weeklyTrend,
                selectedDay = selectedDay,
                onDaySelected = onDaySelected
            )
        }

        item {
            WeeklyDayDetailCard(
                dashboard = dashboard,
                dayIndex = selectedDay
            )
        }

        item {
            WeeklyTopAppsCompactCard(dashboard.topApps)
        }

        item {
            WeeklyRulesCompactCard(dashboard)
        }
    }
}
