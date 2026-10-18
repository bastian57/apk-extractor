package com.daftech.apkextractor

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun formatBytes(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024) return String.format(Locale.US, "%.1f KB", kb)
    val mb = kb / 1024.0
    if (mb < 1024) return String.format(Locale.US, "%.1f MB", mb)
    return String.format(Locale.US, "%.2f GB", mb / 1024.0)
}

fun formatDate(millis: Long): String {
    if (millis <= 0) return "-"
    return try {
        SimpleDateFormat("dd MMM yyyy HH:mm", Locale("id")).format(Date(millis))
    } catch (_: Exception) {
        "-"
    }
}
