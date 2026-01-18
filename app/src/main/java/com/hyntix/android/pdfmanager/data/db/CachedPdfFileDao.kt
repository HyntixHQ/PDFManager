package com.hyntix.android.pdfmanager.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.hyntix.android.pdfmanager.data.model.CachedPdfFile
import kotlinx.coroutines.flow.Flow

/**
 * DAO for cached PDF file operations.
 * Provides instant loading of file lists from cache.
 */
@Dao
interface CachedPdfFileDao {
    
    /**
     * Get all cached files as a reactive Flow.
     */
    @Query("SELECT * FROM cached_pdf_files ORDER BY lastModified DESC")
    fun getAllCachedFilesFlow(): Flow<List<CachedPdfFile>>
    
    /**
     * Get all cached files synchronously (for non-reactive use).
     */
    @Query("SELECT * FROM cached_pdf_files ORDER BY lastModified DESC")
    suspend fun getAllCachedFiles(): List<CachedPdfFile>
    
    /**
     * Insert or update files in cache.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(files: List<CachedPdfFile>)
    
    /**
     * Delete a specific file from cache.
     */
    @Query("DELETE FROM cached_pdf_files WHERE path = :path")
    suspend fun delete(path: String)
    
    /**
     * Delete files that no longer exist (paths not in the provided list).
     */
    @Query("DELETE FROM cached_pdf_files WHERE path NOT IN (:validPaths)")
    suspend fun deleteStale(validPaths: List<String>)
    
    /**
     * Get count of cached files.
     */
    @Query("SELECT COUNT(*) FROM cached_pdf_files")
    suspend fun getCount(): Int
    
    /**
     * Get the most recent scan timestamp.
     */
    @Query("SELECT MAX(lastScanned) FROM cached_pdf_files")
    suspend fun getLastScanTime(): Long?
    
    /**
     * Clear all cache entries.
     */
    @Query("DELETE FROM cached_pdf_files")
    suspend fun clearAll()
    
    /**
     * Update a file's name (after rename operation).
     */
    @Query("UPDATE cached_pdf_files SET name = :newName, path = :newPath, uri = :newUri WHERE path = :oldPath")
    suspend fun updateFileName(oldPath: String, newPath: String, newName: String, newUri: String)
}
