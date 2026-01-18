package com.hyntix.android.pdfmanager.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.hyntix.android.pdfmanager.data.model.FavoriteFile
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteFileDao {

    @Query("SELECT * FROM favorite_files ORDER BY addedAt DESC")
    fun getAllFavorites(): Flow<List<FavoriteFile>>

    @Query("SELECT * FROM favorite_files WHERE uri = :uri LIMIT 1")
    suspend fun getFavorite(uri: String): FavoriteFile?

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_files WHERE uri = :uri)")
    suspend fun isFavorite(uri: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(favoriteFile: FavoriteFile)

    @Delete
    suspend fun deleteFavorite(favoriteFile: FavoriteFile)

    @Query("DELETE FROM favorite_files WHERE uri = :uri")
    suspend fun deleteFavoriteByUri(uri: String)

    @Query("DELETE FROM favorite_files")
    suspend fun deleteAllFavorites()
}
