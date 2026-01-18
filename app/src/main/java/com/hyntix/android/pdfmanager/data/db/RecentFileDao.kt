package com.hyntix.android.pdfmanager.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.hyntix.android.pdfmanager.data.model.RecentFile
import kotlinx.coroutines.flow.Flow

@Dao
interface RecentFileDao {

    @Query("SELECT * FROM recent_files ORDER BY lastOpened DESC")
    fun getAllRecentFiles(): Flow<List<RecentFile>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecentFile(recentFile: RecentFile)

    @Query("SELECT COUNT(*) FROM recent_files")
    suspend fun getRecentFilesCount(): Int
    
    @Query("DELETE FROM recent_files WHERE uri NOT IN (SELECT uri FROM recent_files ORDER BY lastOpened DESC LIMIT 20)")
    suspend fun deleteOldestFiles()

    @Query("DELETE FROM recent_files WHERE uri = :uri")
    suspend fun deleteByUri(uri: String)

    @Query("DELETE FROM recent_files")
    suspend fun deleteAll()

    @Query("SELECT lastPage FROM recent_files WHERE uri = :uri")
    suspend fun getPageNumber(uri: String): Int?

    @Query("UPDATE recent_files SET lastPage = :page WHERE uri = :uri")
    suspend fun updatePageNumber(uri: String, page: Int): Int

    @Query("UPDATE recent_files SET lastOpened = :timestamp WHERE uri = :uri")
    suspend fun updateLastOpened(uri: String, timestamp: Long)

    @Query("SELECT * FROM recent_files WHERE uri = :uri")
    suspend fun getRecentFile(uri: String): RecentFile?
}
