package com.hyntix.android.pdfmanager.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.SavedStateHandle
import com.hyntix.android.pdfmanager.data.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

data class SettingsUiState(
    val appTheme: Int = 0,
    val keepScreenOn: Boolean = false,
    val grayscaleMode: Boolean = false,
    val scrollDirection: Int = 0, // 0 = Vertical, 1 = Horizontal
    val scrollMode: Int = 0 // 0 = Continuous, 1 = Page By Page
)

class SettingsViewModel(
    application: Application,
    private val savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {
    var scrollPosition: Int = 0

    private val repository = SettingsRepository(application)
    
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()
    
    init {
        viewModelScope.launch {
            repository.appTheme.collect { theme ->
                _uiState.update { it.copy(appTheme = theme) }
            }
        }
        
        viewModelScope.launch {
            repository.keepScreenOn.collect { keepOn ->
                _uiState.update { it.copy(keepScreenOn = keepOn) }
            }
        }
        
        viewModelScope.launch {
            repository.grayscaleMode.collect { mode ->
                _uiState.update { it.copy(grayscaleMode = mode) }
            }
        }
        
        viewModelScope.launch {
            repository.scrollDirection.collect { direction ->
                _uiState.update { it.copy(scrollDirection = direction) }
            }
        }
        
        viewModelScope.launch {
            repository.scrollMode.collect { mode ->
                _uiState.update { it.copy(scrollMode = mode) }
            }
        }
    }
    
    fun setAppTheme(theme: Int) {
        viewModelScope.launch {
            repository.setAppTheme(theme)
        }
    }
    
    fun toggleKeepScreenOn(enabled: Boolean) {
        viewModelScope.launch {
            repository.setKeepScreenOn(enabled)
        }
    }
    
    fun toggleGrayscaleMode(enabled: Boolean) {
        viewModelScope.launch {
            repository.setGrayscaleMode(enabled)
        }
    }
    
    fun setScrollDirection(direction: Int) {
        viewModelScope.launch {
            repository.setScrollDirection(direction)
        }
    }
    
    fun setScrollMode(mode: Int) {
        viewModelScope.launch {
            repository.setScrollMode(mode)
        }
    }
    
    fun clearRecentFiles() {
        viewModelScope.launch {
            repository.clearAllRecentFiles()
        }
    }
    
    fun clearCache() {
        viewModelScope.launch {
            val cacheDir = getApplication<Application>().cacheDir
            deleteDir(cacheDir)
        }
    }
    
    private fun deleteDir(dir: File?): Boolean {
        if (dir != null && dir.isDirectory) {
            val children = dir.list()
            if (children != null) {
                for (i in children.indices) {
                    val success = deleteDir(File(dir, children[i]))
                    if (!success) {
                        return false
                    }
                }
            }
            return dir.delete()
        } else return dir != null && dir.isFile && dir.delete()
    }
}
