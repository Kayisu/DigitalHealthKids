package com.example.digitalhealthkids.ui.home.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.digitalhealthkids.core.util.AppUtils
import com.example.digitalhealthkids.ui.home.HomeViewModel

@Composable
fun AppsPage(
    appList: List<HomeViewModel.AppUiModel>,
    onToggleBlock: (String) -> Unit
) {
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                "Uygulama Yönetimi",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
            Text(
                "Kısıtlamak istediğiniz uygulamaları seçin",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        items(appList) { app ->
            val cleanName = AppUtils.getAppName(context, app.packageName, app.appName)

            AppUsageRowItem(
                name = cleanName,
                packageName = app.packageName,
                minutes = app.averageMinutes,
                isBlocked = app.isBlocked,
                onBlockToggle = {
                    onToggleBlock(app.packageName)
                }
            )
        }

        if (appList.isEmpty()) {
            item {
                Text("Listelenecek uygulama yok.")
            }
        }
    }
}