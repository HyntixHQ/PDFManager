package com.hyntix.android.pdfmanager.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.hyntix.android.pdfmanager.data.db.AppDatabase
import com.hyntix.android.pdfmanager.data.model.FavoriteFile
import com.hyntix.android.pdfmanager.data.model.RecentFile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.File

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {

    private val db = AppDatabase.getDatabase(context)
    private val recentFileDao = db.recentFileDao()
    private val favoriteFileDao = db.favoriteFileDao()
    
    companion object {
        val APP_THEME_KEY = intPreferencesKey("app_theme") // 0 = System, 1 = Light, 2 = Dark
        val KEEP_SCREEN_ON_KEY = booleanPreferencesKey("keep_screen_on")
        val GRAYSCALE_MODE_KEY = booleanPreferencesKey("grayscale_mode")
        val DEFAULT_ZOOM_MODE_KEY = intPreferencesKey("default_zoom_mode") // 0 = fit width, 1 = fit page
        val SCROLL_DIRECTION_KEY = intPreferencesKey("scroll_direction") // 0 = vertical, 1 = horizontal
        val SCROLL_MODE_KEY = intPreferencesKey("scroll_mode") // 0 = Continuous, 1 = Page By Page
        val AUTO_SCROLL_SPEED_KEY = intPreferencesKey("auto_scroll_speed") // 1 = Slow, 2 = Medium, 3 = Fast
        val FIRST_LAUNCH_KEY = booleanPreferencesKey("first_launch")
        val DEFAULT_APP_PROMPT_SHOWN_KEY = booleanPreferencesKey("default_app_prompt_shown")
        val IS_GRID_VIEW_KEY = booleanPreferencesKey("is_grid_view")
    }
    
    // ... (existing code)

    val appTheme: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[APP_THEME_KEY] ?: 0
    }

    val keepScreenOn: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEEP_SCREEN_ON_KEY] ?: false
    }

    val grayscaleMode: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[GRAYSCALE_MODE_KEY] ?: false
    }

    val scrollDirection: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[SCROLL_DIRECTION_KEY] ?: 0 // Default: vertical
    }
    
    val scrollMode: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[SCROLL_MODE_KEY] ?: 0 // Default: Continuous
    }
    
    val autoScrollSpeed: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[AUTO_SCROLL_SPEED_KEY] ?: 2 // Default: Medium
    }

    val recentFiles: Flow<List<RecentFile>> = recentFileDao.getAllRecentFiles()

    val favorites: Flow<List<String>> = favoriteFileDao.getAllFavorites().map { list -> 
        list.map { it.uri } 
    }
    

    
    val isFirstLaunch: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[FIRST_LAUNCH_KEY] ?: true
    }
    
    // ... (existing code)
    
    suspend fun setScrollDirection(direction: Int) {
        context.dataStore.edit { preferences ->
            preferences[SCROLL_DIRECTION_KEY] = direction
        }
    }

    suspend fun setAppTheme(theme: Int) {
        context.dataStore.edit { preferences ->
            preferences[APP_THEME_KEY] = theme
        }
    }

    suspend fun setKeepScreenOn(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEEP_SCREEN_ON_KEY] = enabled
        }
    }

    suspend fun setGrayscaleMode(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[GRAYSCALE_MODE_KEY] = enabled
        }
    }
    
    suspend fun setScrollMode(mode: Int) {
        context.dataStore.edit { preferences ->
            preferences[SCROLL_MODE_KEY] = mode
        }
    }
    
    suspend fun setAutoScrollSpeed(speed: Int) {
        context.dataStore.edit { preferences ->
            preferences[AUTO_SCROLL_SPEED_KEY] = speed
        }
    }
    

    
    suspend fun setFirstLaunch(isFirst: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[FIRST_LAUNCH_KEY] = isFirst
        }
    }
    
    suspend fun setDefaultAppPromptShown(shown: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[DEFAULT_APP_PROMPT_SHOWN_KEY] = shown
        }
    }
    
    suspend fun setGridView(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[IS_GRID_VIEW_KEY] = enabled
        }
    }
    
    suspend fun clearAllRecentFiles() {
        recentFileDao.deleteAll()
    }

    suspend fun isFavorite(uri: String): Boolean {
        return favoriteFileDao.isFavorite(uri)
    }

    suspend fun getLastPage(uri: String): Int? {
        return recentFileDao.getPageNumber(uri)
    }

    suspend fun toggleFavorite(uri: String, name: String, size: Long) {
        if (isFavorite(uri)) {
            removeFavorite(uri)
        } else {
             favoriteFileDao.insertFavorite(FavoriteFile(uri = uri, name = name, sizeBytes = size, addedAt = System.currentTimeMillis()))
        }
    }

    suspend fun removeFavorite(uri: String) {
        favoriteFileDao.deleteFavoriteByUri(uri)
    }

    suspend fun updateRecentFilePage(uri: String, page: Int) {
        recentFileDao.updatePageNumber(uri, page)
    }

    suspend fun addRecentFile(uri: String, name: String? = null) {
        // Check if file already exists in recents
        val existingPage = recentFileDao.getPageNumber(uri)
        if (existingPage != null) {
            recentFileDao.updateLastOpened(uri, System.currentTimeMillis())
            return
        }

        val file = File(uri)
        if (name != null) {
             // If name is provided, blindly insert (useful for content URIs or when metadata is known)
            val recent = RecentFile(
                uri = uri,
                name = name,
                lastOpened = System.currentTimeMillis()
            )
            recentFileDao.insertRecentFile(recent)
        } else if (file.exists()) {
            val recent = RecentFile(
                uri = uri,
                name = file.name,
                lastOpened = System.currentTimeMillis()
            )
            recentFileDao.insertRecentFile(recent)
        }
    }

    suspend fun removeRecentFile(uri: String) {
        recentFileDao.deleteByUri(uri)
    }

    suspend fun updateFavoritePath(oldUri: String, newUri: String) {
        val fav = favoriteFileDao.getFavorite(oldUri)
        if (fav != null) {
            favoriteFileDao.deleteFavorite(fav)
            favoriteFileDao.insertFavorite(fav.copy(uri = newUri))
        }
    }

    suspend fun updateRecentFileUri(oldUri: String, newUri: String) {
        val recent = recentFileDao.getRecentFile(oldUri)
        if (recent != null) {
            recentFileDao.deleteByUri(oldUri)
            recentFileDao.insertRecentFile(recent.copy(uri = newUri))
        }
    }
}
