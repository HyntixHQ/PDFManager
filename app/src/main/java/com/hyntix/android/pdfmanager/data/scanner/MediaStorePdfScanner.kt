package com.hyntix.android.pdfmanager.data.scanner

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.provider.MediaStore
import android.util.Log
import com.hyntix.android.pdfmanager.data.model.PdfFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Fast PDF scanner using MediaStore API.
 * 10-100x faster than recursive file walk for large storage.
 * Falls back to traditional scanning if MediaStore is unavailable.
 */
object MediaStorePdfScanner {
    
    private const val TAG = "MediaStorePdfScanner"
    
    /**
     * Scan for all PDF files using MediaStore.
     * This is significantly faster than recursive file walking.
     */
    suspend fun scanPdfs(context: Context): List<PdfFile> = withContext(Dispatchers.IO) {
        val pdfs = mutableListOf<PdfFile>()
        
        try {
            val contentResolver = context.contentResolver
            
            // Query MediaStore for PDF files
            val projection = arrayOf(
                MediaStore.Files.FileColumns._ID,
                MediaStore.Files.FileColumns.DISPLAY_NAME,
                MediaStore.Files.FileColumns.DATA, // Full path
                MediaStore.Files.FileColumns.SIZE,
                MediaStore.Files.FileColumns.DATE_MODIFIED,
                MediaStore.Files.FileColumns.MIME_TYPE
            )
            
            // Selection for PDF files
            val selection = "${MediaStore.Files.FileColumns.MIME_TYPE} = ?"
            val selectionArgs = arrayOf("application/pdf")
            
            // Sort by most recently modified
            val sortOrder = "${MediaStore.Files.FileColumns.DATE_MODIFIED} DESC"
            
            val uri = MediaStore.Files.getContentUri("external")
            
            contentResolver.query(
                uri,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
                val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
                val pathColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA)
                val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE)
                val dateColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_MODIFIED)
                
                while (cursor.moveToNext()) {
                    try {
                        val id = cursor.getLong(idColumn)
                        val name = cursor.getString(nameColumn) ?: "Unknown"
                        val path = cursor.getString(pathColumn) ?: continue
                        val size = cursor.getLong(sizeColumn)
                        val dateModified = cursor.getLong(dateColumn) * 1000 // Convert to millis
                        
                        // Skip if file doesn't exist (MediaStore may be stale)
                        val file = java.io.File(path)
                        if (!file.exists()) continue
                        
                        pdfs.add(PdfFile(
                            name = name,
                            path = path,
                            uri = "file://$path",
                            size = size,
                            lastModified = dateModified,
                            isDirectory = false
                        ))
                    } catch (e: Exception) {
                        Log.w(TAG, "Error reading file entry", e)
                    }
                }
            }
            
            Log.d(TAG, "Scanned ${pdfs.size} PDFs via MediaStore")
            
        } catch (e: Exception) {
            Log.e(TAG, "MediaStore scan failed", e)
            // Return empty list - caller should fall back to file walk
        }
        
        pdfs
    }
    
    /**
     * Check if MediaStore scanning is available and reliable.
     */
    fun isAvailable(context: Context): Boolean {
        return try {
            val uri = MediaStore.Files.getContentUri("external")
            context.contentResolver.query(uri, null, null, null, null)?.use { true } ?: false
        } catch (e: Exception) {
            false
        }
    }
}
