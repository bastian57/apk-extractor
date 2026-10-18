package com.daftech.apkextractor

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.chip.ChipGroup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var adapter: AppAdapter
    private var allApps: List<AppEntry> = emptyList()
    private var filter = 0 // 0=semua, 1=user, 2=sistem
    private var sortMode = 0 // 0=nama, 1=ukuran, 2=terbaru
    private var query = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<MaterialToolbar>(R.id.toolbar).title = getString(R.string.app_name)

        // Izin storage hanya relevan di Android 8-9 (API <= 28).
        if (Build.VERSION.SDK_INT <= 28 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 1
            )
        }

        val rv = findViewById<RecyclerView>(R.id.recycler)
        rv.layoutManager = LinearLayoutManager(this)
        adapter = AppAdapter(
            onItemClick = { e ->
                if (adapter.selectionMode) adapter.toggleSelect(e) else openDetail(e)
            },
            onItemLongClick = { e ->
                adapter.enterSelectionMode(e)
                updateSelectionBar()
                true
            },
            onExtractClick = { e -> quickExtract(listOf(e)) },
            onSelectionChanged = { updateSelectionBar() }
        )
        rv.adapter = adapter

        findViewById<EditText>(R.id.search).addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun afterTextChanged(s: Editable?) {
                query = s?.toString()?.trim() ?: ""
                applyFilter()
            }
        })

        findViewById<ChipGroup>(R.id.chips).setOnCheckedStateChangeListener { _, checkedIds ->
            filter = when {
                checkedIds.contains(R.id.chip_user) -> 1
                checkedIds.contains(R.id.chip_system) -> 2
                else -> 0
            }
            applyFilter()
        }

        val sort = findViewById<Spinner>(R.id.sort)
        sort.adapter = ArrayAdapter(
            this, android.R.layout.simple_spinner_dropdown_item,
            listOf("Nama", "Ukuran", "Terbaru")
        )
        sort.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                sortMode = pos
                applyFilter()
            }

            override fun onNothingSelected(p: AdapterView<*>?) {}
        }

        findViewById<Button>(R.id.btn_extract_selected).setOnClickListener {
            val sel = adapter.selected
                .mapNotNull { pkg -> allApps.firstOrNull { it.packageName == pkg } }
            if (sel.isNotEmpty()) quickExtract(sel)
        }
        findViewById<Button>(R.id.btn_cancel_select).setOnClickListener {
            adapter.exitSelectionMode()
            updateSelectionBar()
        }

        loadApps()
    }

    private fun loadApps() {
        val progress = findViewById<ProgressBar>(R.id.progress)
        progress.visibility = View.VISIBLE
        lifecycleScope.launch(Dispatchers.IO) {
            val list = AppLoader.load(this@MainActivity)
            withContext(Dispatchers.Main) {
                allApps = list
                progress.visibility = View.GONE
                findViewById<TextView>(R.id.count).text = "${list.size} aplikasi"
                applyFilter()
            }
        }
    }

    private fun applyFilter() {
        var list = allApps
        list = when (filter) {
            1 -> list.filter { !it.isSystem }
            2 -> list.filter { it.isSystem }
            else -> list
        }
        if (query.isNotEmpty()) {
            val q = query.lowercase()
            list = list.filter {
                it.label.lowercase().contains(q) || it.packageName.lowercase().contains(q)
            }
        }
        list = when (sortMode) {
            1 -> list.sortedByDescending { it.totalBytes }
            2 -> list.sortedByDescending { it.lastUpdateTime }
            else -> list.sortedBy { it.label.lowercase() }
        }
        adapter.submitList(list)
    }

    private fun openDetail(e: AppEntry) {
        val i = Intent(this, DetailActivity::class.java).putExtra("pkg", e.packageName)
        startActivity(i)
    }

    private fun quickExtract(entries: List<AppEntry>) {
        val bar = findViewById<View>(R.id.extract_progress_wrap)
        bar.visibility = View.VISIBLE
        lifecycleScope.launch(Dispatchers.IO) {
            var files = 0
            var apps = 0
            for (e in entries) {
                val saved = Extractor.saveToDownloads(this@MainActivity, e)
                if (saved.isNotEmpty()) {
                    apps++
                    files += saved.size
                }
            }
            withContext(Dispatchers.Main) {
                bar.visibility = View.GONE
                if (adapter.selectionMode) {
                    adapter.exitSelectionMode()
                    updateSelectionBar()
                }
                Toast.makeText(
                    this@MainActivity,
                    if (apps > 0) "Tersimpan: $files file dari $apps aplikasi ke Download/ApkExtractor"
                    else "Gagal mengekstrak",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun updateSelectionBar() {
        val wrap = findViewById<View>(R.id.selection_bar)
        if (adapter.selectionMode) {
            wrap.visibility = View.VISIBLE
            findViewById<TextView>(R.id.sel_count).text = "${adapter.selected.size} dipilih"
        } else {
            wrap.visibility = View.GONE
        }
    }

    @Deprecated("legacy back handling")
    override fun onBackPressed() {
        if (adapter.selectionMode) {
            adapter.exitSelectionMode()
            updateSelectionBar()
        } else {
            super.onBackPressed()
        }
    }
}
