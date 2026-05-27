package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.AppDatabase
import com.example.data.database.PdfBookmark
import com.example.data.database.PdfFile
import com.example.data.repository.PdfRepository
import com.example.utils.SamplePdfGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File

class PdfViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: PdfRepository
    private val _currentPdf = MutableStateFlow<PdfFile?>(null)
    val currentPdf: StateFlow<PdfFile?> = _currentPdf.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // Database Flows
    val allPdfFiles: StateFlow<List<PdfFile>>
    val favoritePdfFiles: StateFlow<List<PdfFile>>

    init {
        val database = AppDatabase.getDatabase(application)
        repository = PdfRepository(database.pdfDao())

        // Combine search query and all files Flow
        allPdfFiles = repository.allPdfFiles
            .combine(_searchQuery) { files, query ->
                if (query.isBlank()) {
                    files
                } else {
                    files.filter { it.name.contains(query, ignoreCase = true) }
                }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

        favoritePdfFiles = repository.favoritePdfFiles
            .combine(_searchQuery) { files, query ->
                if (query.isBlank()) {
                    files
                } else {
                    files.filter { it.name.contains(query, ignoreCase = true) }
                }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

        // Automatically create and add the welcome sample PDF on first launch if list is empty
        viewModelScope.launch(Dispatchers.IO) {
            repository.allPdfFiles.first().let { currentList ->
                if (currentList.isEmpty()) {
                    val welcomeFile = SamplePdfGenerator.generateWelcomePdf(application)
                    registerWelcomePdf(welcomeFile)
                }
            }
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun clearError() {
        _errorMessage.value = null
    }

    private suspend fun registerWelcomePdf(file: File) {
        val uriStr = Uri.fromFile(file).toString()
        val pageCount = getPageCount(file)
        
        val pdf = PdfFile(
            uri = uriStr,
            name = file.name.replace("_", " "),
            size = file.length(),
            pageCount = pageCount,
            lastOpened = System.currentTimeMillis(),
            isFavorite = false,
            currentPage = 0
        )
        repository.insertPdfFile(pdf)
    }

    // Opens a PDF, takes persistent read permission if it is a content content:// URI
    fun openPdf(uri: Uri, context: Context, forceNavigate: () -> Unit = {}) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                var fileName = "Unknown Document.pdf"
                var fileSize = 0L

                // Persist permissions for Uri if possible
                if (uri.scheme == "content") {
                    try {
                        context.contentResolver.takePersistableUriPermission(
                            uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                        )
                    } catch (e: SecurityException) {
                        Log.w("PdfViewModel", "Couldn't take persistable permissions: ${e.message}")
                    }

                    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                        if (cursor.moveToFirst()) {
                            if (nameIndex != -1) fileName = cursor.getString(nameIndex)
                            if (sizeIndex != -1) fileSize = cursor.getLong(sizeIndex)
                        }
                    }
                } else if (uri.scheme == "file") {
                    val file = uri.path?.let { File(it) }
                    if (file != null && file.exists()) {
                        fileName = file.name
                        fileSize = file.length()
                    }
                } else {
                    // Fallback from last path segment or string
                    fileName = uri.lastPathSegment ?: "Document.pdf"
                }

                // Query page count
                var pageCount = 0
                var fileDesc: ParcelFileDescriptor? = null
                try {
                    fileDesc = context.contentResolver.openFileDescriptor(uri, "r")
                    if (fileDesc != null) {
                        val renderer = PdfRenderer(fileDesc)
                        pageCount = renderer.pageCount
                        renderer.close()
                    }
                } catch (e: Exception) {
                    Log.e("PdfViewModel", "Error reading PDF pages with PdfRenderer", e)
                } finally {
                    fileDesc?.close()
                }

                if (pageCount == 0) {
                    _errorMessage.value = "Unable to read file. Please ensure it's a valid PDF."
                    return@launch
                }

                // Check database to restore last currentPage if previously opened
                val existing = repository.getPdfFileByUri(uri.toString())
                val currentPageToResume = existing?.currentPage ?: 0
                val wasFav = existing?.isFavorite ?: false

                val openedPdf = PdfFile(
                    uri = uri.toString(),
                    name = fileName,
                    size = fileSize,
                    pageCount = pageCount,
                    lastOpened = System.currentTimeMillis(),
                    isFavorite = wasFav,
                    currentPage = currentPageToResume
                )

                // Add to room database list
                repository.insertPdfFile(openedPdf)

                // Set as active
                _currentPdf.value = openedPdf
                viewModelScope.launch(Dispatchers.Main) {
                    forceNavigate()
                }
            } catch (e: Exception) {
                Log.e("PdfViewModel", "Failed to open PDF: ${e.message}", e)
                _errorMessage.value = "Failed to open this PDF file. Check permissions or file status."
            }
        }
    }

    fun openSpecificPdf(pdfFile: PdfFile) {
        viewModelScope.launch(Dispatchers.IO) {
            // Update last opened timestamp
            val updated = pdfFile.copy(lastOpened = System.currentTimeMillis())
            repository.updatePdfFile(updated)
            _currentPdf.value = updated
        }
    }

    fun updateReadingPage(pdfFile: PdfFile, currentPage: Int) {
        if (currentPage < 0 || currentPage >= pdfFile.pageCount) return
        viewModelScope.launch(Dispatchers.IO) {
            val updated = pdfFile.copy(currentPage = currentPage, lastOpened = System.currentTimeMillis())
            repository.updatePdfFile(updated)
            // Also update current active state if matches current active PDF
            if (_currentPdf.value?.uri == pdfFile.uri) {
                _currentPdf.value = updated
            }
        }
    }

    fun toggleFavorite(pdfFile: PdfFile) {
        viewModelScope.launch(Dispatchers.IO) {
            val updated = pdfFile.copy(isFavorite = !pdfFile.isFavorite)
            repository.updatePdfFile(updated)
            if (_currentPdf.value?.uri == pdfFile.uri) {
                _currentPdf.value = updated
            }
        }
    }

    fun deleteRecentPdf(pdfFile: PdfFile) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deletePdfFileByUri(pdfFile.uri)
            if (_currentPdf.value?.uri == pdfFile.uri) {
                _currentPdf.value = null
            }
        }
    }

    // Bookmarks Management
    fun getBookmarks(pdfUri: String): Flow<List<PdfBookmark>> {
        return repository.getBookmarksForPdf(pdfUri)
    }

    fun addBookmark(pdfUri: String, pageIndex: Int, description: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val bookmark = PdfBookmark(
                pdfUri = pdfUri,
                pageIndex = pageIndex,
                label = description.ifBlank { "Page ${pageIndex + 1}" }
            )
            repository.insertBookmark(bookmark)
        }
    }

    fun removeBookmark(bookmarkId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteBookmarkById(bookmarkId)
        }
    }

    fun removeBookmarkAtPage(pdfUri: String, pageIndex: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteBookmarkAtPage(pdfUri, pageIndex)
        }
    }

    // Helper to extract page count from local file
    private fun getPageCount(file: File): Int {
        return try {
            val fd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = PdfRenderer(fd)
            val count = renderer.pageCount
            renderer.close()
            fd.close()
            count
        } catch (e: Exception) {
            0
        }
    }
}
