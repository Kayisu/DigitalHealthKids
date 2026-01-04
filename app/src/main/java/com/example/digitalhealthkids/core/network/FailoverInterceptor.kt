package com.example.digitalhealthkids.core.network

import okhttp3.Interceptor
import okhttp3.Response
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

class FailoverInterceptor(private val baseUrlStore: BaseUrlStore) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        var request = chain.request()

        val primaryUrl = baseUrlStore.primary().toHttpUrlOrNull()
        val secondaryUrl = baseUrlStore.secondary().toHttpUrlOrNull()

        if (primaryUrl != null) {
            val newUrl = request.url.newBuilder()
                .scheme(primaryUrl.scheme)
                .host(primaryUrl.host)
                .port(primaryUrl.port)
                .build()
            request = request.newBuilder().url(newUrl).build()
        }

        try {
            return chain.proceed(request)
        } catch (e: Exception) {
            if (secondaryUrl != null) {
                val failoverUrl = request.url.newBuilder()
                    .scheme(secondaryUrl.scheme)
                    .host(secondaryUrl.host)
                    .port(secondaryUrl.port)
                    .build()

                val failoverRequest = request.newBuilder()
                    .url(failoverUrl)
                    .build()

                return chain.proceed(failoverRequest)
            }
            throw e
        }
    }
}