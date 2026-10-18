package com.daftech.apkextractor

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import java.io.File

/** Membaca daftar aplikasi terinstal via PackageManager (tanpa root). */
object AppLoader {

    fun load(ctx: Context): List<AppEntry> {
        val pm = ctx.packageManager
        val apps = if (Build.VERSION.SDK_INT >= 33) {
            pm.getInstalledApplications(PackageManager.ApplicationInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            pm.getInstalledApplications(0)
        }
        val out = ArrayList<AppEntry>(apps.size)
        for (ai in apps) {
            try {
                val pi = if (Build.VERSION.SDK_INT >= 33) {
                    pm.getPackageInfo(ai.packageName, PackageManager.PackageInfoFlags.of(0))
                } else {
                    @Suppress("DEPRECATION")
                    pm.getPackageInfo(ai.packageName, 0)
                }
                val vCode = if (Build.VERSION.SDK_INT >= 28) {
                    pi.longVersionCode
                } else {
                    @Suppress("DEPRECATION")
                    pi.versionCode.toLong()
                }
                var bytes = 0L
                try {
                    bytes += File(ai.sourceDir).length()
                    ai.splitSourceDirs?.forEach { bytes += File(it).length() }
                } catch (_: Exception) {
                }
                out.add(
                    AppEntry(
                        label = pm.getApplicationLabel(ai)?.toString() ?: ai.packageName,
                        packageName = ai.packageName,
                        versionName = pi.versionName ?: "-",
                        versionCode = vCode,
                        isSystem = (ai.flags and ApplicationInfo.FLAG_SYSTEM) != 0,
                        sourceDir = ai.sourceDir,
                        splitDirs = ai.splitSourceDirs?.toList() ?: emptyList(),
                        targetSdk = ai.targetSdkVersion,
                        minSdk = if (Build.VERSION.SDK_INT >= 24) ai.minSdkVersion else 0,
                        firstInstallTime = pi.firstInstallTime,
                        lastUpdateTime = pi.lastUpdateTime,
                        totalBytes = bytes
                    )
                )
            } catch (_: Exception) {
                // Lewati paket yang tidak bisa dibaca, jangan crash.
            }
        }
        out.sortBy { it.label.lowercase() }
        return out
    }

    fun find(ctx: Context, packageName: String): AppEntry? =
        load(ctx).firstOrNull { it.packageName == packageName }
}
