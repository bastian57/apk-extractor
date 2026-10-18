package com.daftech.apkextractor

/** Satu aplikasi terinstal beserta info forensik dasarnya. */
data class AppEntry(
    val label: String,
    val packageName: String,
    val versionName: String,
    val versionCode: Long,
    val isSystem: Boolean,
    val sourceDir: String,
    val splitDirs: List<String>,
    val targetSdk: Int,
    val minSdk: Int,
    val firstInstallTime: Long,
    val lastUpdateTime: Long,
    val totalBytes: Long
)
