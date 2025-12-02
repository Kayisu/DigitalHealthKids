package com.example.digitalhealthkids.core.util

import android.content.Context
import android.content.pm.PackageManager
import java.util.Locale

object AppUtils {

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
}