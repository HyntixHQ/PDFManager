package com.hyntix.android.pdfmanager.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.hyntix.android.pdfmanager.data.model.FolderPdfCrossRef
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for folder-PDF associations.
 */
@Dao
interface FolderPdfCrossRefDao {
    
    /**
     * Add a PDF to a folder. Ignores if already exists.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addPdfToFolder(crossRef: FolderPdfCrossRef)
    
    /**
     * Add multiple PDFs to a folder at once.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addPdfsToFolder(crossRefs: List<FolderPdfCrossRef>)
    
    /**
     * Remove a single PDF from a folder.
     */
    @Query("DELETE FROM folder_pdf_cross_ref WHERE folderId = :folderId AND pdfUri = :pdfUri")
    suspend fun removePdfFromFolder(folderId: Long, pdfUri: String)
    
    /**
     * Remove multiple PDFs from a folder.
     */
    @Query("DELETE FROM folder_pdf_cross_ref WHERE folderId = :folderId AND pdfUri IN (:pdfUris)")
    suspend fun removePdfsFromFolder(folderId: Long, pdfUris: List<String>)
    
    /**
     * Get all PDF URIs in a specific folder.
     */
    @Query("SELECT pdfUri FROM folder_pdf_cross_ref WHERE folderId = :folderId ORDER BY addedAt DESC")
    fun getPdfUrisInFolder(folderId: Long): Flow<List<String>>
    
    /**
     * Get all folder IDs that contain a specific PDF.
     */
    @Query("SELECT folderId FROM folder_pdf_cross_ref WHERE pdfUri = :pdfUri")
    fun getFolderIdsForPdf(pdfUri: String): Flow<List<Long>>
    
    /**
     * Get PDF count in a folder (for chip display).
     */
    @Query("SELECT COUNT(*) FROM folder_pdf_cross_ref WHERE folderId = :folderId")
    fun getPdfCountInFolder(folderId: Long): Flow<Int>
    
    /**
     * Get all folder IDs that contain a specific PDF (non-reactive version).
     */
    @Query("SELECT folderId FROM folder_pdf_cross_ref WHERE pdfUri = :pdfUri")
    suspend fun getFolderIdsForPdfSync(pdfUri: String): List<Long>
}
