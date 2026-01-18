package com.hyntix.android.pdfmanager.data.repository

import android.content.Context
import android.util.Log
import com.hyntix.android.pdfmanager.data.db.AppDatabase
import com.hyntix.android.pdfmanager.data.model.CachedPdfFile
import com.hyntix.android.pdfmanager.data.model.PdfFile
import com.hyntix.android.pdfmanager.data.model.toCachedPdfFile
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * Repository for PDF file operations.
 * Uses cache-first pattern for instant file list loading.
 */
class FileRepository(private val context: Context) {
    
    companion object {
        private const val TAG = "FileRepository"
        private const val CACHE_VALIDITY_MS = 5 * 60 * 1000L  // 5 minutes
    }
    
    private val cachedPdfDao = AppDatabase.getDatabase(context).cachedPdfFileDao()
    
    /**
     * Reactive flow of cached PDF files.
     * Updates automatically when cache changes.
     */
    val cachedPdfsFlow: Flow<List<PdfFile>> = cachedPdfDao.getAllCachedFilesFlow()
        .map { cached -> cached.map { it.toPdfFile() } }
    
    /**
     * Get all PDFs with cache-first pattern.
     * Returns cached data immediately if fresh, then optionally refreshes in background.
     * 
     * @param forceRefresh If true, always perform full filesystem scan
     * @return List of PDF files
     */
    suspend fun getAllPdfs(forceRefresh: Boolean = false): List<PdfFile> = withContext(Dispatchers.IO) {
        // Check cache first
        if (!forceRefresh) {
            val cachedCount = cachedPdfDao.getCount()
            val lastScan = cachedPdfDao.getLastScanTime() ?: 0
            val cacheAge = System.currentTimeMillis() - lastScan
            
            // Use cache if it has entries and is fresh
            if (cachedCount > 0 && cacheAge < CACHE_VALIDITY_MS) {
                Log.d(TAG, "Using cached files: $cachedCount entries, age: ${cacheAge / 1000}s")
                return@withContext cachedPdfDao.getAllCachedFiles().map { it.toPdfFile() }
            }
        }
        
        Log.d(TAG, "Performing full filesystem scan")
        
        // Perform full scan
        val scannedFiles = scanFilesystem()
        
        // Update cache
        if (scannedFiles.isNotEmpty()) {
            val cachedFiles = scannedFiles.map { it.toCachedPdfFile() }
            cachedPdfDao.insertAll(cachedFiles)
            
            // Remove stale entries (files that no longer exist)
            val validPaths = scannedFiles.map { it.path }
            cachedPdfDao.deleteStale(validPaths)
            
            Log.d(TAG, "Cache updated with ${scannedFiles.size} files")
        }
        
        scannedFiles
    }
    
    /**
     * Perform full filesystem scan for PDF files.
     * Uses MediaStore API first (10-100x faster), falls back to file walk.
     */
    private suspend fun scanFilesystem(): List<PdfFile> {
        // Try MediaStore first (much faster)
        try {
            val mediaStoreResults = com.hyntix.android.pdfmanager.data.scanner.MediaStorePdfScanner.scanPdfs(context)
            if (mediaStoreResults.isNotEmpty()) {
                Log.d(TAG, "MediaStore scan found ${mediaStoreResults.size} PDFs")
                return mediaStoreResults
            }
        } catch (e: Exception) {
            Log.w(TAG, "MediaStore scan failed, falling back to file walk", e)
        }
        
        // Fallback to traditional file walk
        return scanFilesystemLegacy()
    }
    
    /**
     * Legacy filesystem scan using file walk.
     * Slower but more reliable as fallback.
     */
    private fun scanFilesystemLegacy(): List<PdfFile> {
        val root = android.os.Environment.getExternalStorageDirectory()
        val pdfList = mutableListOf<PdfFile>()
        
        try {
            root.walk()
                .filter { file -> 
                    file.isFile && file.extension.equals("pdf", ignoreCase = true) 
                }
                .forEach { file ->
                    pdfList.add(
                        PdfFile(
                            name = file.name,
                            path = file.absolutePath,
                            size = file.length(),
                            lastModified = file.lastModified(),
                            uri = "file://${file.absolutePath}",
                            isDirectory = false
                        )
                    )
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error scanning filesystem", e)
        }
        
        return pdfList.sortedByDescending { it.lastModified }
    }

    suspend fun getFiles(path: String): List<PdfFile> = withContext(Dispatchers.IO) {
        val directory = File(path)
        val files = directory.listFiles() ?: return@withContext emptyList()
        
        files.filter { !it.isHidden && (it.isDirectory || it.extension.equals("pdf", ignoreCase = true)) }
            .map { file ->
            PdfFile(
                name = file.name,
                path = file.absolutePath,
                size = if (file.isDirectory) 0 else file.length(),
                lastModified = file.lastModified(),
                uri = "file://${file.absolutePath}",
                isDirectory = file.isDirectory
            )
        }.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
    }
    
    suspend fun renameFile(filePath: String, newName: String): Boolean = withContext(Dispatchers.IO) {
        val file = File(filePath)
        val newFile = File(file.parent, newName)
        if (newFile.exists()) return@withContext false
        
        val success = file.renameTo(newFile)
        
        // Update cache if successful
        if (success) {
            cachedPdfDao.updateFileName(
                oldPath = filePath,
                newPath = newFile.absolutePath,
                newName = newName,
                newUri = "file://${newFile.absolutePath}"
            )
        }
        
        success
    }

    suspend fun deleteFile(filePath: String): Boolean = withContext(Dispatchers.IO) {
        val file = File(filePath)
        val success = file.delete()
        
        // Remove from cache if successful
        if (success) {
            cachedPdfDao.delete(filePath)
        }
        
        success
    }
    
    /**
     * Clear all cached files.
     */
    suspend fun clearCache() = withContext(Dispatchers.IO) {
        cachedPdfDao.clearAll()
    }
}
