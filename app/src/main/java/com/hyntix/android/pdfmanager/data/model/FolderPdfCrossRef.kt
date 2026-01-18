package com.hyntix.android.pdfmanager.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * Junction table for many-to-many relationship between folders and PDFs.
 * A PDF can belong to multiple folders, and a folder can contain multiple PDFs.
 */
@Entity(
    tableName = "folder_pdf_cross_ref",
    primaryKeys = ["folderId", "pdfUri"],
    foreignKeys = [
        ForeignKey(
            entity = Folder::class,
            parentColumns = ["id"],
            childColumns = ["folderId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["folderId"]),
        Index(value = ["pdfUri"])
    ]
)
data class FolderPdfCrossRef(
    val folderId: Long,
    val pdfUri: String,
    val addedAt: Long = System.currentTimeMillis()
)
