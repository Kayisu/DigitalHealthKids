package com.example.digitalhealthkids

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.compose.rememberNavController
import com.example.digitalhealthkids.ui.navigation.AppNavGraph
import com.example.digitalhealthkids.ui.theme.DigitalHealthKidsTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DigitalHealthKidsTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Ana içerik akışı
                    MainAppFlow()
                }
            }
        }
    }
}

@Composable
fun MainAppFlow() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // İzin durumunu tutan state
    var hasPermission by remember { mutableStateOf(context.hasUsageStatsPermission()) }

    // Yaşam döngüsünü dinle (Kullanıcı ayarlardan geri döndüğünde kontrol etmek için)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                // Uygulama her öne geldiğinde izni tekrar kontrol et
                hasPermission = context.hasUsageStatsPermission()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    if (hasPermission) {
        // İzin varsa normal akış başlar
        val navController = rememberNavController()
        AppNavGraph(navController = navController)
    } else {
        // İzin yoksa uyarı ekranı gösterilir
        PermissionRequestScreen()
    }
}

@Composable
fun PermissionRequestScreen() {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "İzin Gerekiyor",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Uygulamanın çocuğunuzun ekran süresini analiz edebilmesi için 'Kullanım Erişimi' iznine ihtiyacı vardır.\n\nLütfen açılan ekranda 'DigitalHealthKids' uygulamasını bulup izni açın.",
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                // Kullanıcıyı doğrudan ilgili ayar sayfasına yönlendir
                try {
                    val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                    context.startActivity(intent)
                } catch (e: Exception) {
                    // Bazı cihazlarda direkt açılmazsa genel ayarlar
                    e.printStackTrace()
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Ayarları Aç ve İzin Ver")
        }
    }
}

// Extension fonksiyon: İzin kontrolü
fun Context.hasUsageStatsPermission(): Boolean {
    val appOps = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
    val mode = appOps.checkOpNoThrow(
        AppOpsManager.OPSTR_GET_USAGE_STATS,
        android.os.Process.myUid(),
        packageName
    )
    return mode == AppOpsManager.MODE_ALLOWED
}