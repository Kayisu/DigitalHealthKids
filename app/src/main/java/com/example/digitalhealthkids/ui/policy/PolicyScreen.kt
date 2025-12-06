package com.example.digitalhealthkids.ui.policy

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun PolicyScreen(
    userId: String,
    viewModel: PolicyViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    // Ekran açıldığında veriyi çek
    LaunchedEffect(userId) {
        viewModel.loadPolicy(userId)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (state.isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        } else if (state.error != null) {
            Text(
                text = "Hata: ${state.error}",
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.align(Alignment.Center)
            )
        } else if (state.policy != null) {
            val policy = state.policy!!

            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Text(
                        "Kısıtlamalar ve Kurallar",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                    )
                }

                item {
                    PolicyCard(
                        title = "Günlük Limit",
                        value = "${policy.dailyLimitMinutes} dakika",
                        icon = Icons.Default.Timer
                    )
                }

                item {
                    val bedtimeText = if (policy.bedtime != null)
                        "${policy.bedtime.start} - ${policy.bedtime.end}"
                    else "Ayarlanmadı"

                    PolicyCard(
                        title = "Uyku Saati",
                        value = bedtimeText,
                        icon = Icons.Default.Bedtime
                    )
                }

                item {
                    PolicyCard(
                        title = "Engelli Uygulamalar",
                        value = if (policy.blockedApps.isNotEmpty())
                            policy.blockedApps.joinToString(", ")
                        else "Yok",
                        icon = Icons.Default.Block
                    )
                }
            }
        }
    }
}

@Composable
fun PolicyCard(title: String, value: String, icon: ImageVector) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(text = title, style = MaterialTheme.typography.titleMedium)
                Text(text = value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}