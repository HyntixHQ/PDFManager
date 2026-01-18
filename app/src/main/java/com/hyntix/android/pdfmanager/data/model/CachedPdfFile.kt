package com.hyntix.android.pdfmanager.data.model

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Represents a cached PDF file discovered during filesystem scan.
 * Used to provide instant loading on subsequent app launches.
 * Marked as @Immutable to optimize Compose recompositions.
 */
@Immutable
@Entity(
    tableName = "cached_pdf_files",
    indices = [Index(value = ["lastModified"]), Index(value = ["lastScanned"])]
)
data class CachedPdfFile(
    @PrimaryKey
    val path: String,
    val name: String,
    val size: Long,
    val lastModified: Long,
    val uri: String,
    val lastScanned: Long = System.currentTimeMillis()
) {
    /**
     * Convert to PdfFile for UI display.
     */
    fun toPdfFile(): PdfFile = PdfFile(
        name = name,
        path = path,
        size = size,
        lastModified = lastModified,
        uri = uri,
        isDirectory = false
    )
}

/**
 * Extension function to convert PdfFile to CachedPdfFile for caching.
 */
fun PdfFile.toCachedPdfFile(): CachedPdfFile = CachedPdfFile(
    path = path,
    name = name,
    size = size,
    lastModified = lastModified,
    uri = uri
)
