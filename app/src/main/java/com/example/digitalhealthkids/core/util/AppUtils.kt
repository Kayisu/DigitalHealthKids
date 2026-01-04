package com.example.digitalhealthkids.core.util

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.util.Log
import java.util.Locale

object AppUtils {

    fun getAppIcon(context: Context, packageName: String): Drawable? {
        return try {
            context.packageManager.getApplicationIcon(packageName)
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }
    }

    // Paket ismini (com.whatsapp) -> "WhatsApp" a çevirir
    fun getAppName(context: Context, packageName: String, backendName: String?): String {
        // 1. Sistemden sormayı dene
        try {
            val pm = context.packageManager
            val info = pm.getApplicationInfo(packageName, 0)
            return pm.getApplicationLabel(info).toString()
        } catch (e: Exception) {
            // Uygulama telefonda yüklü değilse veya bulunamazsa
        }

        // 2. Backend zaten düzgün bir isim yolladıysa onu kullan
        if (!backendName.isNullOrEmpty() && !backendName.contains(".")) {
            return backendName
        }

        // 3. Hiçbiri olmazsa paketin son parçasını alıp baş harfini büyüt
        // com.instagram.android -> Instagram
        return packageName.substringAfterLast('.')
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
    }

    /**
     * Telefonda yüklü kullanıcı uygulamalarını (sistem hariç) logcat'e basar.
     * Çıktı: package_name, app_label
     */
    fun logInstalledUserApps(context: Context, tag: String = "APP_LIST") {
        val pm = context.packageManager
        val packages = pm.getInstalledApplications(0)
            .filter { (it.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) == 0 }
            .sortedBy { it.packageName.lowercase(Locale.getDefault()) }

        val lines = packages.map { appInfo ->
            val label = pm.getApplicationLabel(appInfo).toString()
            "${appInfo.packageName}\t$label"
        }

        Log.i(tag, "Installed user apps (${lines.size}):")
        // Çok satır tek log yerine okunabilir olsun diye parça parça basalım
        lines.chunked(50).forEachIndexed { idx, chunk ->
            Log.i(tag, "chunk ${idx + 1}/${(lines.size + 49) / 50}\n" + chunk.joinToString("\n"))
        }
    }
}