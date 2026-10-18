package com.daftech.apkextractor

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.appbar.MaterialToolbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class DetailActivity : AppCompatActivity() {

    private var entry: AppEntry? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        toolbar.setNavigationOnClickListener { finish() }

        val pkg = intent.getStringExtra("pkg") ?: run { finish(); return }
        lifecycleScope.launch(Dispatchers.IO) {
            val e = AppLoader.find(this@DetailActivity, pkg)
            withContext(Dispatchers.Main) {
                if (e == null) finish() else bind(e)
            }
        }
    }

    private fun bind(e: AppEntry) {
        entry = e
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        toolbar.title = e.label

        try {
            findViewById<ImageView>(R.id.d_icon)
                .setImageDrawable(packageManager.getApplicationIcon(e.packageName))
        } catch (_: Exception) {
        }

        setText(R.id.d_package, e.packageName)
        setText(R.id.d_version, "v${e.versionName} (${e.versionCode})")
        setText(R.id.d_sdk, "min ${e.minSdk} • target ${e.targetSdk}")
        setText(R.id.d_size, formatBytes(e.totalBytes))
        setText(R.id.d_installed, formatDate(e.firstInstallTime))
        setText(R.id.d_updated, formatDate(e.lastUpdateTime))
        setText(R.id.d_path, e.sourceDir)
        setText(
            R.id.d_splits,
            if (e.splitDirs.isEmpty()) "tidak ada (single APK)"
            else e.splitDirs.joinToString("\n") { File(it).name }
        )
        setText(R.id.d_sha, "menghitung…")

        lifecycleScope.launch(Dispatchers.IO) {
            val sha = try {
                HashUtil.sha256(File(e.sourceDir))
            } catch (_: Exception) {
                "-"
            }
            val perms = readPermissions(e.packageName)
            withContext(Dispatchers.Main) {
                setText(R.id.d_sha, sha)
                setText(
                    R.id.d_perms,
                    if (perms.isEmpty()) "tidak ada / tidak terbaca" else perms.joinToString("\n")
                )
            }
        }

        findViewById<Button>(R.id.btn_extract).setOnClickListener { extractNow() }
        findViewById<Button>(R.id.btn_strings).setOnClickListener { scanNow() }
        findViewById<Button>(R.id.btn_share).setOnClickListener { shareNow() }
    }

    private fun setText(id: Int, s: String) {
        findViewById<TextView>(id).text = s
    }

    private fun readPermissions(pkg: String): List<String> {
        return try {
            val pi = if (Build.VERSION.SDK_INT >= 33) {
                packageManager.getPackageInfo(
                    pkg, PackageManager.PackageInfoFlags.of(PackageManager.GET_PERMISSIONS.toLong())
                )
            } else {
                @Suppress("DEPRECATION")
                packageManager.getPackageInfo(pkg, PackageManager.GET_PERMISSIONS)
            }
            pi.requestedPermissions?.toList() ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun extractNow() {
        val e = entry ?: return
        val bar = findViewById<ProgressBar>(R.id.d_progress)
        bar.visibility = View.VISIBLE
        lifecycleScope.launch(Dispatchers.IO) {
            val saved = Extractor.saveToDownloads(this@DetailActivity, e)
            withContext(Dispatchers.Main) {
                bar.visibility = View.GONE
                Toast.makeText(
                    this@DetailActivity,
                    if (saved.isNotEmpty()) "Tersimpan ${saved.size} file ke Download/ApkExtractor"
                    else "Gagal mengekstrak",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun scanNow() {
        val e = entry ?: return
        val bar = findViewById<ProgressBar>(R.id.d_progress)
        val out = findViewById<TextView>(R.id.d_output)
        bar.visibility = View.VISIBLE
        out.visibility = View.VISIBLE
        out.text = "Memindai strings…"
        lifecycleScope.launch(Dispatchers.IO) {
            val r = StringsScanner.scan(File(e.sourceDir))
            withContext(Dispatchers.Main) {
                bar.visibility = View.GONE
                val head = StringBuilder()
                head.append("Total strings (>=4 char): ${r.total}\n")
                head.append("Menampilkan ${r.samples.size} pertama:\n\n")
                r.samples.forEach { head.append(it).append('\n') }
                out.text = head.toString()
            }
        }
    }

    private fun shareNow() {
        val e = entry ?: return
        lifecycleScope.launch(Dispatchers.IO) {
            val name = Extractor.fileNameFor(e.label, e.versionName)
            val f = Extractor.copyToCache(this@DetailActivity, e.sourceDir, name)
            withContext(Dispatchers.Main) {
                if (f == null) {
                    Toast.makeText(this@DetailActivity, "Gagal menyiapkan file", Toast.LENGTH_SHORT).show()
                    return@withContext
                }
                val uri = FileProvider.getUriForFile(
                    this@DetailActivity, "${BuildConfig.APPLICATION_ID}.fileprovider", f
                )
                val i = Intent(Intent.ACTION_SEND).apply {
                    type = "application/vnd.android.package-archive"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                startActivity(Intent.createChooser(i, "Bagikan APK"))
            }
        }
    }
}
