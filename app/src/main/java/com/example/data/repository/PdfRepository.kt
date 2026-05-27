package com.example.data.repository

import com.example.data.database.PdfBookmark
import com.example.data.database.PdfDao
import com.example.data.database.PdfFile
import kotlinx.coroutines.flow.Flow

class PdfRepository(private val pdfDao: PdfDao) {
    val allPdfFiles: Flow<List<PdfFile>> = pdfDao.getAllPdfFiles()
    val favoritePdfFiles: Flow<List<PdfFile>> = pdfDao.getFavoritePdfFiles()

    suspend fun getPdfFileByUri(uri: String): PdfFile? {
        return pdfDao.getPdfFileByUri(uri)
    }

    suspend fun insertPdfFile(pdfFile: PdfFile) {
        pdfDao.insertPdfFile(pdfFile)
    }

    suspend fun updatePdfFile(pdfFile: PdfFile) {
        pdfDao.updatePdfFile(pdfFile)
    }

    suspend fun deletePdfFileByUri(uri: String) {
        pdfDao.deletePdfFileByUri(uri)
    }

    // Bookmarks
    fun getBookmarksForPdf(pdfUri: String): Flow<List<PdfBookmark>> {
        return pdfDao.getBookmarksForPdf(pdfUri)
    }

    suspend fun insertBookmark(bookmark: PdfBookmark) {
        pdfDao.insertBookmark(bookmark)
    }

    suspend fun deleteBookmarkById(bookmarkId: Int) {
        pdfDao.deleteBookmarkById(bookmarkId)
    }

    suspend fun deleteBookmarkAtPage(pdfUri: String, pageIndex: Int) {
        pdfDao.deleteBookmarkAtPage(pdfUri, pageIndex)
    }
}
