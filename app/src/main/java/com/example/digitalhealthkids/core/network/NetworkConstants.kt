package com.example.digitalhealthkids.core.network

object NetworkConstants {

    const val LOCAL_URL = "http://192.168.1.5:8000/api/"

    const val TAILSCALE_URL = "http://100.103.29.117:8000/api/"


    @Volatile
    var CURRENT_BASE_URL = LOCAL_URL
}