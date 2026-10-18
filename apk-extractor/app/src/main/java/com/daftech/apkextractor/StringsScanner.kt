package com.daftech.apkextractor

import java.io.File
import java.util.Collections
import java.util.zip.ZipFile

/**
 * Mini "rev strings" on-device: pindai classes.dex di dalam APK,
 * tarik run printable ASCII (setara GNU strings, min length 4).
 */
object StringsScanner {

    data class Result(val total: Long, val samples: List<String>)

    fun scan(apk: File, minLen: Int = 4, maxSamples: Int = 500): Result {
        val samples = mutableListOf<String>()
        var total = 0L
        try {
            ZipFile(apk).use { zip ->
                val dexEntries = Collections.list(zip.entries())
                    .filter { it.name.endsWith(".dex") && !it.isDirectory }
                for (e in dexEntries) {
                    zip.getInputStream(e).use { inp ->
                        total += scanBytes(inp.readBytes(), minLen, samples, maxSamples)
                    }
                }
            }
        } catch (_: Exception) {
        }
        return Result(total, samples)
    }

    private fun scanBytes(
        data: ByteArray,
        minLen: Int,
        samples: MutableList<String>,
        maxSamples: Int
    ): Long {
        var total = 0L
        val cur = StringBuilder()
        fun flush() {
            if (cur.length >= minLen) {
                total++
                if (samples.size < maxSamples) samples.add(cur.toString())
            }
            cur.setLength(0)
        }
        for (b in data) {
            val v = b.toInt() and 0xFF
            if (v in 0x20..0x7E) {
                cur.append(v.toChar())
            } else {
                flush()
            }
        }
        flush()
        return total
    }
}
