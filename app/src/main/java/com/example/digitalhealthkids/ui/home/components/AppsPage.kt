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
    onAppClick: (HomeViewModel.AppUiModel) -> Unit,
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
                "Uygulama Listesi",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        items(appList) { app ->
            val cleanName = AppUtils.getAppName(context, app.packageName, app.appName)

            // DÜZELTME BURADA:
            // 1. Surface wrapper'ı kaldırdık (AppUsageRowItem zaten kendi background ve shape'ini yönetiyor).
            // 2. onClick parametresini ekledik.
            AppUsageRowItem(
                name = cleanName,
                packageName = app.packageName,
                minutes = app.averageMinutes,
                isBlocked = app.isBlocked,
                onBlockToggle = { onToggleBlock(app.packageName) },
                onClick = { onAppClick(app) } // <-- EKLENDİ
            )
        }
    }
}