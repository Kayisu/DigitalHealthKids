package com.example.digitalhealthkids.ui.home.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.digitalhealthkids.domain.usage.AppUsageItem

@Composable
fun WeeklyTopAppsCompactCard(apps: List<AppUsageItem>) {
    Card {
        Column(
            Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Bu Haftanın En Çok Kullanılanları",
                style = MaterialTheme.typography.titleMedium)

            apps.take(3).forEach {
                Text("${it.appName} – ${it.minutes} dk")
            }

            Text(
                "Tümünü gör →",
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
