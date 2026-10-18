package com.daftech.apkextractor

import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest

object HashUtil {
    /** SHA-256 file secara streaming (aman untuk APK besar). */
    fun sha256(f: File): String {
        val md = MessageDigest.getInstance("SHA-256")
        FileInputStream(f).use { inp ->
            val buf = ByteArray(8192)
            var n: Int
            while (inp.read(buf).also { n = it } > 0) {
                md.update(buf, 0, n)
            }
        }
        return md.digest().joinToString("") { "%02x".format(it) }
    }
}
