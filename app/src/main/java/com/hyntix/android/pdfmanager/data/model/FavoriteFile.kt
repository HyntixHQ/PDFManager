package com.hyntix.android.pdfmanager.data.model

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Represents a favorited PDF file.
 * This is a separate table from RecentFiles for clean data separation.
 * Marked as @Immutable to optimize Compose recompositions.
 */
@Immutable
@Entity(tableName = "favorite_files", indices = [Index(value = ["addedAt"])])
data class FavoriteFile(
    @PrimaryKey
    val uri: String,
    val name: String,
    val sizeBytes: Long = 0,
    val addedAt: Long = System.currentTimeMillis()
)
