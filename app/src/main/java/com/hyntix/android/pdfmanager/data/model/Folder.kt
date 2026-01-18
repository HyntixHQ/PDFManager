package com.hyntix.android.pdfmanager.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a virtual folder for organizing PDFs.
 * These folders exist only in the database, not on the filesystem.
 */
@Entity(tableName = "folders")
data class Folder(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val createdAt: Long = System.currentTimeMillis()
)
