@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.hyntix.android.pdfmanager.ui.viewer

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import com.hyntix.android.pdfmanager.R
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.delay
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.WindowInsetsCompat
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.regular.*
import com.adamglin.phosphoricons.Fill
import com.adamglin.phosphoricons.fill.*
import com.adamglin.phosphoricons.Bold
import com.adamglin.phosphoricons.bold.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.*
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.background
import androidx.compose.ui.text.buildAnnotatedString
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.LocalContentColor
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.draw.alpha
import androidx.core.content.FileProvider
import com.hyntix.pdf.viewer.PDFView
import com.hyntix.pdf.viewer.listener.OnLoadCompleteListener
import com.hyntix.pdf.viewer.model.PdfBookmark
import com.hyntix.pdf.viewer.model.PdfViewState
import com.hyntix.pdf.viewer.scroll.DefaultScrollHandle
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import com.hyntix.android.pdfmanager.ui.viewer.components.DocumentInfoSheet
import com.hyntix.android.pdfmanager.ui.viewer.reflow.ReflowScreen

// Simple data class to hold flat bookmark for LazyColumn
data class FlatBookmark(
    val title: String,
    val pageIndex: Int,
    val depth: Int,
    val pageLabel: String = ""
)

// Helper to flatten nested bookmarks for LazyColumn
fun flattenBookmarks(bookmarks: List<PdfBookmark>, depth: Int = 0): List<FlatBookmark> {
    val result = mutableListOf<FlatBookmark>()
    for (bookmark in bookmarks) {
        result.add(FlatBookmark(bookmark.title, bookmark.pageIndex, depth, bookmark.pageLabel))
        bookmark.children?.let { result.addAll(flattenBookmarks(it, depth + 1)) }
    }
    return result
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfViewerScreen(
    uri: Uri?,
    isExternal: Boolean = false,
    isFavoriteInitial: Boolean = false,
    initialPage: Int = 0,
    onFavoriteToggle: (String, Boolean) -> Unit = { _, _ -> },
    onPageChange: (Int) -> Unit = {},
    initialVerticalScroll: Boolean = true,
    initialScrollMode: Int = 0,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    var showToc by remember { mutableStateOf(false) }
    var tocItems by remember { mutableStateOf<List<FlatBookmark>>(emptyList()) }
    var pdfViewRef: PDFView? by remember { mutableStateOf(null) }
    var isImmersive by remember { mutableStateOf(false) }
    var isSnapEnabled by remember { mutableStateOf(initialScrollMode == 1) }
    var isInitial by remember { mutableStateOf(true) }

    var hasText by remember { mutableStateOf(true) } // default true, will be updated after PDF loads
    
    // Password State
    var password by remember { mutableStateOf<String?>(null) }
    var showPasswordDialog by rememberSaveable { mutableStateOf(false) }
    var isPasswordError by rememberSaveable { mutableStateOf(false) }

    // Immersive Mode: Control System Bars
    val view = LocalView.current
    LaunchedEffect(isImmersive) {
        val window = (view.context as? android.app.Activity)?.window
        window?.let { win ->
            val insetsController = WindowInsetsControllerCompat(win, view)
            if (isImmersive) {
                insetsController.hide(WindowInsetsCompat.Type.systemBars())
                insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            } else {
                insetsController.show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    // Handles system back button to close TOC drawer
    BackHandler(enabled = showToc) {
        showToc = false
    }

    // Save PDF state across config changes
    var savedPdfState by rememberSaveable { mutableStateOf<PdfViewState?>(null) }
    
    // Snackbar for save feedback
    val snackbarHostState = remember { androidx.compose.material3.SnackbarHostState() }
    
    // Bottom bar state
    var currentPage by remember { mutableStateOf(initialPage) }
    var totalPages by remember { mutableStateOf(0) }
    var isNightMode by rememberSaveable { mutableStateOf(false) }
    var isVerticalScroll by rememberSaveable(initialVerticalScroll) { mutableStateOf(initialVerticalScroll) }
    var showJumpDialog by remember { mutableStateOf(false) }
    var showDocumentInfo by remember { mutableStateOf(false) }
    var isFavorite by remember { mutableStateOf(isFavoriteInitial) }
    
    // Sync isFavorite when isFavoriteInitial changes (async loading from MainActivity)
    androidx.compose.runtime.LaunchedEffect(isFavoriteInitial) {
        isFavorite = isFavoriteInitial
    }
    // Extract file name from URI - properly handle content:// URIs
    val fileName = remember(uri) {
        uri?.let { u ->
            when (u.scheme) {
                "content" -> {
                    // Query ContentResolver for the display name
                    try {
                        context.contentResolver.query(u, null, null, null, null)?.use { cursor ->
                            if (cursor.moveToFirst()) {
                                val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                                if (nameIndex >= 0) {
                                    cursor.getString(nameIndex)
                                } else null
                            } else null
                        }
                    } catch (e: Exception) {
                        null
                    }
                }
                "file" -> {
                    // For file:// URIs, extract from path
                    u.path?.substringAfterLast("/")
                }
                else -> u.lastPathSegment?.substringAfterLast("/")?.substringAfterLast(":")
            } ?: "PDF Document"
        } ?: "PDF Document"
    }

    // Save state periodically and on dispose
    DisposableEffect(pdfViewRef) {
        onDispose {
            pdfViewRef?.let { savedPdfState = it.saveState() }
        }
    }
    
    // Save dialog state for external files
    var showSaveDialog by remember { mutableStateOf(false) }
    var saveFileName by remember { mutableStateOf("") }
    
    // Initialize save file name when dialog is shown
    LaunchedEffect(showSaveDialog) {
        if (showSaveDialog) {
            // Pre-fill with current fileName, ensure it ends with .pdf
            val baseName = if (fileName.endsWith(".pdf", ignoreCase = true)) {
                fileName.dropLast(4)
            } else {
                fileName
            }
            saveFileName = baseName
        }
    }
    
    // Save function for external files with custom name
    fun saveFileWithName(customName: String) {
        uri?.let { sourceUri ->
            scope.launch {
                try {
                    // Create target directory: Download/PDFViewer/
                    val downloadDir = android.os.Environment.getExternalStoragePublicDirectory(
                        android.os.Environment.DIRECTORY_DOWNLOADS
                    )
                    val pdfViewerDir = File(downloadDir, "PDFManager")
                    if (!pdfViewerDir.exists()) {
                        pdfViewerDir.mkdirs()
                    }
                    
                    // Use custom filename with .pdf extension
                    val finalName = if (customName.endsWith(".pdf", ignoreCase = true)) {
                        customName
                    } else {
                        "$customName.pdf"
                    }
                    val targetFile = File(pdfViewerDir, finalName)
                    
                    // Copy content from URI to file
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openInputStream(sourceUri)?.use { input ->
                            targetFile.outputStream().use { output ->
                                input.copyTo(output)
                            }
                        }
                    }
                    // Show save confirmation
                    snackbarHostState.showSnackbar(
                        message = "Saved to Download/PDFManager/$finalName",
                        duration = androidx.compose.material3.SnackbarDuration.Short
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                    snackbarHostState.showSnackbar(
                        message = "Failed to save file",
                        duration = androidx.compose.material3.SnackbarDuration.Short
                    )
                }
            }
        }
    }
    
    // Share function for device files
    fun shareFile() {
        uri?.let { originalUri ->
            try {
                // Convert file:// URI to content:// URI using FileProvider
                val shareUri = if (originalUri.scheme == "file") {
                    val file = File(originalUri.path ?: return@let)
                    FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        file
                    )
                } else {
                    // Already a content:// URI, use as-is
                    originalUri
                }
                
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/pdf"
                    putExtra(Intent.EXTRA_STREAM, shareUri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(shareIntent, "Share PDF"))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Search State
    var showSearch by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<Map<Int, List<android.graphics.RectF>>>(emptyMap()) }
    var isSearching by remember { mutableStateOf(false) }
    var currentSearchMatchIndex by remember { mutableStateOf(0) }
    var totalSearchMatches by remember { mutableStateOf(0) }
    val searchFocusRequester = remember { FocusRequester() }

    // Reflow Mode State - use rememberSaveable to persist across config changes
    var isReflowMode by rememberSaveable { mutableStateOf(false) }
    
    // Track search job for cancellation
    var searchJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }
    
    // Search Function - starts from current page and expands outward for faster first results
    fun performSearch(query: String) {
        val trimmedQuery = query.trim()
        if (trimmedQuery.isBlank()) {
            searchResults = emptyMap()
            totalSearchMatches = 0
            currentSearchMatchIndex = 0
            return
        }
        
        // Cancel any existing search
        searchJob?.cancel()
        
        isSearching = true
        searchJob = scope.launch(Dispatchers.IO) {
            val results = mutableMapOf<Int, List<android.graphics.RectF>>()
            var total = 0
            var foundFirstMatch = false
            
            val pageCount = pdfViewRef?.pageCount ?: 0
            val currentPage = pdfViewRef?.currentPage ?: 0
            
            // Generate page order: current page first, then expand outward
            // e.g., if on page 5 of 10: [5, 6, 4, 7, 3, 8, 2, 9, 1, 0]
            val pageOrder = mutableListOf<Int>()
            pageOrder.add(currentPage)
            var offset = 1
            while (pageOrder.size < pageCount) {
                val forward = currentPage + offset
                val backward = currentPage - offset
                if (forward < pageCount) pageOrder.add(forward)
                if (backward >= 0) pageOrder.add(backward)
                offset++
            }
            
            for (pageIndex in pageOrder) {
                // Check if job was cancelled
                if (searchJob?.isActive != true) return@launch
                
                val matches = pdfViewRef?.pdfFile?.searchPage(pageIndex, trimmedQuery)
                if (!matches.isNullOrEmpty()) {
                    results[pageIndex] = matches
                    total += matches.size
                    
                    // Update UI with first match immediately (for better UX)
                    if (!foundFirstMatch) {
                        foundFirstMatch = true
                        withContext(Dispatchers.Main) {
                            searchResults = results.toMap()
                            totalSearchMatches = total
                            currentSearchMatchIndex = 0
                            // Jump to first match (which is on/near current page)
                            pdfViewRef?.jumpTo(pageIndex, true)
                        }
                    }
                }
            }
            
            // Final update with all results
            withContext(Dispatchers.Main) {
                searchResults = results.toMap()
                totalSearchMatches = total
                isSearching = false
            }
        }
    }

    
    // Cancel search and reset state
    fun cancelSearchAndClose() {
        searchJob?.cancel()
        showSearch = false
        searchQuery = ""
        searchResults = emptyMap()
        totalSearchMatches = 0
        currentSearchMatchIndex = 0
        isSearching = false
    }

    

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { androidx.compose.material3.SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            AnimatedVisibility(
                visible = !isImmersive && !isReflowMode,
                enter = slideInVertically(initialOffsetY = { -it }),
                exit = slideOutVertically(targetOffsetY = { -it })
            ) {
                Column {
                    if (showSearch) {
                        BackHandler {
                            cancelSearchAndClose()
                        }
                        
                        TopAppBar(
                            title = {
                                // Search Input
                                Box(
                                    contentAlignment = androidx.compose.ui.Alignment.CenterStart
                                ) {
                                    if (searchQuery.isEmpty()) {
                                        Text(
                                            text = "Find in page",
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    
                                    LaunchedEffect(Unit) {
                                        searchFocusRequester.requestFocus()
                                    }
                                    
                                    BasicTextField(
                                        value = searchQuery,
                                        onValueChange = { 
                                            searchQuery = it 
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .focusRequester(searchFocusRequester),
                                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                                            color = MaterialTheme.colorScheme.onSurface
                                        ),
                                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                        singleLine = true,
                                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                            imeAction = androidx.compose.ui.text.input.ImeAction.Search
                                        ),
                                        keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                                            onSearch = { performSearch(searchQuery) }
                                        )
                                    )
                                }
                            },
                            actions = {
                                // Search Count and Navigation
                                if (totalSearchMatches > 0) {
                                    Row(
                                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "${currentSearchMatchIndex + 1}/$totalSearchMatches",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(horizontal = 8.dp)
                                        )
                                        
                                        // Vertical Divider
                                        Box(
                                            modifier = Modifier
                                                .width(1.dp)
                                                .height(24.dp)
                                                .background(MaterialTheme.colorScheme.outlineVariant)
                                        )
                                        
                                        TooltipBox(
                                            positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
                                            tooltip = { PlainTooltip { Text("Previous match") } },
                                            state = rememberTooltipState()
                                        ) {
                                            IconButton(
                                                onClick = {
                                                    if (currentSearchMatchIndex > 0) {
                                                        currentSearchMatchIndex--
                                                        // Jump logic
                                                        var count = 0
                                                        for ((page, matches) in searchResults) {
                                                            if (count + matches.size > currentSearchMatchIndex) {
                                                                pdfViewRef?.jumpTo(page, true)
                                                                break
                                                            }
                                                            count += matches.size
                                                        }
                                                    }
                                                },
                                                enabled = currentSearchMatchIndex > 0
                                            ) {
                                                Icon(
                                                    PhosphorIcons.Regular.CaretUp,
                                                    contentDescription = "Previous"
                                                )
                                            }
                                        }
                                        
                                        TooltipBox(
                                            positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
                                            tooltip = { PlainTooltip { Text("Next match") } },
                                            state = rememberTooltipState()
                                        ) {
                                            IconButton(
                                                onClick = {
                                                    if (currentSearchMatchIndex < totalSearchMatches - 1) {
                                                        currentSearchMatchIndex++
                                                        // Jump logic
                                                        var count = 0
                                                        for ((page, matches) in searchResults) {
                                                            if (count + matches.size > currentSearchMatchIndex) {
                                                                pdfViewRef?.jumpTo(page, true)
                                                                break
                                                            }
                                                            count += matches.size
                                                        }
                                                    }
                                                },
                                                enabled = currentSearchMatchIndex < totalSearchMatches - 1
                                            ) {
                                                Icon(
                                                    PhosphorIcons.Regular.CaretDown,
                                                    contentDescription = "Next"
                                                )
                                            }
                                        }
                                    }
                                }
                                
                                // Close Search / Loading Indicator
                                if (isSearching) {
                                    Box(
                                        modifier = Modifier.size(48.dp),
                                        contentAlignment = androidx.compose.ui.Alignment.Center
                                    ) {
                                        androidx.compose.material3.CircularProgressIndicator(
                                            modifier = Modifier.size(24.dp),
                                            strokeWidth = 2.dp
                                        )
                                    }
                                } else {
                                    TooltipBox(
                                        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
                                        tooltip = { PlainTooltip { Text("Close Search") } },
                                        state = rememberTooltipState()
                                    ) {
                                        IconButton(onClick = { cancelSearchAndClose() }) {
                                            Icon(
                                                PhosphorIcons.Regular.X,
                                                contentDescription = "Close Search"
                                            )
                                        }
                                    }
                                }
                            }
                        )
                    } else {
                        // Regular TopAppBar
                        TopAppBar(
                            title = {
                                Text(
                                    text = fileName,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            navigationIcon = {
                                TooltipBox(
                                    positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
                                    tooltip = { PlainTooltip { Text("Back") } },
                                    state = rememberTooltipState()
                                ) {
                                    IconButton(onClick = onBackClick) {
                                        Icon(
                                            imageVector = PhosphorIcons.Regular.ArrowLeft,
                                            contentDescription = "Back"
                                        )
                                    }
                                }
                            },
                            actions = {
                                // Search Button (opens search bar) - hide for image-only PDFs
                                if (hasText) {
                                    TooltipBox(
                                        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
                                        tooltip = { PlainTooltip { Text("Search in PDF") } },
                                        state = rememberTooltipState()
                                    ) {
                                        IconButton(onClick = { showSearch = true }) {
                                            Icon(
                                                imageVector = PhosphorIcons.Regular.MagnifyingGlass,
                                                contentDescription = "Search"
                                            )
                                        }
                                    }
                                }
                                
                                // TOC button
                                if (tocItems.isNotEmpty()) {
                                    TooltipBox(
                                        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
                                        tooltip = { PlainTooltip { Text("Table of Contents") } },
                                        state = rememberTooltipState()
                                    ) {
                                        IconButton(onClick = { showToc = !showToc }) {
                                            Icon(
                                                imageVector = PhosphorIcons.Regular.List,
                                                contentDescription = "Table of Contents"
                                            )
                                        }
                                    }
                                }


                            }
                        )
                    }
                }
            }
        },
        bottomBar = {
            AnimatedVisibility(
                visible = !isImmersive && !isReflowMode,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it })
            ) {
                androidx.compose.material3.Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .height(56.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                
                // Go to Page button (moved from floating capsule)
                TooltipBox(
                    positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
                    tooltip = { PlainTooltip { Text("Go to Page") } },
                    state = rememberTooltipState(),
                    modifier = Modifier.weight(1f)
                ) {
                    IconButton(
                        onClick = { showJumpDialog = true }
                    ) {
                        Icon(
                            imageVector = PhosphorIcons.Regular.Hash,
                            contentDescription = "Go to Page"
                        )
                    }
                }
                // Favorite Toggle - only show for internal files (not from external apps)
                if (!isExternal) {
                    TooltipBox(
                        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
                        tooltip = { PlainTooltip { Text(if (isFavorite) "Remove from Favorites" else "Add to Favorites") } },
                        state = rememberTooltipState(),
                        modifier = Modifier.weight(1f)
                    ) {
                        IconButton(
                            onClick = { 
                                val newFavorite = !isFavorite
                                isFavorite = newFavorite
                                uri?.toString()?.let { uriStr -> onFavoriteToggle(uriStr, newFavorite) }
                            }
                        ) {
                            Icon(
                                imageVector = if (isFavorite) PhosphorIcons.Fill.Star else PhosphorIcons.Regular.Star,
                                contentDescription = if (isFavorite) "Remove from Favorites" else "Add to Favorites",
                                tint = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
                
                // Night Mode Toggle
                TooltipBox(
                    positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
                    tooltip = { PlainTooltip { Text(if (isNightMode) "Disable Night Mode" else "Enable Night Mode") } },
                    state = rememberTooltipState(),
                    modifier = Modifier.weight(1f)
                ) {
                    IconButton(
                        onClick = { 
                            isNightMode = !isNightMode
                            pdfViewRef?.setNightMode(isNightMode)
                            pdfViewRef?.redraw()
                        }
                    ) {
                        Icon(
                            imageVector = if (isNightMode) PhosphorIcons.Fill.Moon else PhosphorIcons.Regular.Moon,
                            contentDescription = if (isNightMode) "Disable Night Mode" else "Enable Night Mode",
                            tint = if (isNightMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                
                // Scroll Direction Toggle - hide for single page files
                if (totalPages > 1) {
                    TooltipBox(
                        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
                        tooltip = { PlainTooltip { Text(if (isVerticalScroll) "Switch to Horizontal" else "Switch to Vertical") } },
                        state = rememberTooltipState(),
                        modifier = Modifier.weight(1f)
                    ) {
                        IconButton(
                            onClick = { 
                                isVerticalScroll = !isVerticalScroll
                                // PdfViewerContent will reload with new direction, retaining current page
                            }
                        ) {
                            Icon(
                                imageVector = if (isVerticalScroll) PhosphorIcons.Regular.Scroll else PhosphorIcons.Regular.BookOpenText,
                                contentDescription = if (isVerticalScroll) "Switch to Horizontal" else "Switch to Vertical"
                            )
                        }
                    }
                }
                
                // Reflow Mode Toggle - only show if document has text
                if (hasText) {
                    TooltipBox(
                        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
                        tooltip = { PlainTooltip { Text(if (isReflowMode) "Exit Reflow Mode" else "Enter Reflow Mode") } },
                        state = rememberTooltipState(),
                        modifier = Modifier.weight(1f)
                    ) {
                        IconButton(
                            onClick = { isReflowMode = !isReflowMode }
                        ) {
                            Icon(
                                imageVector = PhosphorIcons.Regular.Rows,
                                contentDescription = if (isReflowMode) "Exit Reflow Mode" else "Enter Reflow Mode",
                                tint = if (isReflowMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
                }
                }
            }
        }

    ) { innerPadding ->
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            // Text Selection State - must be outside the conditional to persist
            var selectionText by remember { mutableStateOf("") }
            var isSelectionActive by remember { mutableStateOf(false) }
            var selectionPage by remember { mutableStateOf(-1) }

            // IMPORTANT: Always keep PdfViewerContent composed to prevent document disposal
            // Use alpha to hide it when in Reflow mode
            // Apply innerPadding ONLY to the PDF viewer, not to ReflowScreen
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .then(if (isReflowMode) Modifier.alpha(0f) else Modifier)
            ) {
                // PDF Manager
                PdfViewerContent(
                    modifier = Modifier.fillMaxSize(),
                    uri = uri,
                    password = password,
                    savedState = savedPdfState,
                    isVerticalScroll = isVerticalScroll,
                    onTocLoaded = { tocItems = flattenBookmarks(it) },
                    onPdfViewReady = { pdfView -> 
                        pdfViewRef = pdfView
                    },
                    onError = { t ->
                         if (t is com.hyntix.pdf.viewer.exception.PdfPasswordException) {
                             showPasswordDialog = true
                             isPasswordError = (password != null) // If password was provided but failed, it's incorrect
                         }
                    },
                    onStateRestored = { savedPdfState = null },
                    defaultPage = initialPage,
                    onPageChange = { page, pageCount -> 
                        currentPage = page
                        totalPages = pageCount
                        onPageChange(page)
                    },
                    onPageCountLoaded = { pageCount -> 
                        totalPages = pageCount
                        // Check if document has text for reflow mode availability
                        // This happens after PDF is fully loaded
                        scope.launch(Dispatchers.IO) {
                            val result = pdfViewRef?.pdfFile?.hasAnyText() ?: false
                            withContext(Dispatchers.Main) { hasText = result }
                        }
                    },
                    onTap = { 
                        // If selection is active, clear it on tap
                        if (isSelectionActive) {
                           pdfViewRef?.clearTextSelection()
                        } else {
                           isImmersive = !isImmersive
                        }
                    },
                    enableSnap = isSnapEnabled,
                    nightMode = isNightMode,
                    searchResults = searchResults,
                    currentSearchMatchIndex = currentSearchMatchIndex,
                    onTextSelected = { text, page ->
                        selectionText = text
                        selectionPage = page
                        isSelectionActive = true
                    },
                    onSelectionCleared = {
                        isSelectionActive = false
                        selectionText = ""
                        selectionPage = -1
                    }
                )
                
                
                // TOC Sidesheet (only when not in Reflow mode)
                if (!isReflowMode) {
                    TocSidesheet(
                        visible = showToc,
                        bookmarks = tocItems,
                        onBookmarkClick = { pageIndex ->
                            pdfViewRef?.jumpTo(pageIndex)
                            showToc = false
                        },
                        onDismiss = { showToc = false }
                    )
                    
                    // Floating Selection Panel
                    FloatingSelectionPanel(
                        isVisible = isSelectionActive,
                        onCopy = {
                            if (selectionText.isNotEmpty()) {
                                val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                val clip = android.content.ClipData.newPlainText("PDF Text", selectionText)
                                clipboard.setPrimaryClip(clip)
                                // Clear selection after copy
                                pdfViewRef?.clearTextSelection()
                            }
                        },
                        onSelectAll = {
                            if (selectionPage >= 0) {
                                pdfViewRef?.selectAllText(selectionPage)
                            }
                        },
                        onWebSearch = {
                             if (selectionText.isNotEmpty()) {
                                try {
                                    val intent = android.content.Intent(android.content.Intent.ACTION_WEB_SEARCH).apply {
                                        putExtra(android.app.SearchManager.QUERY, selectionText)
                                    }
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                        },
                        onDismiss = {
                            pdfViewRef?.clearTextSelection()
                        }
                    )
                }
            }
            
            // Reflow Mode overlay (shown on top when active) - NO innerPadding
            if (isReflowMode) {
                ReflowScreen(
                    pdfFile = pdfViewRef?.pdfFile,
                    initialPage = currentPage,
                    onNavigateBack = { isReflowMode = false }
                )
            }
        }
    }

    
    // Save As Dialog
    if (showSaveDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text("Save As") },
            text = {
                Column {
                    Text(
                        text = "Enter file name:",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    androidx.compose.material3.OutlinedTextField(
                        value = saveFileName,
                        onValueChange = { saveFileName = it },
                        singleLine = true,
                        suffix = { Text(".pdf") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                androidx.compose.material3.TextButton(
                    onClick = {
                        showSaveDialog = false
                        saveFileWithName(saveFileName)
                    },
                    enabled = saveFileName.isNotBlank()
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showSaveDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    // Jump to Page Dialog
    if (showJumpDialog) {
        var jumpPageText by remember { mutableStateOf("") }
        
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showJumpDialog = false },
            title = { Text("Go to Page") },
            text = {
                Column {
                    Text(
                        text = "Enter page number (1-$totalPages):",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    androidx.compose.material3.OutlinedTextField(
                        value = jumpPageText,
                        onValueChange = { 
                            // Only allow digits
                            if (it.all { c -> c.isDigit() }) jumpPageText = it
                        },
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                androidx.compose.material3.TextButton(
                    onClick = {
                        val pageNum = jumpPageText.toIntOrNull()
                        if (pageNum != null && pageNum in 1..totalPages) {
                            pdfViewRef?.jumpTo(pageNum - 1) // 0-indexed
                            showJumpDialog = false
                        }
                    },
                    enabled = jumpPageText.isNotBlank() && 
                        (jumpPageText.toIntOrNull() ?: 0) in 1..totalPages
                ) {
                    Text("Go")
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showJumpDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    // Document Info Bottom Sheet
    if (showDocumentInfo) {
        pdfViewRef?.pdfFile?.pdfDocument?.let { doc ->
            DocumentInfoSheet(
                document = doc,
                fileName = fileName,
                onDismiss = { showDocumentInfo = false }
            )
        }
    }


    // Password Dialog
    if (showPasswordDialog) {
        var inputPassword by remember { mutableStateOf("") }
        
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { 
                showPasswordDialog = false 
                onBackClick() // Exit viewer on cancel
            },
            title = { Text(stringResource(R.string.password_required)) },
            text = {
                Column {
                    if (isPasswordError) {
                         Text(
                            text = stringResource(R.string.incorrect_password),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(bottom = 8.dp)
                         )
                    }
                    androidx.compose.material3.OutlinedTextField(
                        value = inputPassword,
                        onValueChange = { inputPassword = it },
                        singleLine = true,
                        label = { Text(stringResource(R.string.password_hint)) },
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Password
                        ),
                        isError = isPasswordError,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                androidx.compose.material3.TextButton(
                    onClick = {
                        showPasswordDialog = false
                        isPasswordError = false
                        password = inputPassword
                    },
                    enabled = inputPassword.isNotEmpty()
                ) {
                    Text(stringResource(R.string.open))
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { 
                    showPasswordDialog = false 
                    onBackClick() // Exit viewer
                }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
fun TocSidesheet(
    visible: Boolean,
    bookmarks: List<FlatBookmark>,
    onBookmarkClick: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = slideInHorizontally { it },
        exit = slideOutHorizontally { it }
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            // Tap area to dismiss (no overlay)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable { onDismiss() }
            )
            
            // TOC Panel (on the right)
            Surface(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(280.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 8.dp
            ) {
                Column {
                    // Header (reduced padding)
                    Text(
                        text = "Contents",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                    )
                    HorizontalDivider()
                    
                    // Bookmark list
                    LazyColumn(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        itemsIndexed(bookmarks) { _, bookmark ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onBookmarkClick(bookmark.pageIndex) }
                                    .padding(
                                        start = (16 + bookmark.depth * 16).dp,
                                        end = 16.dp,
                                        top = 12.dp,
                                        bottom = 12.dp
                                    ),
                                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = bookmark.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                // Show page label or page number
                                val pageLabel = bookmark.pageLabel.ifEmpty { 
                                    if (bookmark.pageIndex >= 0) (bookmark.pageIndex + 1).toString() else ""
                                }
                                if (pageLabel.isNotEmpty()) {
                                    Text(
                                        text = pageLabel,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(start = 8.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PdfViewerContent(
    modifier: Modifier = Modifier, 
    uri: Uri? = null,
    password: String? = null,
    savedState: PdfViewState? = null,
    isVerticalScroll: Boolean = true,
    onError: (Throwable) -> Unit = {},
    onTocLoaded: (List<PdfBookmark>) -> Unit = {},
    onPdfViewReady: (PDFView) -> Unit = {},
    onStateRestored: () -> Unit = {},
    defaultPage: Int = 0,
    onPageChange: (page: Int, pageCount: Int) -> Unit = { _, _ -> },
    onPageCountLoaded: (Int) -> Unit = {},
    onTap: () -> Unit = {},
    enableSnap: Boolean = false,
    nightMode: Boolean = false,
    searchResults: Map<Int, List<android.graphics.RectF>> = emptyMap(),
    currentSearchMatchIndex: Int = -1,
    onTextSelected: (String, Int) -> Unit = { _, _ -> },
    onSelectionCleared: () -> Unit = {}
) {
    var pdfView: PDFView? by remember { mutableStateOf(null) }
    // Track loaded state to avoid reloading on recomposition
    var loadedUri by remember { mutableStateOf<Uri?>(null) }
    var loadedPassword by remember { mutableStateOf<String?>(null) }
    var loadedVertical by remember { mutableStateOf(true) }
    var loadedSnap by remember { mutableStateOf(false) }
    var currentLoadedPage by remember { mutableStateOf(defaultPage) }
    
    val primaryColor = MaterialTheme.colorScheme.primary.toArgb()
    val onPrimaryColor = MaterialTheme.colorScheme.onPrimary.toArgb()
    val surfaceContainerColor = MaterialTheme.colorScheme.surfaceContainer.toArgb()
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface.toArgb()
    
    val selectionHandleColor = MaterialTheme.colorScheme.primary.toArgb()
    val selectionHighlightColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f).toArgb()
    
    // Highlight Paint for search (Normal)
    val highlightPaint = remember {
        android.graphics.Paint().apply {
            color = android.graphics.Color.YELLOW
            alpha = 100 // Transparent yellow
            style = android.graphics.Paint.Style.FILL
        }
    }
    
    // Highlight Paint for CURRENT search match
    val highlightCurrentPaint = remember {
        android.graphics.Paint().apply {
            color = android.graphics.Color.parseColor("#FF9800") // Orange
            alpha = 150 // Slightly more opaque
            style = android.graphics.Paint.Style.FILL
        }
    }
    
    // Pre-calculate start indices for each page to map global index to page-local index
    val pageStartIndices = remember(searchResults) {
        val map = mutableMapOf<Int, Int>()
        var count = 0
        searchResults.keys.sorted().forEach { page ->
            map[page] = count
            count += searchResults[page]?.size ?: 0
        }
        map
    }

    AndroidView(
        modifier = modifier,
        factory = { context ->
            PDFView(context, null).also { 
                pdfView = it
                onPdfViewReady(it)
            }
        },
        update = { view ->
            pdfView = view
            // Apply Scroll Mode Settings
            view.setPageSnap(enableSnap)
            view.setPageFling(enableSnap) // Enable fling only if snapping is enabled (Paging Mode)
            
            onPdfViewReady(view)
            
            // Search Highlights
            // We use onDrawAll to draw highlights
            val drawListener = object : com.hyntix.pdf.viewer.listener.OnDrawListener {
                override fun onLayerDrawn(canvas: android.graphics.Canvas, pageWidth: Float, pageHeight: Float, displayedPage: Int) {
                     val pdfFile = view.pdfFile ?: return
                     
                     // Draw search highlights
                     val matches = searchResults[displayedPage]
                     val startIndex = pageStartIndices[displayedPage] ?: -1
                     
                     if (matches != null) {
                         matches.forEachIndexed { index, rect ->
                             // Determine if this is the current match
                             val isCurrent = (startIndex >= 0) && ((startIndex + index) == currentSearchMatchIndex)
                             val paint = if (isCurrent) highlightCurrentPaint else highlightPaint
                             
                             // Map PDF coords -> device coords
                             val deviceRect = pdfFile.mapRectToDevice(
                                 displayedPage,
                                 0, 0, // startX, startY
                                 pageWidth.toInt(), pageHeight.toInt(), // scaled page size
                                 rect
                             )
                             canvas.drawRect(deviceRect, paint)
                         }
                     }
                }
            }
            view.callbacks.onDrawAll = drawListener

            // Selection Listeners
            view.callbacks.onTextSelected = object : com.hyntix.pdf.viewer.listener.OnTextSelectedListener {
                override fun onTextSelected(text: String, rects: List<android.graphics.RectF>, page: Int) {
                    onTextSelected(text, page)
                }

                override fun onSelectionCleared() {
                    onSelectionCleared()
                }
            }
            
            // Trigger redraw when search results change (including clear)
            view.invalidate()
            
            // Update settings if changed
            if (view.isSwipeVertical != isVerticalScroll) {
                // If scroll direction changes, we might need closer management, 
                // but usually the library handles it via configuration or reload.
                // Assuming simple update for now, but often requires reload for scroll direction change.
            }

            if (loadedUri != uri || loadedPassword != password || loadedVertical != isVerticalScroll || loadedSnap != enableSnap) {
                // CRITICAL: Capture current page from view BEFORE reload (fixes page reset on scroll direction change)
                val viewCurrentPage = view.currentPage
                
                loadedUri = uri
                loadedPassword = password
                loadedVertical = isVerticalScroll
                loadedSnap = enableSnap
                
                // Priority: savedState > viewCurrentPage (if doc loaded) > currentLoadedPage
                // This ensures we preserve the actual viewing position, not a stale listener value
                val pageToRestore = when {
                    savedState != null -> savedState.currentPage
                    view.pdfFile != null -> viewCurrentPage // Doc is loaded, use its actual page
                    else -> currentLoadedPage
                }
                
                // Load complete listener to extract TOC and restore state
                val loadListener = object : com.hyntix.pdf.viewer.listener.OnLoadCompleteListener {
                    override fun loadComplete(nbPages: Int) {
                        onTocLoaded(view.bookmarks)
                        onPageCountLoaded(nbPages)
                        // Restore state after load
                        savedState?.let { 
                            view.restoreState(it)
                            onStateRestored()
                        }
                    }
                }
                
                // Create page change listener
                val pageChangeListener = object : com.hyntix.pdf.viewer.listener.OnPageChangeListener {
                    override fun onPageChanged(page: Int, pageCount: Int) {
                        currentLoadedPage = page
                        onPageChange(page, pageCount)
                    }
                }
                
                // Create tap listener
                val tapListener = object : com.hyntix.pdf.viewer.listener.OnTapListener {
                    override fun onTap(e: android.view.MotionEvent): Boolean {
                        android.util.Log.d("PdfViewerDebug", "Tap detected at ${e.x}, ${e.y}")
                        onTap()
                        return true
                    }
                }
                
                // Create styled scroll handle - only for vertical scrolling, on right side
                val scrollHandle = if (isVerticalScroll) {
                    DefaultScrollHandle(view.context, false).apply { // false = right side (inverted=false)
                        background = android.graphics.drawable.GradientDrawable().apply {
                            setColor(surfaceContainerColor)
                            cornerRadius = 999f // Full capsule
                        }
                        setTextColor(onSurfaceColor)
                        setTextSize(12f)
                        val density = view.context.resources.displayMetrics.density
                        setPadding((12 * density).toInt(), (6 * density).toInt(), (12 * density).toInt(), (6 * density).toInt())
                    }
                } else {
                    null // No scroll handle for horizontal
                }
                
                if (uri != null) {
                    view.fromUri(uri)
                        .defaultPage(pageToRestore)
                        .nightMode(nightMode)
                        .enableAnnotationRendering(true)
                        .apply { if (scrollHandle != null) scrollHandle(scrollHandle) }
                        .spacing(10)
                        .swipeHorizontal(!isVerticalScroll)
                        .pageSnap(!isVerticalScroll || enableSnap) // Page snap only for horizontal OR when temporarily enabled
                        .password(password)
                        .onLoad(loadListener)
                        .onError(object : com.hyntix.pdf.viewer.listener.OnErrorListener {
                            override fun onError(t: Throwable) {
                                onError(t)
                            }
                        })
                        .onPageChange(pageChangeListener)
                        .onTap(tapListener)
                        .enableTextSelection(true) // Enable library text selection
                        .load()
                        
                    // Apply handle colors
                    view.setSelectionHandleColor(selectionHandleColor)
                    view.setSelectionHighlightColor(selectionHighlightColor)
                }

            }
        }
    )
}



@Composable
fun FloatingSelectionPanel(
    isVisible: Boolean,
    onCopy: () -> Unit,
    onSelectAll: () -> Unit,
    onWebSearch: () -> Unit,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter
    ) {
        androidx.compose.animation.AnimatedVisibility(
            visible = isVisible,
            enter = slideInVertically(initialOffsetY = { it }) + androidx.compose.animation.fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + androidx.compose.animation.fadeOut(),
            modifier = Modifier
                .padding(bottom = 32.dp) // Add bottom padding to stay above nav bar
        ) {
            Surface(
                shape = RoundedCornerShape(32.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                shadowElevation = 8.dp,
                modifier = Modifier
                    .height(64.dp)
                    .widthIn(max = 320.dp)
            ) {
                Row(
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceEvenly,
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    // Copy
                    IconButton(onClick = onCopy) {
                        Icon(
                            imageVector = PhosphorIcons.Regular.Copy,
                            contentDescription = "Copy",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Select All
                    IconButton(onClick = onSelectAll) {
                        Icon(
                            imageVector = PhosphorIcons.Regular.SelectionAll,
                            contentDescription = "Select All",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    
                    // Web Search
                    IconButton(onClick = onWebSearch) {
                        Icon(
                            imageVector = PhosphorIcons.Regular.Globe,
                            contentDescription = "Web Search",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    
                    // Vertical Divider
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(24.dp)
                            .background(MaterialTheme.colorScheme.outlineVariant)
                            .padding(horizontal = 8.dp)
                    )

                    // Close/Dismiss
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
