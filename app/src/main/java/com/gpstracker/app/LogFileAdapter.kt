package com.gpstracker.app

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.gpstracker.app.databinding.ItemLogFileBinding
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class LogFileAdapter(private val onItemClick: (File) -> Unit) : 
    ListAdapter<File, LogFileAdapter.LogFileViewHolder>(LogFileDiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LogFileViewHolder {
        val binding = ItemLogFileBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return LogFileViewHolder(binding, onItemClick)
    }
    
    override fun onBindViewHolder(holder: LogFileViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    class LogFileViewHolder(
        private val binding: ItemLogFileBinding,
        private val onItemClick: (File) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(file: File) {
            binding.apply {
                fileNameText.text = file.name
                fileSizeText.text = formatFileSize(file.length())
                fileDateText.text = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                    .format(Date(file.lastModified()))
                
                root.setOnClickListener {
                    onItemClick(file)
                }
            }
        }
        
        private fun formatFileSize(bytes: Long): String {
            return when {
                bytes < 1024 -> "$bytes B"
                bytes < 1024 * 1024 -> "${bytes / 1024} KB"
                bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
                else -> "${bytes / (1024 * 1024 * 1024)} GB"
            }
        }
    }
    
    class LogFileDiffCallback : DiffUtil.ItemCallback<File>() {
        override fun areItemsTheSame(oldItem: File, newItem: File): Boolean {
            return oldItem.absolutePath == newItem.absolutePath
        }
        
        override fun areContentsTheSame(oldItem: File, newItem: File): Boolean {
            return oldItem.lastModified() == newItem.lastModified() && oldItem.length() == newItem.length()
        }
    }
}
