package com.example.digitalhealthkids.service

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import com.example.digitalhealthkids.core.network.usage.getTodayTotalUsageMillis // 🔥 Bunu import et
import com.example.digitalhealthkids.core.network.usage.isUserApp
import com.example.digitalhealthkids.ui.block.BlockingActivity
import java.util.Calendar

class AppBlockingService : AccessibilityService() {

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // 🔥 ZIRHLAMA: Tüm mantığı try-catch içine alıyoruz
        try {
            if (event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
                val packageName = event.packageName?.toString() ?: return

                // Kendimizi ve Launcher'ı asla engelleme
                if (packageName == this.packageName) return
                if (!isUserApp(this, packageName)) return

                checkRulesAndBlock(packageName)
            }
        } catch (e: Exception) {
            // Hata olursa Log'a yaz ama servisin çökmesine (ve kapanmasına) izin verme!
            e.printStackTrace()
            android.util.Log.e("AppBlockingService", "Hata oluştu, servis ayakta tutuluyor: ${e.message}")
        }
    }

    private fun checkRulesAndBlock(packageName: String) {
        val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

        // --- KURAL 1: Yasaklı Uygulamalar ---
        val blockedPackages = prefs.getStringSet("blocked_packages", emptySet()) ?: emptySet()
        if (blockedPackages.contains(packageName)) {
            blockApp()
            return
        }

        // --- KURAL 2: Günlük Limit (Global) ---
        val dailyLimitMinutes = prefs.getInt("daily_limit", -1)
        if (dailyLimitMinutes != -1) { // Eğer limit ayarlanmışsa (-1 değilse)
            val limitMillis = dailyLimitMinutes * 60 * 1000L
            val currentUsageMillis = getTodayTotalUsageMillis(this)

            if (currentUsageMillis > limitMillis) {
                blockApp() // Kotan doldu, hiçbir şeye giremezsin!
                return
            }
        }

        // --- KURAL 3: Uyku Vakti ---
        val bedStart = prefs.getString("bedtime_start", null)
        val bedEnd = prefs.getString("bedtime_end", null)

        if (bedStart != null && bedEnd != null) {
            if (isItBedtime(bedStart, bedEnd)) {
                blockApp() // Uyku vakti, telefon kapalı!
                return
            }
        }
    }

    private fun isItBedtime(start: String, end: String): Boolean {
        try {
            val now = Calendar.getInstance()
            val currentMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)

            val (sH, sM) = start.split(":").map { it.toInt() }
            val startMinutes = sH * 60 + sM

            val (eH, eM) = end.split(":").map { it.toInt() }
            val endMinutes = eH * 60 + eM

            return if (startMinutes > endMinutes) {
                // Gece yarısını geçen aralık (Örn: 22:00 -> 07:00)
                currentMinutes >= startMinutes || currentMinutes < endMinutes
            } else {
                // Aynı gün içi (Örn: 14:00 -> 16:00)
                currentMinutes in startMinutes until endMinutes
            }
        } catch (e: Exception) {
            return false
        }
    }

    private fun blockApp() {
        val intent = Intent(this, BlockingActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
        startActivity(intent)
    }

    override fun onInterrupt() {}
}