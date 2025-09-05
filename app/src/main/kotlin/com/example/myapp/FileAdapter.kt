package com.example.myapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class FileAdapter(
    private val files: List<FileItem>,
    private val onItemClick: (FileItem) -> Unit
) : RecyclerView.Adapter<FileAdapter.FileViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FileViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_file, parent, false)
        return FileViewHolder(view)
    }

    override fun getItemCount(): Int = files.size

    override fun onBindViewHolder(holder: FileViewHolder, position: Int) {
        val file = files[position]
        holder.bind(file)
    }

    inner class FileViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val fileNameText: TextView = itemView.findViewById(R.id.fileName)
        private val fileDetailsText: TextView = itemView.findViewById(R.id.fileDetails)
        private val fileIcon: ImageView = itemView.findViewById(R.id.fileIcon)
        private val favoriteIcon: ImageView = itemView.findViewById(R.id.favoriteIcon)

        fun bind(file: FileItem) {
            fileNameText.text = file.name
            fileDetailsText.text = "${file.date} · ${file.size}"

            // Dosya türüne göre ikon
            fileIcon.setImageResource(
                when (file.type.lowercase()) {
                    "pdf" -> R.drawable.ic_pdf
                    "doc", "docx" -> R.drawable.ic_word
                    "xls", "xlsx" -> R.drawable.ic_excel
                    "ppt", "pptx" -> R.drawable.ic_powerpoint
                    else -> R.drawable.ic_file
                }
            )

            // Favori ikonu
            favoriteIcon.setImageResource(
                if (file.isFavorite) R.drawable.ic_star_filled else R.drawable.ic_star_outline
            )

            // Tıklama
            itemView.setOnClickListener { onItemClick(file) }

            // Favori tıklama
            favoriteIcon.setOnClickListener {
                file.isFavorite = !file.isFavorite
                notifyItemChanged(adapterPosition)
            }
        }
    }
}
