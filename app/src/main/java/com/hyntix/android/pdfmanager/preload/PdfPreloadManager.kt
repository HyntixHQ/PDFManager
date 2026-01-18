package com.hyntix.android.pdfmanager.preload

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.hyntix.pdfium.PdfiumCore
import kotlinx.coroutines.*
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * Manages predictive preloading of PDF documents.
 * Preloads document metadata when user shows intent to open (long press, hover).
 */
class PdfPreloadManager(private val context: Context) {
    
    companion object {
        private const val TAG = "PdfPreloadManager"
        private const val PRELOAD_DELAY_MS = 150L
        private const val CACHE_VALIDITY_MS = 30_000L
    }
    
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val pdfiumCore = PdfiumCore()
    
    // Cache of preloaded documents
    private val preloadedDocs = ConcurrentHashMap<String, PreloadedDocument>()
    
    // Currently preloading job - use lock for synchronization
    private val preloadLock = Any()
    @Volatile private var currentPreloadJob: Job? = null
    @Volatile private var currentPreloadUri: String? = null
    
    /**
     * Data class holding preloaded document information.
     */
    data class PreloadedDocument(
        val uri: String,
        val pageCount: Int,
        val firstPageWidth: Int,
        val firstPageHeight: Int,
        val thumbnail: Bitmap?,
        val timestamp: Long = System.currentTimeMillis()
    )
    
    /**
     * Preload document when user shows intent (long press, hover, focus).
     * Call this when user lingers on a file item.
     */
    fun preloadOnIntent(uri: String) {
        // Don't re-preload same document
        if (uri == currentPreloadUri || preloadedDocs.containsKey(uri)) return
        
        // Synchronized access to cancel/start jobs
        synchronized(preloadLock) {
            // Cancel previous preload
            currentPreloadJob?.cancel()
            currentPreloadUri = uri
            
            currentPreloadJob = scope.launch {
                try {
                    // Small delay to avoid preloading on quick taps
                    delay(PRELOAD_DELAY_MS)
                    
                    val path = uri.removePrefix("file://")
                    val file = File(path)
                    if (!file.exists()) return@launch
                    
                    Log.d(TAG, "Preloading: $uri")
                    
                    // Use path-based document opening
                    val doc = pdfiumCore.openDocument(path, null) ?: return@launch
                    val pageCount = doc.pageCount
                    
                    // Get first page dimensions
                    val firstPage = doc.openPage(0)
                    val pageWidth = firstPage.width.toInt()
                    val pageHeight = firstPage.height.toInt()
                    
                    // Render small thumbnail
                    val thumbnailWidth = 150
                    val thumbnailHeight = (thumbnailWidth * pageHeight / pageWidth).coerceAtLeast(1)
                    
                    val thumbnail = try {
                        val bitmap = Bitmap.createBitmap(
                            thumbnailWidth, 
                            thumbnailHeight,
                            Bitmap.Config.RGB_565
                        )
                        firstPage.render(bitmap, 0, 0, thumbnailWidth, thumbnailHeight, false)
                        bitmap
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to render thumbnail", e)
                        null
                    }
                    
                    firstPage.close()
                    doc.close()
                    
                    preloadedDocs[uri] = PreloadedDocument(
                        uri = uri,
                        pageCount = pageCount,
                        firstPageWidth = pageWidth,
                        firstPageHeight = pageHeight,
                        thumbnail = thumbnail
                    )
                    
                    Log.d(TAG, "Preloaded: $uri, pages: $pageCount")
                    
                } catch (e: Exception) {
                    if (e !is CancellationException) {
                        Log.w(TAG, "Preload failed for: $uri", e)
                    }
                }
            }
        } // synchronized
    }
    
    /**
     * Get preloaded document if available and fresh.
     */
    fun getPreloaded(uri: String): PreloadedDocument? {
        val doc = preloadedDocs[uri]
        
        // Check if still fresh
        if (doc != null && System.currentTimeMillis() - doc.timestamp < CACHE_VALIDITY_MS) {
            return doc
        }
        
        // Stale or not found
        preloadedDocs.remove(uri)?.thumbnail?.recycle()
        return null
    }
    
    /**
     * Clear preloaded document after it's been used.
     */
    fun clearPreloaded(uri: String) {
        preloadedDocs.remove(uri)?.thumbnail?.recycle()
    }
    
    /**
     * Clear all preloaded documents.
     */
    fun clearAll() {
        currentPreloadJob?.cancel()
        currentPreloadUri = null
        preloadedDocs.values.forEach { it.thumbnail?.recycle() }
        preloadedDocs.clear()
    }
    
    /**
     * Dispose resources when no longer needed.
     */
    fun dispose() {
        scope.cancel()
        clearAll()
    }
}

