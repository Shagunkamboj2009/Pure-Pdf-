package com.example.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PdfDao {
    @Query("SELECT * FROM pdf_files ORDER BY lastOpened DESC")
    fun getAllPdfFiles(): Flow<List<PdfFile>>

    @Query("SELECT * FROM pdf_files WHERE isFavorite = 1 ORDER BY name ASC")
    fun getFavoritePdfFiles(): Flow<List<PdfFile>>

    @Query("SELECT * FROM pdf_files WHERE uri = :uri LIMIT 1")
    suspend fun getPdfFileByUri(uri: String): PdfFile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPdfFile(pdfFile: PdfFile)

    @Update
    suspend fun updatePdfFile(pdfFile: PdfFile)

    @Query("DELETE FROM pdf_files WHERE uri = :uri")
    suspend fun deletePdfFileByUri(uri: String)

    // Bookmarks
    @Query("SELECT * FROM pdf_bookmarks WHERE pdfUri = :pdfUri ORDER BY pageIndex ASC")
    fun getBookmarksForPdf(pdfUri: String): Flow<List<PdfBookmark>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: PdfBookmark)

    @Query("DELETE FROM pdf_bookmarks WHERE id = :bookmarkId")
    suspend fun deleteBookmarkById(bookmarkId: Int)

    @Query("DELETE FROM pdf_bookmarks WHERE pdfUri = :pdfUri AND pageIndex = :pageIndex")
    suspend fun deleteBookmarkAtPage(pdfUri: String, pageIndex: Int)
}
