package com.example.digitalhealthkids.core.util

fun formatDuration(totalMinutes: Int): String {
    if (totalMinutes < 60) {
        return "$totalMinutes dk"
    }
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60

    return if (minutes == 0) {
        "$hours sa"
    } else {
        "$hours sa $minutes dk"
    }
}