package com.example.digitalhealthkids.data.local

import android.content.Context
import android.content.SharedPreferences
import com.example.digitalhealthkids.core.network.policy.PolicyResponseDto
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PolicyManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences("policy_cache", Context.MODE_PRIVATE)
    private val gson = Gson()

    // 1. Yeni gelen politikayı kaydet (Cache'le)
    fun updatePolicy(policy: PolicyResponseDto) {
        val json = gson.toJson(policy)
        prefs.edit().putString("cached_policy", json).apply()
    }

    // 2. Kayıtlı politikayı oku (Offline olsa bile çalışır)
    fun getPolicy(): PolicyResponseDto? {
        val json = prefs.getString("cached_policy", null) ?: return null
        return try {
            gson.fromJson(json, PolicyResponseDto::class.java)
        } catch (e: Exception) {
            null
        }
    }

    // 3. Kullanıcı çıkış yaparsa temizle
    fun clear() {
        prefs.edit().remove("cached_policy").apply()
    }
}