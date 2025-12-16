package com.example.digitalhealthkids.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import com.example.digitalhealthkids.core.network.usage.getTodayTotalUsageMillis // Yeni yazdığımız fonksiyon
import com.example.digitalhealthkids.data.local.PolicyManager
import com.example.digitalhealthkids.ui.block.BlockingActivity
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import java.util.Calendar

@AndroidEntryPoint // 🔥 1. Hilt ile servisi işaretle
class AppBlockingService : AccessibilityService() {

    @Inject
    lateinit var policyManager: PolicyManager // 🔥 2. PolicyManager'ı enjekte et

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val packageName = event.packageName?.toString() ?: return

            // Kendi uygulamamızı engellemeyelim
            if (packageName == this.packageName) return

            checkRulesAndBlock(packageName)
        }
    }

    private fun checkRulesAndBlock(currentPackage: String) {
        // Cache'deki en son politikayı al
        val policy = policyManager.getPolicy() ?: return // Politika yoksa işlem yapma

        // 1. KURAL: Yasaklı Uygulamalar Listesi
        // (Buraya ileride "Kategori Kontrolü" de eklenecek)
        if (policy.blockedApps.contains(currentPackage)) {
            blockApp("Bu uygulama engellendi.")
            return
        }

        // 2. KURAL: Uyku Vakti Kontrolü
        if (isBedtime(policy.bedtime?.start, policy.bedtime?.end)) {
            blockApp("Uyku vakti! Telefon dinleniyor.")
            return
        }

        // 3. KURAL: Günlük Toplam Süre Limiti
        // Limiti dakikadan milisaniyeye çevir
        val limitMillis = policy.dailyLimitMinutes * 60 * 1000L
        if (limitMillis > 0) {
            val usedMillis = getTodayTotalUsageMillis(this)
            if (usedMillis > limitMillis) {
                blockApp("Günlük ekran süresi doldu.")
                return
            }
        }
    }

    private fun isBedtime(start: String?, end: String?): Boolean {
        if (start == null || end == null) return false

        // Basit saat kontrolü (Örn: start="22:00", end="07:00")
        // Gerçek dünyada burası biraz daha kompleks Date işlemi gerektirir ama MVP için:
        val now = Calendar.getInstance()
        val currentHour = now.get(Calendar.HOUR_OF_DAY)
        val currentMinute = now.get(Calendar.MINUTE)
        val currentTimeVal = currentHour * 60 + currentMinute

        val sParts = start.split(":")
        val eParts = end.split(":")
        val sVal = (sParts[0].toInt() * 60) + sParts[1].toInt()
        val eVal = (eParts[0].toInt() * 60) + eParts[1].toInt()

        return if (sVal > eVal) {
            // Gece yarısını geçen aralık (Örn: 22:00 - 07:00)
            currentTimeVal >= sVal || currentTimeVal < eVal
        } else {
            // Aynı gün içi aralık (Örn: 14:00 - 16:00)
            currentTimeVal in sVal until eVal
        }
    }

    private fun blockApp(reason: String) {
        val intent = Intent(this, BlockingActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            putExtra("BLOCK_REASON", reason) // BlockingActivity'de bunu gösterebiliriz
        }
        startActivity(intent)
    }

    override fun onInterrupt() {}
}