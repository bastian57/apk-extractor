package com.daftech.apkextractor

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class AppAdapter(
    private val onItemClick: (AppEntry) -> Unit,
    private val onItemLongClick: (AppEntry) -> Boolean,
    private val onExtractClick: (AppEntry) -> Unit,
    private val onSelectionChanged: () -> Unit
) : RecyclerView.Adapter<AppAdapter.VH>() {

    var items: List<AppEntry> = emptyList()
    var selectionMode: Boolean = false
        private set
    val selected = mutableSetOf<String>()

    inner class VH(v: View) : RecyclerView.ViewHolder(v) {
        val icon: ImageView = v.findViewById(R.id.app_icon)
        val name: TextView = v.findViewById(R.id.app_name)
        val sub: TextView = v.findViewById(R.id.app_sub)
        val size: TextView = v.findViewById(R.id.app_size)
        val check: CheckBox = v.findViewById(R.id.app_check)
        val extract: ImageButton = v.findViewById(R.id.btn_quick_extract)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_app, parent, false)
        return VH(v)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(h: VH, position: Int) {
        val e = items[position]
        val pm = h.itemView.context.packageManager
        try {
            h.icon.setImageDrawable(pm.getApplicationIcon(e.packageName))
        } catch (_: Exception) {
            h.icon.setImageResource(android.R.drawable.sym_def_app_icon)
        }
        h.name.text = e.label
        h.sub.text = "${e.packageName} • v${e.versionName}"
        h.size.text = formatBytes(e.totalBytes)
        h.check.visibility = if (selectionMode) View.VISIBLE else View.GONE
        h.check.isChecked = selected.contains(e.packageName)
        h.extract.visibility = if (selectionMode) View.GONE else View.VISIBLE

        h.itemView.setOnClickListener { onItemClick(e) }
        h.itemView.setOnLongClickListener { onItemLongClick(e) }
        h.extract.setOnClickListener { onExtractClick(e) }
        h.check.setOnClickListener { toggleSelect(e) }
    }

    fun enterSelectionMode(first: AppEntry) {
        selectionMode = true
        selected.add(first.packageName)
        notifyDataSetChanged()
        onSelectionChanged()
    }

    fun exitSelectionMode() {
        selectionMode = false
        selected.clear()
        notifyDataSetChanged()
    }

    fun toggleSelect(e: AppEntry) {
        if (!selected.add(e.packageName)) {
            selected.remove(e.packageName)
        }
        if (selected.isEmpty()) {
            exitSelectionMode()
        } else {
            notifyDataSetChanged()
        }
        onSelectionChanged()
    }

    fun submitList(list: List<AppEntry>) {
        items = list
        notifyDataSetChanged()
    }
}
