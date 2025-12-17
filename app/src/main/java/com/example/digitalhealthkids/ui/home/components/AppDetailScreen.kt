package com.example.digitalhealthkids.ui.home.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.digitalhealthkids.ui.policy.AddPolicyDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDetailScreen(
    packageName: String,
    appName: String, // Navigasyondan parametre olarak gelecek
    category: String, // Navigasyondan parametre olarak gelecek
    onBackClick: () -> Unit,
    onAddPolicy: (String, Int) -> Unit // PolicyViewModel'deki addPolicy fonksiyonu
) {
    var showPolicyDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(appName) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Text("<") // İkon kullanabilirsin: Icons.Default.ArrowBack
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. Başlık ve Kategori
            Text(text = appName, style = MaterialTheme.typography.headlineMedium)
            Text(text = "Kategori: $category", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.secondary)

            Spacer(modifier = Modifier.height(32.dp))

            // 2. İstatistik Kartı (Mock Data - İleride API'den gelecek)
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Bugünkü Kullanım", style = MaterialTheme.typography.titleMedium)
                    Text("45 Dakika", style = MaterialTheme.typography.displaySmall) // Burası dinamik olmalı
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 3. Kısıtlama Butonu
            Button(
                onClick = { showPolicyDialog = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Süre Sınırı Koy")
            }
        }

        // 4. Policy Dialog Entegrasyonu
        if (showPolicyDialog) {
            AddPolicyDialog(
                onDismiss = { showPolicyDialog = false },
                onConfirm = { limit ->
                    onAddPolicy(packageName, limit)
                    showPolicyDialog = false
                }
            )
        }
    }
}