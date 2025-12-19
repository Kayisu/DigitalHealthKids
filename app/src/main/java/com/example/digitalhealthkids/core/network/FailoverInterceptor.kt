package com.example.digitalhealthkids.core.network

import okhttp3.Interceptor
import okhttp3.Response
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.io.IOException

class FailoverInterceptor : Interceptor {

    private val LOCAL_URL = "http://192.168.1.5:8000/api/"
    private val TAILSCALE_URL = "http://100.103.29.117:8000/api/"



    override fun intercept(chain: Interceptor.Chain): Response {
        var request = chain.request()

        // 1. Önce LOCAL adresi dene
        val localHttpUrl = LOCAL_URL.toHttpUrlOrNull()

        if (localHttpUrl != null) {
            val newUrl = request.url.newBuilder()
                .scheme(localHttpUrl.scheme)
                .host(localHttpUrl.host)
                .port(localHttpUrl.port)
                .build()

            request = request.newBuilder().url(newUrl).build()
        }

        try {
            // İsteği gönder!
            return chain.proceed(request)
        } catch (e: Exception) {
            // 💥 HATA! Demek ki evde değiliz veya local kapalı.
            // Hiç panik yapma, hemen Tailscale'e dön.

            val remoteHttpUrl = TAILSCALE_URL.toHttpUrlOrNull()

            if (remoteHttpUrl != null) {
                val failoverUrl = request.url.newBuilder()
                    .scheme(remoteHttpUrl.scheme)
                    .host(remoteHttpUrl.host)
                    .port(remoteHttpUrl.port)
                    .build()

                val failoverRequest = request.newBuilder()
                    .url(failoverUrl)
                    .build()

                // Şimdi Tailscale ile tekrar dene
                return chain.proceed(failoverRequest)
            }

            // Tailscale de bozuksa yapacak bir şey yok, hatayı fırlat.
            throw e
        }
    }
}