package com.example.digitalhealthkids.ui.home.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import com.example.digitalhealthkids.domain.usage.DashboardData
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun WeeklyRulesCompactCard(data: DashboardData) {
    Card {
        Column(
            Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Uyku & Kurallar", style = MaterialTheme.typography.titleMedium)

            if (data.bedtimeStart != null) {
                Text("Yatma: ${data.bedtimeStart} – ${data.bedtimeEnd}")
            } else {
                Text("Uyku saati tanımlı değil")
            }
        }
    }
}
