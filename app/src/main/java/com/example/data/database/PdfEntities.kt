package com.example.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pdf_files")
data class PdfFile(
    @PrimaryKey val uri: String, // Local URI or Custom Uri e.g., cache:// or assets://
    val name: String,
    val size: Long,
    val pageCount: Int,
    val lastOpened: Long,
    val isFavorite: Boolean = false,
    val currentPage: Int = 0
)

@Entity(tableName = "pdf_bookmarks")
data class PdfBookmark(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val pdfUri: String,
    val pageIndex: Int,
    val label: String,
    val timestamp: Long = System.currentTimeMillis()
)
