package com.hyntix.android.pdfmanager.data.repository

import android.content.Context
import com.hyntix.android.pdfmanager.data.db.AppDatabase
import com.hyntix.android.pdfmanager.data.model.Folder
import com.hyntix.android.pdfmanager.data.model.FolderPdfCrossRef
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * Repository for virtual folder operations.
 * Manages folders and folder-PDF associations.
 */
class FolderRepository(context: Context) {
    
    private val folderDao = AppDatabase.getDatabase(context).folderDao()
    private val crossRefDao = AppDatabase.getDatabase(context).folderPdfCrossRefDao()
    
    /**
     * Reactive flow of all folders ordered by name.
     */
    val foldersFlow: Flow<List<Folder>> = folderDao.getAllFolders()
    
    /**
     * Create a new folder with the given name.
     * @return The ID of the created folder
     */
    suspend fun createFolder(name: String): Long = withContext(Dispatchers.IO) {
        folderDao.insertFolder(Folder(name = name.trim()))
    }
    
    /**
     * Create a new folder and add PDFs to it.
     * @return The ID of the created folder
     */
    suspend fun createFolderWithPdfs(name: String, pdfUris: List<String>): Long = withContext(Dispatchers.IO) {
        val folderId = folderDao.insertFolder(Folder(name = name.trim()))
        if (pdfUris.isNotEmpty()) {
            val crossRefs = pdfUris.map { uri ->
                FolderPdfCrossRef(folderId = folderId, pdfUri = uri)
            }
            crossRefDao.addPdfsToFolder(crossRefs)
        }
        folderId
    }
    
    /**
     * Rename an existing folder.
     */
    suspend fun renameFolder(folderId: Long, newName: String) = withContext(Dispatchers.IO) {
        folderDao.renameFolder(folderId, newName.trim())
    }
    
    /**
     * Delete a folder. PDFs remain on device, only the association is removed.
     */
    suspend fun deleteFolder(folderId: Long) = withContext(Dispatchers.IO) {
        folderDao.deleteFolder(folderId)
    }
    
    /**
     * Check if a folder with the given name already exists (case-insensitive).
     */
    suspend fun folderExists(name: String): Boolean = withContext(Dispatchers.IO) {
        folderDao.folderExists(name.trim())
    }
    
    /**
     * Get a folder by ID.
     */
    suspend fun getFolderById(folderId: Long): Folder? = withContext(Dispatchers.IO) {
        folderDao.getFolderById(folderId)
    }
    
    /**
     * Add PDFs to a folder.
     */
    suspend fun addPdfsToFolder(folderId: Long, pdfUris: List<String>) = withContext(Dispatchers.IO) {
        val crossRefs = pdfUris.map { uri ->
            FolderPdfCrossRef(folderId = folderId, pdfUri = uri)
        }
        crossRefDao.addPdfsToFolder(crossRefs)
    }
    
    /**
     * Remove PDFs from a folder.
     */
    suspend fun removePdfsFromFolder(folderId: Long, pdfUris: List<String>) = withContext(Dispatchers.IO) {
        crossRefDao.removePdfsFromFolder(folderId, pdfUris)
    }
    
    /**
     * Toggle a PDF's membership in a folder.
     */
    suspend fun togglePdfInFolder(folderId: Long, pdfUri: String, isInFolder: Boolean) = withContext(Dispatchers.IO) {
        if (isInFolder) {
            crossRefDao.addPdfToFolder(FolderPdfCrossRef(folderId = folderId, pdfUri = pdfUri))
        } else {
            crossRefDao.removePdfFromFolder(folderId, pdfUri)
        }
    }
    
    /**
     * Get reactive flow of PDF URIs in a specific folder.
     */
    fun getPdfUrisInFolder(folderId: Long): Flow<List<String>> {
        return crossRefDao.getPdfUrisInFolder(folderId)
    }
    
    /**
     * Get reactive flow of folder IDs that contain a specific PDF.
     */
    fun getFolderIdsForPdf(pdfUri: String): Flow<List<Long>> {
        return crossRefDao.getFolderIdsForPdf(pdfUri)
    }
    
    /**
     * Get folder IDs for a PDF synchronously (non-reactive).
     */
    suspend fun getFolderIdsForPdfSync(pdfUri: String): List<Long> = withContext(Dispatchers.IO) {
        crossRefDao.getFolderIdsForPdfSync(pdfUri)
    }
    
    /**
     * Get reactive flow of PDF count in a folder.
     */
    fun getPdfCountInFolder(folderId: Long): Flow<Int> {
        return crossRefDao.getPdfCountInFolder(folderId)
    }
}
