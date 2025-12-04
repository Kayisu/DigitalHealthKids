package com.example.digitalhealthkids.service

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import com.example.digitalhealthkids.ui.block.BlockingActivity

class AppBlockingService : AccessibilityService() {

    private var blockedPackages: Set<String> = emptySet()

    override fun onServiceConnected() {
        super.onServiceConnected()
        updateBlockedList()
    }

    private fun updateBlockedList() {
        // Entegrasyonun kilit noktası: SharedPrefs'ten oku
        val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        blockedPackages = prefs.getStringSet("blocked_packages", emptySet()) ?: emptySet()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val packageName = event.packageName?.toString() ?: return

            // Kendimizi engellemeyelim
            if (packageName == this.packageName) return

            // Her pencere değişiminde listeyi tazeleyelim (MVP için kabul edilebilir)
            updateBlockedList()

            if (blockedPackages.contains(packageName)) {
                // 🔥 ENGELLEME!
                val intent = Intent(this, BlockingActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                }
                startActivity(intent)
            }
        }
    }

    override fun onInterrupt() {}
}