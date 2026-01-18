package com.hyntix.android.pdfmanager.data.model

import androidx.compose.runtime.Immutable

/**
 * Represents a PDF file or directory in the file system.
 * Marked as @Immutable to optimize Compose recompositions.
 */
@Immutable
data class PdfFile(
    val name: String,
    val path: String,
    val size: Long,
    val lastModified: Long,
    val uri: String,
    val isDirectory: Boolean = false
)
