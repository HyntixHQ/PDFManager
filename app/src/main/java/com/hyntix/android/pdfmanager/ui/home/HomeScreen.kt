@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.hyntix.android.pdfmanager.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.ui.unit.dp
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.Bold
import com.adamglin.phosphoricons.bold.*
import com.adamglin.phosphoricons.Fill
import com.adamglin.phosphoricons.fill.*
import com.adamglin.phosphoricons.regular.*
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.ui.composed
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.foundation.rememberScrollState
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collect
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: com.hyntix.android.pdfmanager.ui.home.HomeViewModel,
    onSettingsClick: () -> Unit,
    onPdfSelected: (String) -> Unit,
    onDuplicateFinderClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val selectedTab by viewModel.selectedTab.collectAsState()
    val searchActive by viewModel.searchActive.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()  // Use ViewModel state
    val focusRequester = remember { FocusRequester() }

    val allPdfs by viewModel.allPdfsUiState.collectAsState()
    val recentPdfs by viewModel.recentPdfsUiState.collectAsState()
    val favoritePdfs by viewModel.favoritePdfsUiState.collectAsState()
    val filesInDirectory by viewModel.filesInDirectoryUiState.collectAsState()
    val currentDirectory by viewModel.currentDirectory.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    
    // Bulk Selection State
    val selectionMode by viewModel.selectionMode.collectAsState()
    val selectedUris by viewModel.selectedUris.collectAsState()
    val selectedCount by viewModel.selectedCount.collectAsState()
    val currentSelectedFile by viewModel.currentSelectedFile.collectAsState()
    val allSelected by viewModel.allSelected.collectAsState()
    val showRenameAction by viewModel.showRenameAction.collectAsState()
    
    // Use stable states from ViewModel to ensure persistence across navigation
    val allPdfsListState = viewModel.allPdfsListState
    val recentPdfsListState = viewModel.recentPdfsListState
    val favoritePdfsListState = viewModel.favoritePdfsListState
    val filesListState = viewModel.filesListState

    // Handle scroll to top event
    LaunchedEffect(Unit) {
        viewModel.scrollToTopEvent.collect {
            kotlinx.coroutines.delay(100) // Ensure list update is processed
            allPdfsListState.scrollToItem(0)
        }
    }

    var fileToRename by remember { mutableStateOf<com.hyntix.android.pdfmanager.data.model.PdfFile?>(null) }
    var fileToDelete by remember { mutableStateOf<com.hyntix.android.pdfmanager.data.model.PdfFile?>(null) }
    var fileToShowInfo by remember { mutableStateOf<com.hyntix.android.pdfmanager.data.model.PdfFile?>(null) }
    var showBulkDeleteConfirm by remember { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current
    
    // ============ FOLDER STATE ============
    val folders by viewModel.foldersUiState.collectAsState()
    val selectedFolderId by viewModel.selectedFolderId.collectAsState()
    val folderFilteredPdfs by viewModel.folderFilteredPdfs.collectAsState()
    
    // Folder UI state
    var showCreateFolderSheet by remember { mutableStateOf(false) }
    var showPdfSelectionScreen by remember { mutableStateOf(false) }
    var pendingFolderName by remember { mutableStateOf("") }
    var folderToManage by remember { mutableStateOf<com.hyntix.android.pdfmanager.data.model.Folder?>(null) }
    var showRenameFolderDialog by remember { mutableStateOf(false) }
    var showDeleteFolderConfirm by remember { mutableStateOf(false) }
    var showAddPdfsToFolder by remember { mutableStateOf(false) }
    var showRemovePdfsFromFolder by remember { mutableStateOf(false) }
    var fileToAddToFolder by remember { mutableStateOf<com.hyntix.android.pdfmanager.data.model.PdfFile?>(null) }
    var folderIdsForFile by remember { mutableStateOf<List<Long>>(emptyList()) }
    val scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main)
    // ============ END FOLDER STATE ============

    // Handle back press to exit selection mode first
    BackHandler(enabled = selectionMode) {
        viewModel.clearSelection()
    }

    // Handle back press for folder navigation
    androidx.activity.compose.BackHandler(enabled = !selectionMode && selectedTab == 3 && currentDirectory.path != android.os.Environment.getExternalStorageDirectory().path) {
        viewModel.navigateUp()
    }

    Scaffold(
        topBar = {
            Column {
                if (searchActive) {
                    BackHandler {
                         viewModel.clearSearch()
                    }
                    LaunchedEffect(Unit) {
                        focusRequester.requestFocus()
                    }
                    
                    TopAppBar(
                        title = {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    contentAlignment = Alignment.CenterStart,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    if (searchQuery.isEmpty()) {
                                        Text(
                                            text = "Search PDFs...",
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    androidx.compose.foundation.text.BasicTextField(
                                        value = searchQuery,
                                        onValueChange = { viewModel.onSearchQueryChanged(it) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .focusRequester(focusRequester),
                                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                                            color = MaterialTheme.colorScheme.onSurface
                                        ),
                                        singleLine = true,
                                        cursorBrush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary)
                                    )
                                }
                                
                                if (searchQuery.isNotEmpty()) {
                                    TooltipBox(
                                        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
                                        tooltip = { PlainTooltip { Text("Clear search query") } },
                                        state = rememberTooltipState()
                                    ) {
                                        IconButton(
                                            onClick = { viewModel.onSearchQueryChanged("") }
                                        ) {
                                            Icon(PhosphorIcons.Regular.X, "Clear")
                                        }
                                    }
                                }
                            }
                        },
                        navigationIcon = {
                            TooltipBox(
                                positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
                                tooltip = { PlainTooltip { Text("Close search") } },
                                state = rememberTooltipState()
                            ) {
                                 IconButton(onClick = { 
                                     viewModel.clearSearch()
                                 }) {
                                     Icon(PhosphorIcons.Regular.ArrowLeft, "Back")
                                 }
                            }
                        },
                        actions = {
                            // Empty actions as clear button is now in title
                        }
                    )
                } else if (selectionMode) {
                    // ======== SELECTION MODE CONTEXTUAL TOP BAR ========
                    TopAppBar(
                        title = { Text("$selectedCount Selected") },
                        actions = {
                            TooltipBox(
                                positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
                                tooltip = { PlainTooltip { Text(if (allSelected) "Deselect all files" else "Select all files") } },
                                state = rememberTooltipState()
                            ) {
                                IconButton(onClick = { viewModel.toggleSelectAll() }) {
                                    Icon(
                                        if (allSelected) PhosphorIcons.Regular.MinusSquare else PhosphorIcons.Regular.CheckSquare, 
                                        if (allSelected) "Deselect All" else "Select All"
                                    )
                                }
                            }
                        }
                    )
                } else {
                    TopAppBar(
                        title = { 
                            val title = when (selectedTab) {
                                0 -> "PDF Manager"
                                1 -> "Recent"
                                2 -> "Favorites"
                                3 -> if (currentDirectory.name == "0") "Internal Storage" else currentDirectory.name
                                else -> "PDF Manager"
                            }
                            Text(title)
                        },
                        navigationIcon = {
                             if (selectedTab == 3 && currentDirectory.path != android.os.Environment.getExternalStorageDirectory().path) {
                                 TooltipBox(
                                     positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
                                     tooltip = { PlainTooltip { Text("Go back to parent folder") } },
                                     state = rememberTooltipState()
                                 ) {
                                     IconButton(onClick = { viewModel.navigateUp() }) {
                                         Icon(PhosphorIcons.Regular.ArrowLeft, "Back")
                                     }
                                 }
                             }
                        },
                        actions = {
                            if (selectedTab == 0) {
                                TooltipBox(
                                    positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
                                    tooltip = { PlainTooltip { Text("Search PDFs") } },
                                    state = rememberTooltipState()
                                ) {
                                    IconButton(onClick = { viewModel.setSearchActive(true) }) {
                                        Icon(
                                            imageVector = PhosphorIcons.Regular.MagnifyingGlass,
                                            contentDescription = "Search"
                                        )
                                    }
                                }
                            }
                            TooltipBox(
                                positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
                                tooltip = { PlainTooltip { Text("Find duplicate PDFs") } },
                                state = rememberTooltipState()
                            ) {
                                IconButton(onClick = onDuplicateFinderClick) {
                                    Icon(
                                        imageVector = PhosphorIcons.Regular.Files,
                                        contentDescription = "Find Duplicates"
                                    )
                                }
                            }
    
                            TooltipBox(
                                positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
                                tooltip = { PlainTooltip { Text("Settings") } },
                                state = rememberTooltipState()
                            ) {
                                IconButton(onClick = onSettingsClick) {
                                    Icon(
                                        imageVector = PhosphorIcons.Regular.Gear,
                                        contentDescription = "Settings"
                                    )
                                }
                            }
                        }
                    )
                }
                
                HorizontalDivider(
                    thickness = 1.dp, 
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
                )
            }
        },
        bottomBar = {
            // Hide navigation bar during selection mode
            if (!selectionMode) {
                // ======== NORMAL NAVIGATION BAR ========
                androidx.compose.material3.Surface(
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Column {
                        HorizontalDivider(
                            thickness = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .windowInsetsPadding(WindowInsets.navigationBars)
                                .height(56.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                        TooltipBox(
                            positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
                            tooltip = { PlainTooltip { Text("Home") } },
                            state = rememberTooltipState(),
                            modifier = Modifier.weight(1f)
                        ) {
                            IconButton(
                                onClick = { viewModel.setSelectedTab(0) }
                            ) {
                                Icon(
                                    imageVector = if (selectedTab == 0) PhosphorIcons.Fill.House else PhosphorIcons.Regular.House,
                                    contentDescription = "Home",
                                    tint = if (selectedTab == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                        
                        TooltipBox(
                            positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
                            tooltip = { PlainTooltip { Text("Recent files") } },
                            state = rememberTooltipState(),
                            modifier = Modifier.weight(1f)
                        ) {
                            IconButton(
                                onClick = { viewModel.setSelectedTab(1) }
                            ) {
                                Icon(
                                    imageVector = if (selectedTab == 1) PhosphorIcons.Fill.Clock else PhosphorIcons.Regular.Clock,
                                    contentDescription = "Recent",
                                    tint = if (selectedTab == 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                        
                        TooltipBox(
                            positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
                            tooltip = { PlainTooltip { Text("Favorite files") } },
                            state = rememberTooltipState(),
                            modifier = Modifier.weight(1f)
                        ) {
                            IconButton(
                                onClick = { viewModel.setSelectedTab(2) }
                            ) {
                                Icon(
                                    imageVector = if (selectedTab == 2) PhosphorIcons.Fill.Star else PhosphorIcons.Regular.Star,
                                    contentDescription = "Favorites",
                                    tint = if (selectedTab == 2) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                        
                        TooltipBox(
                            positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
                            tooltip = { PlainTooltip { Text("Browse directories") } },
                            state = rememberTooltipState(),
                            modifier = Modifier.weight(1f)
                        ) {
                            IconButton(
                                onClick = { viewModel.setSelectedTab(3) }
                            ) {
                                Icon(
                                    imageVector = if (selectedTab == 3) PhosphorIcons.Fill.Folder else PhosphorIcons.Regular.Folder,
                                    contentDescription = "Files",
                                    tint = if (selectedTab == 3) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                        }
                    }
                }
            }
        },
        modifier = modifier
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (isLoading) {
                  LoadingSkeletonList()
            } else {
                when (selectedTab) {
                    0 -> {
                        // Home tab with folder chips
                        Column(modifier = Modifier.fillMaxSize()) {
                            // Folder filter chips row
                            if (!selectionMode && !searchActive) {
                                FolderChipsRow(
                                    folders = folders,
                                    selectedFolderId = selectedFolderId,
                                    onSelectFolder = { viewModel.selectFolder(it) },
                                    onAddFolderClick = { showCreateFolderSheet = true },
                                    onFolderLongPress = { folder -> folderToManage = folder },
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            }
                            
                            // Use folder-filtered PDFs when a folder is selected
                            val displayPdfs = if (selectedFolderId != null) folderFilteredPdfs else allPdfs
                            
                            Box(modifier = Modifier.weight(1f)) {
                                AllPdfsTab(
                                    pdfs = displayPdfs,
                                    isRefreshing = isRefreshing,
                                    onRefresh = { viewModel.refresh() },
                                    onPdfSelected = { uri ->
                                        if (selectionMode) {
                                            if (selectedCount == 1 && selectedUris.contains(uri)) {
                                                viewModel.clearSelection()
                                            } else {
                                                viewModel.toggleSelection(uri)
                                            }
                                        } else {
                                            onPdfSelected(uri)
                                        }
                                    },
                                    onLongPress = { pdf ->
                                        if (selectionMode) {
                                            viewModel.toggleSelection(pdf.uri)
                                        } else {
                                            viewModel.enterSelectionMode(pdf.uri)
                                        }
                                    },
                                    onDelete = { fileToDelete = it },
                                    selectedPath = currentSelectedFile?.path,
                                    selectionMode = selectionMode,
                                    selectedUris = selectedUris,
                                    listState = allPdfsListState
                                )
                                
                                // Small FAB for folder management (visible when folder is selected)
                                if (selectedFolderId != null && !selectionMode) {
                                    val currentFolder = folders.find { it.id == selectedFolderId }
                                    if (currentFolder != null) {
                                        androidx.compose.material3.SmallFloatingActionButton(
                                            onClick = { folderToManage = currentFolder },
                                            modifier = Modifier
                                                .align(Alignment.BottomEnd)
                                                .padding(16.dp),
                                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                        ) {
                                            Icon(
                                                imageVector = PhosphorIcons.Regular.PencilSimple,
                                                contentDescription = "Manage folder"
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    1 -> RecentFilesTab(
                        pdfs = recentPdfs,
                        isRefreshing = isRefreshing,
                        onRefresh = { viewModel.refresh() },
                        onPdfSelected = { onPdfSelected(it) },
                        onClearClick = { uri -> viewModel.removeFromRecent(uri) },
                        listState = recentPdfsListState
                    )
                    2 -> FavoritesTab(
                        pdfs = favoritePdfs,
                        isRefreshing = isRefreshing,
                        onRefresh = { viewModel.refresh() },
                        onPdfSelected = { onPdfSelected(it) },
                        onRemove = { viewModel.toggleFavorite(it) },
                        listState = favoritePdfsListState
                    )
                    3 -> FilesTab(
                        files = filesInDirectory,
                        onFileClick = { file ->
                             if (file.isDirectory) {
                                 viewModel.navigateTo(java.io.File(file.path))
                             } else {
                                 if (selectionMode) {
                                     viewModel.toggleSelection(file.uri)
                                 } else {
                                     onPdfSelected(file.uri)
                                 }
                             }
                        },
                        onNavigateUp = { viewModel.navigateUp() },
                        onLongPress = { pdf ->
                            if (selectionMode) {
                                viewModel.toggleSelection(pdf.uri)
                            } else {
                                viewModel.enterSelectionMode(pdf.uri)
                            }
                        },
                        onDelete = { fileToDelete = it },
                        selectedPath = currentSelectedFile?.path,
                        selectionMode = selectionMode,
                        selectedUris = selectedUris,
                        listState = filesListState
                    )
                }
                
                // File Action Panel (shown when in selection mode)
                FileActionPanel(
                    visible = selectionMode,
                    onShare = {
                        val filesToShare = viewModel.getSelectedFiles()
                        if (filesToShare.size == 1) {
                            filesToShare.firstOrNull()?.let { file ->
                                try {
                                    val fileUri = androidx.core.content.FileProvider.getUriForFile(
                                        context,
                                        "${context.packageName}.fileprovider",
                                        java.io.File(file.path)
                                    )
                                    val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                        type = "application/pdf"
                                        putExtra(android.content.Intent.EXTRA_STREAM, fileUri)
                                        addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(android.content.Intent.createChooser(shareIntent, "Share PDF"))
                                } catch (e: Exception) {
                                    android.widget.Toast.makeText(context, "Share failed: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            }
                        } else {
                            // Multi-file share
                            val urisToShare = filesToShare.map { file ->
                                androidx.core.content.FileProvider.getUriForFile(
                                    context,
                                    "${context.packageName}.fileprovider",
                                    java.io.File(file.path)
                                )
                            }
                            if (urisToShare.isNotEmpty()) {
                                val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND_MULTIPLE).apply {
                                    type = "application/pdf"
                                    putParcelableArrayListExtra(android.content.Intent.EXTRA_STREAM, ArrayList(urisToShare))
                                    addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(android.content.Intent.createChooser(shareIntent, "Share PDFs"))
                            }
                        }
                        viewModel.clearSelection()
                    },
                    onRename = {
                        fileToRename = currentSelectedFile
                        viewModel.clearSelection()
                    },
                    onAddToFolder = {
                        // For single file, use the add-to-folder sheet
                        // For multi-file, we'll add all selected to folders
                        if (selectedCount == 1) {
                            currentSelectedFile?.let { file ->
                                fileToAddToFolder = file
                            }
                        } else {
                            // For multi-select, we need a different approach
                            // For now, just show toast - can enhance later
                            val files = viewModel.getSelectedFiles()
                            if (files.isNotEmpty()) {
                                fileToAddToFolder = files.first()
                            }
                        }
                        viewModel.clearSelection()
                    },
                    onDelete = {
                        if (selectedCount == 1) {
                            fileToDelete = currentSelectedFile
                            viewModel.clearSelection()
                        } else {
                            showBulkDeleteConfirm = true
                        }
                    },
                    onDismiss = { viewModel.clearSelection() },
                    showRename = showRenameAction
                )
            }
        }
    }
    
    // Rename Dialog
    if (fileToRename != null) {
        var newName by remember { mutableStateOf(fileToRename!!.name) }
        AlertDialog(
            onDismissRequest = { fileToRename = null },
            title = { Text("Rename File") },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("New Name") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.renameFile(fileToRename!!, newName)
                        fileToRename = null
                    }
                ) {
                    Text("Rename")
                }
            },
            dismissButton = {
                TextButton(onClick = { fileToRename = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Delete Dialog
    if (fileToDelete != null) {
        AlertDialog(
            onDismissRequest = { fileToDelete = null },
            title = { Text("Delete File") },
            text = { Text("Are you sure you want to delete '${fileToDelete!!.name}'? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteFile(fileToDelete!!)
                        fileToDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { fileToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Bulk Delete Confirmation Dialog
    if (showBulkDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showBulkDeleteConfirm = false },
            title = { Text("Delete $selectedCount Files?") },
            text = { Text("Are you sure you want to delete $selectedCount files? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteSelected()
                        showBulkDeleteConfirm = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBulkDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // File Info Bottom Sheet
    if (fileToShowInfo != null) {
        FileInfoBottomSheet(
            file = fileToShowInfo!!,
            onDismiss = { fileToShowInfo = null }
        )
    }
    
    // ============ FOLDER DIALOGS ============
    
    // Create Folder Sheet (Step 1: Enter name)
    if (showCreateFolderSheet) {
        CreateFolderSheet(
            onDismiss = { showCreateFolderSheet = false },
            onCreateFolder = { name ->
                pendingFolderName = name
                showCreateFolderSheet = false
                showPdfSelectionScreen = true
            }
        )
    }
    
    // PDF Selection Screen (Step 2: Select PDFs for new folder)
    if (showPdfSelectionScreen) {
        PdfSelectionScreen(
            title = "Add to \"$pendingFolderName\"",
            allPdfs = allPdfs,
            preSelectedUris = emptySet(),
            onDone = { selectedPdfUris ->
                viewModel.createFolder(pendingFolderName, selectedPdfUris.toList()) { folderId ->
                    // Auto-select the new folder
                    viewModel.selectFolder(folderId)
                }
                showPdfSelectionScreen = false
                pendingFolderName = ""
            },
            onCancel = {
                showPdfSelectionScreen = false
                pendingFolderName = ""
            }
        )
    }
    
    // Folder Management Sheet (only show when no sub-action is active)
    if (folderToManage != null && 
        !showRenameFolderDialog && 
        !showDeleteFolderConfirm && 
        !showAddPdfsToFolder && 
        !showRemovePdfsFromFolder) {
        FolderManagementSheet(
            folder = folderToManage!!,
            onDismiss = { folderToManage = null },
            onRename = { showRenameFolderDialog = true },
            onAddPdfs = { showAddPdfsToFolder = true },
            onRemovePdfs = { showRemovePdfsFromFolder = true },
            onDelete = { showDeleteFolderConfirm = true }
        )
    }
    
    // Rename Folder Dialog
    if (showRenameFolderDialog && folderToManage != null) {
        var newName by remember { mutableStateOf(folderToManage!!.name) }
        AlertDialog(
            onDismissRequest = { 
                showRenameFolderDialog = false
                folderToManage = null
            },
            title = { Text("Rename Folder") },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("Folder name") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newName.isNotBlank()) {
                            viewModel.renameFolder(folderToManage!!.id, newName.trim())
                        }
                        showRenameFolderDialog = false
                        folderToManage = null
                    }
                ) {
                    Text("Rename")
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showRenameFolderDialog = false
                    folderToManage = null
                }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    // Delete Folder Confirmation Dialog
    if (showDeleteFolderConfirm && folderToManage != null) {
        AlertDialog(
            onDismissRequest = { 
                showDeleteFolderConfirm = false
                folderToManage = null
            },
            title = { Text("Delete Folder?") },
            text = { 
                Text("Delete folder \"${folderToManage!!.name}\"? The PDFs will not be deleted from your device.") 
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteFolder(folderToManage!!.id)
                        showDeleteFolderConfirm = false
                        folderToManage = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showDeleteFolderConfirm = false
                    folderToManage = null
                }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    // Add PDFs to Folder Screen
    if (showAddPdfsToFolder && folderToManage != null) {
        val currentFolder = folderToManage!!
        val pdfUrisInFolder by viewModel.folderFilteredPdfs.collectAsState()
        val existingUris = remember(pdfUrisInFolder) { pdfUrisInFolder.map { it.uri }.toSet() }
        
        PdfSelectionScreen(
            title = "Add to \"${currentFolder.name}\"",
            allPdfs = allPdfs,
            preSelectedUris = existingUris,
            onDone = { selectedUris ->
                // Add only the new ones
                val newUris = selectedUris - existingUris
                if (newUris.isNotEmpty()) {
                    viewModel.addPdfsToFolder(currentFolder.id, newUris.toList())
                }
                showAddPdfsToFolder = false
                folderToManage = null
            },
            onCancel = {
                showAddPdfsToFolder = false
                folderToManage = null
            }
        )
    }
    
    // Remove PDFs from Folder Screen
    if (showRemovePdfsFromFolder && folderToManage != null) {
        val currentFolder = folderToManage!!
        // We need to show only PDFs in this folder
        val pdfsInFolder by viewModel.folderFilteredPdfs.collectAsState()
        var selectedToRemove by remember { mutableStateOf<Set<String>>(emptySet()) }
        
        // Reuse selection screen concept but for removal
        PdfSelectionScreen(
            title = "Remove from \"${currentFolder.name}\"",
            allPdfs = pdfsInFolder,
            preSelectedUris = emptySet(),
            onDone = { selectedUris ->
                if (selectedUris.isNotEmpty()) {
                    viewModel.removePdfsFromFolder(currentFolder.id, selectedUris.toList())
                }
                showRemovePdfsFromFolder = false
                folderToManage = null
            },
            onCancel = {
                showRemovePdfsFromFolder = false
                folderToManage = null
            }
        )
    }
    
    // Add file to folder(s) sheet (from file long-press menu)
    if (fileToAddToFolder != null) {
        val file = fileToAddToFolder!!
        val currentFolderIds by viewModel.getFolderIdsForPdf(file.uri).collectAsState(initial = emptyList())
        
        AddToFolderSheet(
            folders = folders,
            currentFolderIds = currentFolderIds,
            onToggleFolder = { folderId, isChecked ->
                viewModel.togglePdfInFolder(folderId, file.uri, isChecked)
            },
            onCreateNewFolder = {
                fileToAddToFolder = null
                showCreateFolderSheet = true
            },
            onDismiss = { fileToAddToFolder = null }
        )
    }
    // ============ END FOLDER DIALOGS ============
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AllPdfsTab(
    pdfs: List<com.hyntix.android.pdfmanager.data.model.PdfFile>,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onPdfSelected: (String) -> Unit,
    onLongPress: (com.hyntix.android.pdfmanager.data.model.PdfFile) -> Unit,
    onDelete: (com.hyntix.android.pdfmanager.data.model.PdfFile) -> Unit,
    selectedPath: String?,
    selectionMode: Boolean = false,
    selectedUris: Set<String> = emptySet(),
    listState: androidx.compose.foundation.lazy.LazyListState
) {
    androidx.compose.material3.pulltorefresh.PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize()
    ) {
        androidx.compose.foundation.lazy.LazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = listState
        ) {
            if (pdfs.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillParentMaxSize()) {
                        EmptyState("No PDFs found", "Pull down to scan for PDF files")
                    }
                }
            } else {
                items(
                    count = pdfs.size,
                    key = { index -> pdfs[index].path }
                ) { index ->
                    val pdf = pdfs[index]
                    FileListItem(
                        name = pdf.name,
                        info = formatSize(pdf.size) + " • " + formatDate(pdf.lastModified),
                        icon = PhosphorIcons.Regular.FilePdf,
                        isDir = false,
                        isSelected = if (selectionMode) selectedUris.contains(pdf.uri) else pdf.path == selectedPath,
                        onClick = { onPdfSelected(pdf.uri) },
                        onLongClick = { onLongPress(pdf) },
                        onDelete = if (selectionMode) null else { { onDelete(pdf) } }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilesTab(
    files: List<com.hyntix.android.pdfmanager.data.model.PdfFile>,
    onFileClick: (com.hyntix.android.pdfmanager.data.model.PdfFile) -> Unit,
    onNavigateUp: () -> Unit,
    onLongPress: (com.hyntix.android.pdfmanager.data.model.PdfFile) -> Unit,
    onDelete: (com.hyntix.android.pdfmanager.data.model.PdfFile) -> Unit,
    selectedPath: String?,
    selectionMode: Boolean = false,
    selectedUris: Set<String> = emptySet(),
    listState: androidx.compose.foundation.lazy.LazyListState
) {
    Box(modifier = Modifier.fillMaxSize()) {
        androidx.compose.foundation.lazy.LazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = listState
        ) {
            if (files.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                        EmptyState("No PDFs found", "")
                    }
                }
            } else {
                items(
                    count = files.size,
                    key = { index -> files[index].path }
                ) { index ->
                    val file = files[index]
                    FileListItem(
                        name = file.name,
                        info = if (file.isDirectory) "Folder" else formatSize(file.size) + " • " + formatDate(file.lastModified),
                        icon = if (file.isDirectory) PhosphorIcons.Regular.Folder else PhosphorIcons.Regular.FilePdf,
                        isDir = file.isDirectory,
                        isSelected = if (!file.isDirectory && selectionMode) selectedUris.contains(file.uri) else file.path == selectedPath,
                        onClick = { onFileClick(file) },
                        onLongClick = if (!file.isDirectory) { { onLongPress(file) } } else null,
                        onDelete = if (!file.isDirectory && !selectionMode) { { onDelete(file) } } else null
                    )
                }
            }
        }
    }
}


@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun FileListItem(
    name: String,
    info: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isDir: Boolean,
    isSelected: Boolean = false,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null
) {
    val dismissState = rememberSwipeToDismissBoxState(
        positionalThreshold = { it * 0.5f }
    )
    
    // Handle delete and snap back without using deprecated confirmValueChange
    LaunchedEffect(dismissState.currentValue) {
        if (dismissState.currentValue == SwipeToDismissBoxValue.EndToStart) {
            if (onDelete != null) {
                onDelete()
            }
            // Snap back to settled state
            dismissState.snapTo(SwipeToDismissBoxValue.Settled)
        }
    }
    
    // Only enable swipe if onDelete is provided and not a directory
    if (onDelete != null && !isDir) {
        SwipeToDismissBox(
            state = dismissState,
            enableDismissFromStartToEnd = false,
            enableDismissFromEndToStart = true,
            backgroundContent = {
                // Delete background (shown when swiping left)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.errorContainer),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Icon(
                        imageVector = PhosphorIcons.Regular.Trash,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(end = 24.dp)
                    )
                }
            },
            content = {
                FileListItemContent(
                    name = name,
                    info = info,
                    icon = icon,
                    isSelected = isSelected,
                    onClick = onClick,
                    onLongClick = onLongClick
                )
            }
        )
    } else {
        FileListItemContent(
            name = name,
            info = info,
            icon = icon,
            isSelected = isSelected,
            onClick = onClick,
            onLongClick = onLongClick
        )
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun FileListItemContent(
    name: String,
    info: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)?
) {
    ListItem(
        headlineContent = { 
            Text(
                text = name, 
                maxLines = 1, 
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                modifier = Modifier.padding(bottom = 4.dp)
            ) 
        },
        supportingContent = if (info.isNotEmpty()) { { Text(info) } } else null,
        leadingContent = { 
            Icon(
                imageVector = if (isSelected) PhosphorIcons.Regular.CheckCircle else icon, 
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = if (isSelected) MaterialTheme.colorScheme.primary else LocalContentColor.current
            ) 
        },
        tonalElevation = if (isSelected) 8.dp else 0.dp,
        colors = ListItemDefaults.colors(
            containerColor = if (isSelected) 
                MaterialTheme.colorScheme.secondaryContainer
            else 
                MaterialTheme.colorScheme.surface
        ),
        modifier = Modifier
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    )
}

// Helper functions for formatting
// Helper functions for formatting
// Use shared formatters to avoid allocation on every item bind
private val sizeFormat = java.text.DecimalFormat("#,##0.#")
private val dateFormat = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault())

fun formatSize(size: Long): String {
     if (size <= 0) return "0 B"
     val units = arrayOf("B", "KB", "MB", "GB", "TB")
     val digitGroups = (Math.log10(size.toDouble()) / Math.log10(1024.0)).toInt()
     // synchronized not strictly needed for UI thread only, but good practice if reused elsewhere
     return sizeFormat.format(size / Math.pow(1024.0, digitGroups.toDouble())) + " " + units[digitGroups]
}

fun formatDate(timestamp: Long): String {
    return dateFormat.format(java.util.Date(timestamp))
}

fun formatRelativeTime(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    val seconds = diff / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    val days = hours / 24
    
    return when {
        seconds < 60 -> "Just now"
        minutes < 60 -> "${minutes}m ago"
        hours < 24 -> "${hours}h ago"
        days < 7 -> "${days}d ago"
        else -> formatDate(timestamp) // Reuses shared formatter
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecentFilesTab(
    pdfs: List<com.hyntix.android.pdfmanager.data.model.PdfFile>,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onPdfSelected: (String) -> Unit,
    onClearClick: (String) -> Unit,
    listState: androidx.compose.foundation.lazy.LazyListState
) {
    androidx.compose.material3.pulltorefresh.PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize()
    ) {
        androidx.compose.foundation.lazy.LazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = listState
        ) {
            if (pdfs.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillParentMaxSize()) {
                        EmptyState("No recent files", "PDFs you open will appear here")
                    }
                }
            } else {
                items(
                    count = pdfs.size,
                    key = { index -> pdfs[index].path }
                ) { index ->
                    val pdf = pdfs[index]
                    val dismissState = rememberSwipeToDismissBoxState(
                        positionalThreshold = { it * 0.5f }
                    )
                    
                    androidx.compose.runtime.LaunchedEffect(dismissState.currentValue) {
                        if (dismissState.currentValue == SwipeToDismissBoxValue.EndToStart) {
                            onClearClick(pdf.uri)
                        }
                    }
                    
                    SwipeToDismissBox(
                        state = dismissState,
                        enableDismissFromStartToEnd = false,
                        enableDismissFromEndToStart = true,
                        backgroundContent = {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(MaterialTheme.colorScheme.errorContainer),
                                contentAlignment = Alignment.CenterEnd
                            ) {
                                Icon(
                                    imageVector = PhosphorIcons.Regular.Trash,
                                    contentDescription = "Remove",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.padding(end = 24.dp)
                                )
                            }
                        },
                        content = {
                            androidx.compose.material3.ListItem(
                                headlineContent = { Text(pdf.name, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis) },
                                supportingContent = { Text(formatSize(pdf.size) + " • " + formatRelativeTime(pdf.lastModified)) },
                                leadingContent = {
                                    Icon(
                                        imageVector = PhosphorIcons.Regular.FilePdf,
                                        contentDescription = null,
                                        modifier = Modifier.size(40.dp)
                                    )
                                },
                                modifier = Modifier
                                    .background(MaterialTheme.colorScheme.surface)
                                    .clickable { onPdfSelected(pdf.uri) }
                            )
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FavoritesTab(
    pdfs: List<com.hyntix.android.pdfmanager.data.model.PdfFile>,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onPdfSelected: (String) -> Unit,
    onRemove: (com.hyntix.android.pdfmanager.data.model.PdfFile) -> Unit,
    listState: androidx.compose.foundation.lazy.LazyListState
) {
    androidx.compose.material3.pulltorefresh.PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize()
    ) {
        androidx.compose.foundation.lazy.LazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = listState
        ) {
            if (pdfs.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillParentMaxSize()) {
                        EmptyState("No favorites", "Star PDFs to add them to your favorites")
                    }
                }
            } else {
                items(
                    count = pdfs.size,
                    key = { index -> pdfs[index].path }
                ) { index ->
                    val pdf = pdfs[index]
                    val dismissState = rememberSwipeToDismissBoxState(
                        positionalThreshold = { it * 0.5f }
                    )

                    androidx.compose.runtime.LaunchedEffect(dismissState.currentValue) {
                        if (dismissState.currentValue == SwipeToDismissBoxValue.EndToStart) {
                            onRemove(pdf)
                        }
                    }
                    
                    SwipeToDismissBox(
                        state = dismissState,
                        enableDismissFromStartToEnd = false,
                        enableDismissFromEndToStart = true,
                        backgroundContent = {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(MaterialTheme.colorScheme.errorContainer),
                                contentAlignment = Alignment.CenterEnd
                            ) {
                                Icon(
                                    imageVector = PhosphorIcons.Regular.Trash,
                                    contentDescription = "Remove",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.padding(end = 24.dp)
                                )
                            }
                        },
                        content = {
                            androidx.compose.material3.ListItem(
                                headlineContent = { Text(pdf.name, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis) },
                                supportingContent = { Text(formatSize(pdf.size) + " • " + formatDate(pdf.lastModified)) },
                                leadingContent = {
                                    Icon(
                                        imageVector = PhosphorIcons.Regular.FilePdf,
                                        contentDescription = null,
                                        modifier = Modifier.size(40.dp)
                                    )
                                },
                                modifier = Modifier
                                    .background(MaterialTheme.colorScheme.surface)
                                    .clickable { onPdfSelected(pdf.uri) }
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun LoadingSkeletonList() {
    androidx.compose.foundation.lazy.LazyColumn(
        modifier = Modifier.fillMaxSize(),
        userScrollEnabled = false // Skeletons shouldn't scroll
    ) {
        items(10) {
            FileListItemSkeleton()
        }
    }
}

@Composable
fun FileListItemSkeleton() {
    ListItem(
        headlineContent = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp) // Margin behavior
                    .height(16.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .shimmerEffect()
            )
        },
        supportingContent = {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(12.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .shimmerEffect()
            )
        },
        leadingContent = {
            Box(
                modifier = Modifier
                    .size(40.dp) // Increase size
                    .clip(RoundedCornerShape(4.dp))
                    .shimmerEffect()
            )
        },
        modifier = Modifier.clickable(enabled = false) {}
    )
}

/**
 * Optimized shimmer effect that doesn't cause recompositions.
 * Uses fixed animation range instead of measuring size on each frame.
 */
fun Modifier.shimmerEffect(): Modifier = composed {
    val shimmerColors = listOf(
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f),
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
    )
    
    val transition = androidx.compose.animation.core.rememberInfiniteTransition(label = "Shimmer")
    val translateAnimation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,  // Fixed value - no measurement needed
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = androidx.compose.animation.core.tween(
                durationMillis = 1000,
                easing = androidx.compose.animation.core.LinearEasing
            ),
            repeatMode = androidx.compose.animation.core.RepeatMode.Restart
        ),
        label = "ShimmerTranslate"
    )
    
    background(
        brush = androidx.compose.ui.graphics.Brush.linearGradient(
            colors = shimmerColors,
            start = androidx.compose.ui.geometry.Offset(translateAnimation - 500f, 0f),
            end = androidx.compose.ui.geometry.Offset(translateAnimation, translateAnimation / 5f)
        )
    )
}

@Composable
private fun EmptyState(title: String, subtitle: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = PhosphorIcons.Regular.File,
                contentDescription = null,
                modifier = Modifier.size(80.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
@Composable
fun FileActionPanel(
    visible: Boolean,
    onShare: () -> Unit,
    onRename: () -> Unit,
    onAddToFolder: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
    showRename: Boolean = true
) {
    
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter
    ) {
        androidx.compose.animation.AnimatedVisibility(
            visible = visible,
            enter = androidx.compose.animation.slideInVertically(initialOffsetY = { it }) + androidx.compose.animation.fadeIn(),
            exit = androidx.compose.animation.slideOutVertically(targetOffsetY = { it }) + androidx.compose.animation.fadeOut(),
            modifier = Modifier.padding(bottom = 32.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(32.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                shadowElevation = 8.dp,
                modifier = Modifier
                    .height(64.dp)
                    .widthIn(max = 360.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceEvenly,
                    modifier = Modifier.padding(horizontal = 12.dp)
                ) {
                    // Share
                    TooltipBox(
                        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
                        tooltip = { PlainTooltip { Text("Share selected files") } },
                        state = rememberTooltipState()
                    ) {
                        IconButton(onClick = onShare) {
                            Icon(
                                imageVector = PhosphorIcons.Regular.ShareNetwork,
                                contentDescription = "Share",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                    
                    // Rename (only show for single file)
                    if (showRename) {
                        TooltipBox(
                            positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
                            tooltip = { PlainTooltip { Text("Rename file") } },
                            state = rememberTooltipState()
                        ) {
                            IconButton(onClick = onRename) {
                                Icon(
                                    imageVector = PhosphorIcons.Regular.PencilSimple,
                                    contentDescription = "Rename",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                    
                    // Add to Folder
                    TooltipBox(
                        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
                        tooltip = { PlainTooltip { Text("Add to folder") } },
                        state = rememberTooltipState()
                    ) {
                        IconButton(onClick = onAddToFolder) {
                            Icon(
                                imageVector = PhosphorIcons.Regular.Folder,
                                contentDescription = "Add to folder",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    // Delete
                    TooltipBox(
                        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
                        tooltip = { PlainTooltip { Text("Delete selected files") } },
                        state = rememberTooltipState()
                    ) {
                        IconButton(onClick = onDelete) {
                            Icon(
                                imageVector = PhosphorIcons.Regular.Trash,
                                contentDescription = "Delete",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    
                    // Vertical Divider
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(24.dp)
                            .background(MaterialTheme.colorScheme.outlineVariant)
                    )

                    // Close/Dismiss
                    TooltipBox(
                        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
                        tooltip = { PlainTooltip { Text("Clear selection") } },
                        state = rememberTooltipState()
                    ) {
                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = PhosphorIcons.Regular.X,
                                contentDescription = "Dismiss",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileInfoBottomSheet(
    file: com.hyntix.android.pdfmanager.data.model.PdfFile,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
        ) {
            // Title
            Text(
                text = "File Info",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            
            HorizontalDivider()
            
            // File info rows
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                FileInfoRow(label = "Name", value = file.name)
                FileInfoRow(label = "Size", value = formatSize(file.size))
                FileInfoRow(label = "Modified", value = formatDate(file.lastModified))
                FileInfoRow(label = "Path", value = file.path, isSmall = true)
            }
        }
    }
}

@Composable
private fun FileInfoRow(
    label: String,
    value: String,
    isSmall: Boolean = false
) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = if (isSmall) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyLarge,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
        )
    }
}
