package com.example.digitalhealthkids

import android.accessibilityservice.AccessibilityService
import android.app.AppOpsManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
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
import com.example.digitalhealthkids.service.AppBlockingService // 🔥 Servis Import'u
import com.example.digitalhealthkids.ui.navigation.AppNavGraph
import com.example.digitalhealthkids.ui.theme.DigitalHealthKidsTheme
import dagger.hilt.android.AndroidEntryPoint
import android.content.pm.ApplicationInfo
import android.util.Log
import com.example.digitalhealthkids.core.util.AppUtils
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        printInstalledPackagesForPython(this)
        // Geçici: Tüm kullanıcı uygulamalarını logcat'e bas (APP_LIST etiketi)
        AppUtils.logInstalledUserApps(this)
        setContent {
            DigitalHealthKidsTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
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

    // İzin durumlarını tutan state'ler
    var hasUsagePermission by remember { mutableStateOf(context.hasUsageStatsPermission()) }
    var hasAccessibilityPermission by remember { mutableStateOf(context.isAccessibilityServiceEnabled()) }

    // Yaşam döngüsünü dinle (Kullanıcı ayarlardan dönünce kontrol et)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasUsagePermission = context.hasUsageStatsPermission()
                hasAccessibilityPermission = context.isAccessibilityServiceEnabled()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // 🔥 İzin Kontrol Akışı
    when {
        !hasUsagePermission -> {
            // 1. Önce Kullanım İzni
            PermissionScreen(
                title = "Kullanım İzni Gerekiyor",
                description = "Uygulamanın çocuğunuzun ekran süresini analiz edebilmesi için 'Kullanım Erişimi' iznine ihtiyacı vardır.",
                buttonText = "Ayarları Aç ve İzin Ver",
                onButtonClick = {
                    try {
                        context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                    } catch (e: Exception) { e.printStackTrace() }
                }
            )
        }
        !hasAccessibilityPermission -> {
            // 2. Sonra Erişilebilirlik İzni (Bloklama için şart)
            PermissionScreen(
                title = "Uygulama Engelleme İzni",
                description = "Seçilen uygulamaları engelleyebilmek için 'DigitalHealthKids' erişilebilirlik servisini açmanız gerekmektedir.\n\nAyarlarda 'Yüklü Uygulamalar' veya 'Erişilebilirlik' altında uygulamamızı bulup açınız.",
                buttonText = "Erişilebilirlik Ayarlarını Aç",
                onButtonClick = {
                    try {
                        context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                    } catch (e: Exception) { e.printStackTrace() }
                }
            )
        }
        else -> {
            // 3. Her şey tamsa Uygulamaya Gir
            val navController = rememberNavController()
            AppNavGraph(navController = navController)
        }
    }
}

fun printInstalledPackagesForPython(context: android.content.Context) {
    val pm = context.packageManager
    // Sadece kullanıcı uygulamalarını (System app olmayanları) alalım ki liste temiz olsun
    val packages = pm.getInstalledPackages(0)
        .filter { (it.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM) == 0 }
        .map { "\"${it.packageName}\"" } // Python formatına uygun tırnak içine alıyoruz

    // Logcat'e Python listesi formatında basıyoruz
    val pythonListString = packages.joinToString(separator = ", ", prefix = "[", postfix = "]")

    Log.e("MOCK_DATA_GEN", "👇 AŞAĞIDAKİ SATIRI KOPYALA VE PYTHON SCRIPTINE YAPIŞTIR 👇")
    Log.e("MOCK_DATA_GEN", pythonListString)
    Log.e("MOCK_DATA_GEN", "👆 YUKARIDAKİ SATIRI KOPYALA 👆")
}
// Ortak İzin Ekranı Tasarımı
@Composable
fun PermissionScreen(
    title: String,
    description: String,
    buttonText: String,
    onButtonClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = description,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onButtonClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(buttonText)
        }
    }
}

// Extension: Kullanım İzni Kontrolü
fun Context.hasUsageStatsPermission(): Boolean {
    val appOps = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
    val mode = appOps.checkOpNoThrow(
        AppOpsManager.OPSTR_GET_USAGE_STATS,
        android.os.Process.myUid(),
        packageName
    )
    return mode == AppOpsManager.MODE_ALLOWED
}

// 🔥 Extension: Erişilebilirlik İzni Kontrolü
fun Context.isAccessibilityServiceEnabled(): Boolean {
    val expectedComponentName = ComponentName(this, AppBlockingService::class.java)

    val enabledServicesSetting = Settings.Secure.getString(
        contentResolver,
        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
    ) ?: return false

    val colonSplitter = TextUtils.SimpleStringSplitter(':')
    colonSplitter.setString(enabledServicesSetting)

    while (colonSplitter.hasNext()) {
        val componentNameString = colonSplitter.next()
        val enabledComponent = ComponentName.unflattenFromString(componentNameString)
        if (enabledComponent != null && enabledComponent == expectedComponentName) return true
    }
    return false
}


