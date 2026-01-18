package com.hyntix.android.pdfmanager.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hyntix.android.pdfmanager.data.model.Folder
import com.hyntix.android.pdfmanager.data.model.PdfFile
import com.hyntix.android.pdfmanager.data.repository.FileRepository
import com.hyntix.android.pdfmanager.data.repository.FolderRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.distinctUntilChanged
import java.io.File
import com.hyntix.android.pdfmanager.preload.PdfPreloadManager
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import androidx.compose.foundation.lazy.LazyListState

class HomeViewModel(
    application: Application,
    private val repository: FileRepository,
    private val settingsRepository: com.hyntix.android.pdfmanager.data.repository.SettingsRepository,
    private val folderRepository: FolderRepository = FolderRepository(application.applicationContext)
) : AndroidViewModel(application) {

    // Preload manager for predictive loading
    private val preloadManager = PdfPreloadManager(application.applicationContext)

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchActive = MutableStateFlow(false)
    val searchActive: StateFlow<Boolean> = _searchActive.asStateFlow()

    fun setSearchActive(active: Boolean) {
        _searchActive.value = active
    }

    fun clearSearch() {
        _searchQuery.value = ""
        _searchActive.value = false
    }

    // Selected tab state - persists across navigation
    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()
    
    fun setSelectedTab(tab: Int) {
        _selectedTab.value = tab
    }

    // ============ VIRTUAL FOLDERS STATE ============
    
    /** Currently selected folder ID. null = "All" (no filter) */
    private val _selectedFolderId = MutableStateFlow<Long?>(null)
    val selectedFolderId: StateFlow<Long?> = _selectedFolderId.asStateFlow()
    
    /** Reactive flow of all folders */
    val foldersUiState: StateFlow<List<Folder>> = folderRepository.foldersFlow
        .stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.Eagerly, emptyList())
    
    /** PDF URIs in the currently selected folder (reactive) */
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    private val _folderPdfUris: StateFlow<Set<String>> = _selectedFolderId
        .flatMapLatest { folderId ->
            if (folderId == null) flowOf(emptySet())
            else folderRepository.getPdfUrisInFolder(folderId).map { it.toSet() }
        }
        .distinctUntilChanged()
        .stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.Eagerly, emptySet())
    
    /** PDFs filtered by selected folder (used when a folder is selected) */
    val folderFilteredPdfs: StateFlow<List<PdfFile>> by lazy {
        combine(
            _allPdfs,
            _selectedFolderId,
            _folderPdfUris,
            _searchQuery
        ) { allPdfs, folderId, folderUris, query ->
            val filtered = if (folderId == null) {
                allPdfs // No folder filter
            } else {
                allPdfs.filter { folderUris.contains(it.uri) }
            }
            if (query.isBlank()) filtered else filtered.filter { it.name.contains(query, ignoreCase = true) }
        }
        .distinctUntilChanged()
        .stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.Eagerly, emptyList())
    }
    
    /** Select a folder to filter PDFs. Pass null for "All". */
    fun selectFolder(folderId: Long?) {
        _selectedFolderId.value = folderId
    }
    
    /** Create a new folder with optional initial PDFs */
    fun createFolder(name: String, pdfUris: List<String> = emptyList(), onComplete: (Long) -> Unit = {}) {
        viewModelScope.launch {
            val folderId = folderRepository.createFolderWithPdfs(name, pdfUris)
            onComplete(folderId)
        }
    }
    
    /** Rename an existing folder */
    fun renameFolder(folderId: Long, newName: String) {
        viewModelScope.launch {
            folderRepository.renameFolder(folderId, newName)
        }
    }
    
    /** Delete a folder (PDFs remain on device) */
    fun deleteFolder(folderId: Long) {
        viewModelScope.launch {
            // If we're viewing this folder, switch to "All"
            if (_selectedFolderId.value == folderId) {
                _selectedFolderId.value = null
            }
            folderRepository.deleteFolder(folderId)
        }
    }
    
    /** Check if folder name already exists */
    suspend fun folderExists(name: String): Boolean {
        return folderRepository.folderExists(name)
    }
    
    /** Add PDFs to a folder */
    fun addPdfsToFolder(folderId: Long, pdfUris: List<String>) {
        viewModelScope.launch {
            folderRepository.addPdfsToFolder(folderId, pdfUris)
        }
    }
    
    /** Remove PDFs from a folder */
    fun removePdfsFromFolder(folderId: Long, pdfUris: List<String>) {
        viewModelScope.launch {
            folderRepository.removePdfsFromFolder(folderId, pdfUris)
        }
    }
    
    /** Toggle a PDF's membership in a folder */
    fun togglePdfInFolder(folderId: Long, pdfUri: String, isInFolder: Boolean) {
        viewModelScope.launch {
            folderRepository.togglePdfInFolder(folderId, pdfUri, isInFolder)
        }
    }
    
    /** Get folder IDs for a PDF (reactive) */
    fun getFolderIdsForPdf(pdfUri: String) = folderRepository.getFolderIdsForPdf(pdfUri)
    
    /** Get folder IDs for a PDF (synchronous) */
    suspend fun getFolderIdsForPdfSync(pdfUri: String) = folderRepository.getFolderIdsForPdfSync(pdfUri)
    
    // ============ END VIRTUAL FOLDERS ============

    // ============ BULK SELECTION STATE ============
    private val _selectionMode = MutableStateFlow(false)
    val selectionMode: StateFlow<Boolean> = _selectionMode.asStateFlow()

    private val _selectedUris = MutableStateFlow<Set<String>>(emptySet())
    val selectedUris: StateFlow<Set<String>> = _selectedUris.asStateFlow()

    /** Count of selected items for UI display */
    val selectedCount: StateFlow<Int> = _selectedUris
        .map { it.size }
        .stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.Lazily, 0)

    /** The currently selected file when exactly 1 is selected (for single-file actions) */
    val currentSelectedFile: StateFlow<com.hyntix.android.pdfmanager.data.model.PdfFile?> by lazy {
        kotlinx.coroutines.flow.combine(_selectedUris, _allPdfs) { uris, pdfs ->
            if (uris.size == 1) {
                pdfs.find { it.uri == uris.first() }
            } else {
                null
            }
        }.stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.Lazily, null)
    }

    /** Whether all files are currently selected */
    val allSelected: StateFlow<Boolean> by lazy {
        kotlinx.coroutines.flow.combine(_selectedUris, _allPdfs) { uris, pdfs ->
            pdfs.isNotEmpty() && uris.size == pdfs.size
        }.stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.Lazily, false)
    }

    /** Whether rename action should be shown (only for single file selection) */
    val showRenameAction: StateFlow<Boolean> = selectedCount
        .map { it == 1 }
        .stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.Lazily, true)

    fun enterSelectionMode(initialUri: String) {
        _selectionMode.value = true
        _selectedUris.value = setOf(initialUri)
    }

    fun toggleSelection(uri: String) {
        _selectedUris.update { current ->
            if (current.contains(uri)) current - uri else current + uri
        }
        // Exit selection mode if no items selected
        if (_selectedUris.value.isEmpty()) {
            _selectionMode.value = false
        }
    }

    fun selectAll() {
        _selectedUris.value = _allPdfs.value.map { it.uri }.toSet()
    }

    fun clearSelection() {
        _selectionMode.value = false
        _selectedUris.value = emptySet()
    }

    fun toggleSelectAll() {
        if (_selectedUris.value.size == _allPdfs.value.size) {
            clearSelection()
        } else {
            selectAll()
        }
    }

    fun deleteSelected() {
        viewModelScope.launch {
            val urisToDelete = _selectedUris.value.toList()
            for (uri in urisToDelete) {
                val file = _allPdfs.value.find { it.uri == uri }
                if (file != null) {
                    val success = repository.deleteFile(file.path)
                    if (success) {
                        settingsRepository.removeFavorite(uri)
                        settingsRepository.removeRecentFile(uri)
                    }
                }
            }
            clearSelection()
            refresh(showIndicator = false)
        }
    }

    /** Get selected files for multi-file operations */
    fun getSelectedFiles(): List<com.hyntix.android.pdfmanager.data.model.PdfFile> {
        return _selectedUris.value.mapNotNull { uri ->
            _allPdfs.value.find { it.uri == uri }
        }
    }
    // ============ END BULK SELECTION ============
    
    /**
     * Trigger preload when user lingers on a file item.
     * Called from UI layer on long press or hover.
     */
    fun preloadOnIntent(uri: String) {
        preloadManager.preloadOnIntent(uri)
    }
    
    /**
     * Get preloaded data if available.
     */
    fun getPreloaded(uri: String) = preloadManager.getPreloaded(uri)
    
    /**
     * Clear preloaded data after use.
     */
    fun clearPreloaded(uri: String) = preloadManager.clearPreloaded(uri)
    
    override fun onCleared() {
        super.onCleared()
        preloadManager.dispose()
    }

    private val _allPdfs = MutableStateFlow<List<PdfFile>>(emptyList())
    // Exposed as filtered UI state
    val allPdfsUiState: StateFlow<List<PdfFile>> = kotlinx.coroutines.flow.combine(_allPdfs, _searchQuery) { pdfs, query ->
        if (query.isBlank()) pdfs else pdfs.filter { it.name.contains(query, ignoreCase = true) }
    }.stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.Lazily, emptyList())

    // True Recent Logic: Reactive Stream
    val recentPdfsUiState: StateFlow<List<PdfFile>> = kotlinx.coroutines.flow.combine(
        _allPdfs,
        settingsRepository.recentFiles,
        _searchQuery
    ) { allPdfs, recentFiles, query ->
        val pdfMap = allPdfs.associateBy { it.uri } // Use URI for matching
        val recents = recentFiles.mapNotNull { recentFile ->
            // recentFile.uri is the URI from DB (file:// or content://)
            val existingPdf = pdfMap[recentFile.uri]
            if (existingPdf != null) {
                // Use the scanned file but override lastModified with lastOpened from DB
                existingPdf.copy(lastModified = recentFile.lastOpened)
            } else {
                val uriStr = recentFile.uri
                if (uriStr.startsWith("file://")) {
                    // Extract path
                    val path = uriStr.substring(7)
                    val file = File(path)
                    if (file.exists()) {
                         PdfFile(
                            name = recentFile.name,
                            path = path,
                            size = file.length(),
                            lastModified = recentFile.lastOpened,
                            uri = uriStr
                        )
                    } else null
                } else {
                    // Content URI or other - blindly add using DB metadata
                     PdfFile(
                        name = recentFile.name,
                        path = uriStr, // Path is meaningless for content URI, use URI
                        size = 0, // Unknown size unless stored in DB (we don't store size in RecentFile yet)
                        lastModified = recentFile.lastOpened,
                        uri = uriStr
                    )
                }
            }
        }
        if (query.isBlank()) recents else recents.filter { it.name.contains(query, ignoreCase = true) }
    }.stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.Lazily, emptyList())

    // Favorites Logic: Reactive Stream
    val favoritePdfsUiState: StateFlow<List<PdfFile>> = kotlinx.coroutines.flow.combine(
        _allPdfs,
        settingsRepository.favorites,
        _searchQuery
    ) { allPdfs, favPaths, query ->
        val favorites = allPdfs.filter { favPaths.contains(it.uri) } // Match by URI
        if (query.isBlank()) favorites else favorites.filter { it.name.contains(query, ignoreCase = true) }
    }.stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.Lazily, emptyList())

    // File Browser State
    private val _currentDirectory = MutableStateFlow<File>(android.os.Environment.getExternalStorageDirectory())
    val currentDirectory: StateFlow<File> = _currentDirectory.asStateFlow()
    
    private val _filesInDirectory = MutableStateFlow<List<PdfFile>>(emptyList())
    val filesInDirectoryUiState: StateFlow<List<PdfFile>> = kotlinx.coroutines.flow.combine(_filesInDirectory, _searchQuery) { files, query ->
        if (query.isBlank()) files else files.filter { it.name.contains(query, ignoreCase = true) }
    }.stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.Lazily, emptyList())
    
    // UI State
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private var refreshJob: kotlinx.coroutines.Job? = null

    // Track permission request state in ViewModel to survive configuration changes
    var isPermissionRequestInProgress = false

    // Persistent scroll states for each tab
    val allPdfsListState = LazyListState()
    val recentPdfsListState = LazyListState()
    val favoritePdfsListState = LazyListState()
    val filesListState = LazyListState()

    init {
        // Initial load is silent (shimmer handles it)
        refresh(showIndicator = false)
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }
    
    fun addToRecent(file: PdfFile) {
        viewModelScope.launch {
            settingsRepository.addRecentFile(file.uri, file.name)
        }
    }

    fun removeFromRecent(uri: String) {
        viewModelScope.launch {
            settingsRepository.removeRecentFile(uri)
        }
    }

    enum class SortOption {
        NAME, DATE, SIZE
    }



    // One-time events for UI
    private val _scrollToTopChannel = Channel<Unit>(Channel.BUFFERED)
    val scrollToTopEvent = _scrollToTopChannel.receiveAsFlow()

    fun sortFiles(option: SortOption) {
        val sortedList = when (option) {
            SortOption.NAME -> _allPdfs.value.sortedBy { it.name.lowercase() }
            SortOption.DATE -> _allPdfs.value.sortedByDescending { it.lastModified }
            SortOption.SIZE -> _allPdfs.value.sortedByDescending { it.size }
        }
        _allPdfs.value = sortedList
        // Trigger scroll to top
        viewModelScope.launch {
            _scrollToTopChannel.send(Unit)
        }
    }

    fun loadAllPdfs() {
        viewModelScope.launch {
            if (_allPdfs.value.isEmpty()) {
                _isLoading.value = true
            }
            try {
                _allPdfs.value = repository.getAllPdfs().sortedByDescending { it.lastModified }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun loadCurrentDirectory() {
        viewModelScope.launch {
            try {
                _filesInDirectory.value = repository.getFiles(_currentDirectory.value.absolutePath)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            // ...
        }
    }
    
    fun navigateTo(directory: File) {
        if (directory.isDirectory) {
            _currentDirectory.value = directory
            loadCurrentDirectory()
        }
    }
    
    fun navigateUp(): Boolean {
        val parent = _currentDirectory.value.parentFile
        if (parent != null && parent.canRead() && parent.absolutePath.startsWith(android.os.Environment.getExternalStorageDirectory().absolutePath)) {
            _currentDirectory.value = parent
            loadCurrentDirectory()
            return true
        }
        return false // At root or cannot go up
    }
    
    // Unified refresh that waits for all data
    fun refresh(showIndicator: Boolean = true, forceLoad: Boolean = false) {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            if (forceLoad) {
                _isLoading.value = true
                _allPdfs.value = emptyList() // Clear list to force UI reset if desired, or just show skeleton overlay
            } else if (showIndicator) {
                _isRefreshing.value = true
            } else if (_allPdfs.value.isEmpty()) {
                _isLoading.value = true
            }

            try {
                // Use async to fetch both in parallel
                // Pass forceRefresh=true when user explicitly refreshes (showIndicator=true)
                // to ensure cache is updated
                val allPdfsDeferred = async { 
                    repository.getAllPdfs(forceRefresh = showIndicator || forceLoad)
                        .sortedByDescending { it.lastModified }
                }
                val filesDeferred = async { 
                    repository.getFiles(_currentDirectory.value.absolutePath)
                }
                
                // Wait for data
                val allPdfs = allPdfsDeferred.await()
                val files = filesDeferred.await()
                
                // Update StateFlows
                _allPdfs.value = allPdfs
                _filesInDirectory.value = files
                
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isRefreshing.value = false
                _isLoading.value = false
            }
        }
    }

    fun toggleFavorite(file: PdfFile) {
        viewModelScope.launch {
            settingsRepository.toggleFavorite(file.uri, file.name, file.size)
        }
    }

    fun renameFile(file: PdfFile, newName: String) {
        val oldPath = file.path
        val newPath = File(File(oldPath).parent, newName).absolutePath
        val oldUri = file.uri
        val newUri = if (oldUri.startsWith("file://")) "file://$newPath" else oldUri // Only update URI if file scheme

        viewModelScope.launch {
             val success = repository.renameFile(oldPath, newName)
             if (success) {
                 settingsRepository.updateFavoritePath(oldUri, newUri)
                 // Also update recent if needed? DB doesn't support generic update path yet, but we should probably remove old and add new?
                 // For now, refreshing handles list, but DB entry becomes stale.
                 // Ideally: settingsRepository.updateRecentPath(oldUri, newUri)
                 settingsRepository.updateRecentFileUri(oldUri, newUri)
                 refresh(showIndicator = false)
             } else {
                 refresh(showIndicator = false)
             }
        }
    }

    fun deleteFile(file: PdfFile) {
        val pathToDelete = file.path
        val uriToDelete = file.uri
        viewModelScope.launch {
            val success = repository.deleteFile(pathToDelete)
            if (success) {
                settingsRepository.removeFavorite(uriToDelete)
                settingsRepository.removeRecentFile(uriToDelete)
                refresh(showIndicator = false)
            } else {
                refresh(showIndicator = false)
            }
        }
    }

    // Removed updateListsSilently as it is better to just refresh from source for consistency

}
