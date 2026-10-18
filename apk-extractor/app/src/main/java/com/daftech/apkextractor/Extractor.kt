package com.daftech.apkextractor

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

/** Menyalin APK (base + split) dari sourceDir ke Download/ApkExtractor. */
object Extractor {

    fun fileNameFor(label: String, version: String): String {
        val safe = label.replace(Regex("[^A-Za-z0-9._-]+"), "_").take(40).ifEmpty { "app" }
        val v = version.replace(Regex("[^A-Za-z0-9._-]+"), "_").take(20).ifEmpty { "0" }
        return "${safe}_v${v}.apk"
    }

    /**
     * Simpan base APK + semua split ke Download/ApkExtractor.
     * @return daftar nama file yang berhasil disimpan.
     */
    fun saveToDownloads(ctx: Context, entry: AppEntry): List<String> {
        val saved = mutableListOf<String>()
        val baseName = fileNameFor(entry.label, entry.versionName)
        val jobs = mutableListOf<Pair<File, String>>()
        jobs += File(entry.sourceDir) to baseName
        entry.splitDirs.forEachIndexed { i, sp ->
            val f = File(sp)
            val splitTag = f.nameWithoutExtension.take(24).ifEmpty { "split$i" }
            jobs += f to baseName.replace(".apk", "_${splitTag}.apk")
        }
        for ((src, name) in jobs) {
            if (!src.exists()) continue
            try {
                if (Build.VERSION.SDK_INT >= 29) {
                    val values = ContentValues().apply {
                        put(MediaStore.Downloads.DISPLAY_NAME, name)
                        put(MediaStore.Downloads.MIME_TYPE, "application/vnd.android.package-archive")
                        put(MediaStore.Downloads.RELATIVE_PATH, "Download/ApkExtractor")
                    }
                    val uri = ctx.contentResolver.insert(
                        MediaStore.Downloads.EXTERNAL_CONTENT_URI, values
                    ) ?: continue
                    ctx.contentResolver.openOutputStream(uri)?.use { out ->
                        FileInputStream(src).use { inp -> inp.copyTo(out) }
                    }
                    saved += name
                } else {
                    @Suppress("DEPRECATION")
                    val dir = File(
                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                        "ApkExtractor"
                    )
                    dir.mkdirs()
                    FileInputStream(src).use { inp ->
                        FileOutputStream(File(dir, name)).use { out -> inp.copyTo(out) }
                    }
                    saved += name
                }
            } catch (_: Exception) {
                // Lanjut ke file berikutnya.
            }
        }
        return saved
    }

    /** Salin APK ke cache internal untuk dibagikan via FileProvider. */
    fun copyToCache(ctx: Context, srcPath: String, name: String): File? {
        return try {
            val dir = File(ctx.cacheDir, "shared").apply { mkdirs() }
            val dst = File(dir, name)
            FileInputStream(srcPath).use { inp ->
                FileOutputStream(dst).use { out -> inp.copyTo(out) }
            }
            dst
        } catch (_: Exception) {
            null
        }
    }
}
