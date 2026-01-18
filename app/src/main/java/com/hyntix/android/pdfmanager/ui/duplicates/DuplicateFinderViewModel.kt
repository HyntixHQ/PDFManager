package com.hyntix.android.pdfmanager.ui.duplicates

import android.os.Environment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hyntix.android.pdfmanager.data.repository.DuplicateRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

data class DuplicateFileInfo(
    val path: String,
    val name: String,
    val size: Long,
    val lastModified: Long,
    val isSelected: Boolean = false
)

data class DuplicateGroupUi(
    val hash: String,
    val files: List<DuplicateFileInfo>,
    val totalSize: Long
)

sealed class ScanState {
    object Idle : ScanState()
    object Scanning : ScanState()
    data class Complete(val groups: List<DuplicateGroupUi>, val totalDuplicateSize: Long) : ScanState()
    data class Error(val message: String) : ScanState()
}

class DuplicateFinderViewModel : ViewModel() {
    
    private val repository = DuplicateRepository()
    
    private val _scanState = MutableStateFlow<ScanState>(ScanState.Idle)
    val scanState: StateFlow<ScanState> = _scanState.asStateFlow()
    
    private val _selectedPaths = MutableStateFlow<Set<String>>(emptySet())
    val selectedPaths: StateFlow<Set<String>> = _selectedPaths.asStateFlow()
    
    private val _isDeleting = MutableStateFlow(false)
    val isDeleting: StateFlow<Boolean> = _isDeleting.asStateFlow()
    
    fun startScan() {
        viewModelScope.launch {
            _scanState.value = ScanState.Scanning
            _selectedPaths.value = emptySet()
            
            try {
                val rootPath = Environment.getExternalStorageDirectory().absolutePath
                val groups = repository.findDuplicates(rootPath)
                
                val groupsUi = groups.map { group ->
                    val files = group.paths.map { path ->
                        val file = File(path)
                        DuplicateFileInfo(
                            path = path,
                            name = file.name,
                            size = file.length(),
                            lastModified = file.lastModified()
                        )
                    }.sortedByDescending { it.lastModified } // Newest first
                    
                    DuplicateGroupUi(
                        hash = group.hash,
                        files = files,
                        totalSize = files.sumOf { it.size }
                    )
                }
                
                val totalDuplicateSize = groupsUi.sumOf { group ->
                    // Count all but one file as "duplicate" size (keep one)
                    group.files.drop(1).sumOf { it.size }
                }
                
                _scanState.value = ScanState.Complete(groupsUi, totalDuplicateSize)
            } catch (e: Exception) {
                _scanState.value = ScanState.Error(e.message ?: "Unknown error")
            }
        }
    }
    
    fun toggleSelection(path: String) {
        _selectedPaths.value = if (_selectedPaths.value.contains(path)) {
            _selectedPaths.value - path
        } else {
            _selectedPaths.value + path
        }
    }
    
    fun selectAllExceptNewest(group: DuplicateGroupUi) {
        // Select all files except the newest (first in list since sorted by date desc)
        val pathsToSelect = group.files.drop(1).map { it.path }
        _selectedPaths.value = _selectedPaths.value + pathsToSelect
    }
    
    fun selectAllExceptOldest(group: DuplicateGroupUi) {
        // Select all files except the oldest (last in list)
        val pathsToSelect = group.files.dropLast(1).map { it.path }
        _selectedPaths.value = _selectedPaths.value + pathsToSelect
    }
    
    fun clearSelection() {
        _selectedPaths.value = emptySet()
    }
    
    fun getSelectedSize(): Long {
        return repository.calculateTotalSize(_selectedPaths.value.toList())
    }
    
    fun deleteSelected(onComplete: (Int) -> Unit) {
        viewModelScope.launch {
            _isDeleting.value = true
            val count = repository.deleteFiles(_selectedPaths.value.toList())
            _selectedPaths.value = emptySet()
            _isDeleting.value = false
            
            // Re-scan after deletion
            startScan()
            onComplete(count)
        }
    }
}
