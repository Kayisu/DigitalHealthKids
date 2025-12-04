package com.example.digitalhealthkids.ui.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.digitalhealthkids.ui.home.HomeViewModel

@Composable
fun AppsPage(
    userId: String,
    viewModel: HomeViewModel
) {
    val appList by viewModel.appList.collectAsState()

    // Veri yoksa hesaplat (Recomposition'da tetiklenir)
    LaunchedEffect(Unit) {
        viewModel.calculateAppStats()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Uygulama Yönetimi",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "Haftalık ortalamalar ve kısıtlamalar",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (appList.isEmpty()) {
            Box(Modifier.fillMaxSize(), Alignment.Center) {
                Text("Henüz yeterli veri yok.")
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(appList) { app ->
                    AppControlCard(
                        appName = app.appName,
                        packageName = app.packageName,
                        avgMinutes = app.averageMinutes,
                        isBlocked = app.isBlocked,
                        onToggleBlock = {
                            viewModel.toggleAppBlock(userId, app.packageName)
                        }
                    )
                }
                // Alt boşluk
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }
}

@Composable
fun AppControlCard(
    appName: String,
    packageName: String,
    avgMinutes: Int,
    isBlocked: Boolean,
    onToggleBlock: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isBlocked) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
            else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. İkon
            AppIcon(packageName = packageName, modifier = Modifier.size(48.dp))

            Spacer(modifier = Modifier.width(16.dp))

            // 2. İsim ve Süre
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = appName,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    maxLines = 1
                )
                Text(
                    text = "Ort. $avgMinutes dk/gün",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // 3. O Meşhur Saat/Kilit Simgesi
            // Butonun kendisi
            IconButton(
                onClick = onToggleBlock,
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        color = if (isBlocked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primaryContainer,
                        shape = CircleShape
                    )
            ) {
                Icon(
                    // Engelliyse Kilit, değilse Kum Saati (veya Saat)
                    imageVector = if (isBlocked) Icons.Default.Lock else Icons.Default.HourglassEmpty,
                    contentDescription = "Engelle",
                    tint = if (isBlocked) Color.White else MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}