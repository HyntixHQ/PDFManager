package com.hyntix.android.pdfmanager.data.model

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Represents a recently opened PDF file.
 * Marked as @Immutable to optimize Compose recompositions.
 */
@Immutable
@Entity(tableName = "recent_files", indices = [Index(value = ["lastOpened"])])
data class RecentFile(
    @PrimaryKey
    val uri: String,
    val name: String,
    val lastOpened: Long,
    val lastPage: Int = 0
)
