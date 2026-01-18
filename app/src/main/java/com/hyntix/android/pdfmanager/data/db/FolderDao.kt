package com.hyntix.android.pdfmanager.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.hyntix.android.pdfmanager.data.model.Folder
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for folder operations.
 */
@Dao
interface FolderDao {
    
    /**
     * Get all folders ordered by name.
     */
    @Query("SELECT * FROM folders ORDER BY name ASC")
    fun getAllFolders(): Flow<List<Folder>>
    
    /**
     * Get a specific folder by ID.
     */
    @Query("SELECT * FROM folders WHERE id = :folderId")
    suspend fun getFolderById(folderId: Long): Folder?
    
    /**
     * Insert a new folder and return its generated ID.
     */
    @Insert
    suspend fun insertFolder(folder: Folder): Long
    
    /**
     * Rename an existing folder.
     */
    @Query("UPDATE folders SET name = :newName WHERE id = :folderId")
    suspend fun renameFolder(folderId: Long, newName: String)
    
    /**
     * Delete a folder by ID.
     * Associated entries in folder_pdf_cross_ref will be deleted via CASCADE.
     */
    @Query("DELETE FROM folders WHERE id = :folderId")
    suspend fun deleteFolder(folderId: Long)
    
    /**
     * Check if a folder with the given name already exists.
     */
    @Query("SELECT EXISTS(SELECT 1 FROM folders WHERE name = :name COLLATE NOCASE)")
    suspend fun folderExists(name: String): Boolean
    
    /**
     * Get folder count for UI display.
     */
    @Query("SELECT COUNT(*) FROM folders")
    suspend fun getFolderCount(): Int
}
