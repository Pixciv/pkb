package com.example.app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.DateFormat
import java.util.*

class FileAdapter(
    private val items: MutableList<FileItem>,
    private val listener: Listener
) : RecyclerView.Adapter<FileAdapter.VH>() {

    interface Listener {
        fun onOpen(item: FileItem)
        fun onToggleFavorite(item: FileItem)
        fun onRename(item: FileItem)
        fun onDelete(item: FileItem)
        fun onLongPress(item: FileItem)
        fun onMenu(item: FileItem, anchor: View)
    }

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val icon: ImageView = view.findViewById(R.id.icon)
        val title: TextView = view.findViewById(R.id.title)
        val sub: TextView = view.findViewById(R.id.sub)
        val menuBtn: ImageButton = view.findViewById(R.id.menu_btn)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_file, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val it = items[position]
        holder.title.text = it.name
        val df = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
        holder.sub.text = "${humanReadableByteCount(it.size)} · ${df.format(Date(it.lastModified))}"
        // icon by type (basic)
        holder.icon.setImageResource(if (it.type == "pdf") R.drawable.ic_file else R.drawable.ic_file)

        holder.itemView.setOnClickListener { listener.onOpen(it) }
        holder.itemView.setOnLongClickListener {
            listener.onLongPress(it)
            true
        }
        holder.menuBtn.setOnClickListener { listener.onMenu(it, holder.menuBtn) }
    }

    override fun getItemCount(): Int = items.size

    fun updateList(newList: List<FileItem>) {
        items.clear()
        items.addAll(newList)
        notifyDataSetChanged()
    }

    private fun humanReadableByteCount(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        var digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
        return String.format("%.1f %s", bytes / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
    }
}
