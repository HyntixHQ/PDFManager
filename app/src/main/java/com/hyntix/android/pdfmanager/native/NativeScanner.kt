package com.hyntix.android.pdfmanager.native

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Native file scanner using Rust.
 * Uses Rust-based native library for 5-10x faster scanning.
 */
object NativeScanner {
    
    private const val TAG = "NativeScanner"
    
    data class FileInfo(
        val path: String,
        val size: Long,
        val lastModified: Long
    )
    
    data class DuplicateGroup(
        val hash: String,
        val paths: Array<String>
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is DuplicateGroup) return false
            return hash == other.hash && paths.contentEquals(other.paths)
        }
        override fun hashCode(): Int = 31 * hash.hashCode() + paths.contentHashCode()
    }
    
    init {
        System.loadLibrary("pdf_scanner")
        Log.i(TAG, "Native scanner loaded successfully")
    }
    
    /**
     * Check if native scanner is available.
     */
    fun isAvailable(): Boolean = true
    
    /**
     * Scan for PDF files using native code.
     */
    suspend fun scanPdfsAsync(rootPath: String): List<String> = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        val nativeResult = scanPdfs(rootPath)
        val result = nativeResult?.toList() ?: emptyList()
        val elapsed = System.currentTimeMillis() - startTime
        Log.d(TAG, "Native scan found ${result.size} PDFs in ${elapsed}ms")
        result
    }
    
    /**
     * Scan for PDF files with metadata using native code.
     */
    suspend fun scanPdfsWithInfoAsync(rootPath: String): List<FileInfo> = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        val nativeResult = scanPdfsWithInfo(rootPath)
        val result = nativeResult?.toList() ?: emptyList()
        val elapsed = System.currentTimeMillis() - startTime
        Log.d(TAG, "Native scan with info found ${result.size} PDFs in ${elapsed}ms")
        result
    }
    
    /**
     * Find duplicate PDF files using content hashing.
     * Uses 3-phase approach: size grouping -> partial hash -> full MD5 hash.
     */
    suspend fun findDuplicatesAsync(rootPath: String): List<DuplicateGroup> = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        val nativeResult = findDuplicates(rootPath)
        val result = nativeResult?.toList() ?: emptyList()
        val elapsed = System.currentTimeMillis() - startTime
        Log.d(TAG, "Native duplicate scan found ${result.size} groups in ${elapsed}ms")
        result
    }
    
    // Native methods (implemented in Rust)
    private external fun scanPdfs(rootPath: String): Array<String>?
    private external fun scanPdfsWithInfo(rootPath: String): Array<FileInfo>?
    private external fun findDuplicates(rootPath: String): Array<DuplicateGroup>?
}
