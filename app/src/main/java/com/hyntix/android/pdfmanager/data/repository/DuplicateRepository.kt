package com.hyntix.android.pdfmanager.data.repository

import com.hyntix.android.pdfmanager.native.NativeScanner
import java.io.File

/**
 * Repository for duplicate file detection and management.
 */
class DuplicateRepository {
    
    /**
     * Find duplicate PDF files in the given root path.
     */
    suspend fun findDuplicates(rootPath: String): List<NativeScanner.DuplicateGroup> {
        return NativeScanner.findDuplicatesAsync(rootPath)
    }
    
    /**
     * Delete the specified files.
     * Returns the number of successfully deleted files.
     */
    suspend fun deleteFiles(paths: List<String>): Int {
        var count = 0
        for (path in paths) {
            try {
                val file = File(path)
                if (file.exists() && file.delete()) {
                    count++
                }
            } catch (e: Exception) {
                // Log error but continue with other files
            }
        }
        return count
    }
    
    /**
     * Calculate total size of files in bytes.
     */
    fun calculateTotalSize(paths: List<String>): Long {
        return paths.sumOf { path ->
            try {
                File(path).length()
            } catch (e: Exception) {
                0L
            }
        }
    }
}
