@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.hyntix.android.pdfmanager.ui.viewer.reflow

import androidx.compose.material3.TooltipAnchorPosition

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.regular.ArrowLeft
import com.adamglin.phosphoricons.regular.CaretLeft
import com.adamglin.phosphoricons.regular.CaretRight
import com.adamglin.phosphoricons.regular.TextT
import com.adamglin.phosphoricons.regular.Play
import com.adamglin.phosphoricons.regular.Pause
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hyntix.pdf.viewer.PdfFile
import com.hyntix.android.pdfmanager.data.repository.SettingsRepository
import androidx.activity.compose.BackHandler
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReflowScreen(
    pdfFile: PdfFile?,
    initialPage: Int,
    onNavigateBack: () -> Unit
) {
    // Handle system back button to close Reflow mode instead of navigating away
    BackHandler {
        onNavigateBack()
    }
    
    // Show loading if pdfFile is temporarily null (e.g. during config changes)
    if (pdfFile == null) {
        Box(
            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    val context = LocalContext.current
    val settingsRepository = remember { SettingsRepository(context) }
    val scope = rememberCoroutineScope()
    
    var currentPage by remember { mutableIntStateOf(initialPage) }
    var textContent by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var fontSize by remember { mutableFloatStateOf(16f) }
    var showFontControls by remember { mutableStateOf(false) }
    
    // Auto-scroll state
    var isAutoScrolling by remember { mutableStateOf(false) }
    var showSpeedSelector by remember { mutableStateOf(false) }
    val savedSpeed by settingsRepository.autoScrollSpeed.collectAsState(initial = 50)
    var sliderSpeed by remember { mutableFloatStateOf(savedSpeed.toFloat()) }
    
    // Update slider when saved speed loads
    LaunchedEffect(savedSpeed) {
        sliderSpeed = savedSpeed.toFloat()
    }
    
    // Scroll state for auto-scroll
    val scrollState = rememberScrollState()
    
    // Track the previous scroll position to detect user interaction
    var lastAutoScrollValue by remember { mutableIntStateOf(0) }

    // Load text when page changes
    LaunchedEffect(currentPage) {
        isLoading = true
        textContent = ReflowHelper.extractCleanedText(pdfFile, currentPage)
        isLoading = false
    }
    
    // Detect user scroll interaction - if scroll position changes unexpectedly, stop auto-scroll
    LaunchedEffect(scrollState.isScrollInProgress) {
        if (scrollState.isScrollInProgress && isAutoScrolling) {
            // User is manually scrolling, stop auto-scroll
            isAutoScrolling = false
        }
    }
    
    // Auto-scroll effect
    LaunchedEffect(isAutoScrolling, sliderSpeed) {
        if (isAutoScrolling) {
            // Speed: 1-100 mapped to 0.5-10 pixels per tick
            val pixelsPerTick = (sliderSpeed / 20f).coerceIn(0.5f, 5f).roundToInt().coerceAtLeast(1)
            while (isAutoScrolling) {
                val newValue = (scrollState.value + pixelsPerTick).coerceAtMost(scrollState.maxValue)
                scrollState.scrollTo(newValue)
                lastAutoScrollValue = newValue
                
                // Stop at bottom
                if (newValue >= scrollState.maxValue) {
                    isAutoScrolling = false
                }
                delay(16) // ~60fps
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reflow Mode") },
                navigationIcon = {
                    TooltipBox(
                        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
                        tooltip = { PlainTooltip { Text("Back to visual mode") } },
                        state = rememberTooltipState()
                    ) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(PhosphorIcons.Regular.ArrowLeft, contentDescription = "Back")
                        }
                    }
                },
                actions = {
                    // Auto-scroll toggle
                    TooltipBox(
                        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
                        tooltip = { PlainTooltip { Text(if (isAutoScrolling) "Stop Auto-Scroll" else "Start Auto-Scroll") } },
                        state = rememberTooltipState()
                    ) {
                        IconButton(onClick = { 
                            if (isAutoScrolling) {
                                isAutoScrolling = false
                            } else {
                                showSpeedSelector = true
                            }
                        }) {
                            Icon(
                                if (isAutoScrolling) PhosphorIcons.Regular.Pause else PhosphorIcons.Regular.Play,
                                contentDescription = if (isAutoScrolling) "Stop Auto-Scroll" else "Start Auto-Scroll",
                                tint = if (isAutoScrolling) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                    
                    // Font size toggle
                    TooltipBox(
                        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
                        tooltip = { PlainTooltip { Text("Adjust context font size") } },
                        state = rememberTooltipState()
                    ) {
                        IconButton(onClick = { showFontControls = !showFontControls }) {
                            Icon(PhosphorIcons.Regular.TextT, contentDescription = "Font Size")
                        }
                    }
                }
            )
        },
        bottomBar = {
            androidx.compose.material3.Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .height(56.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TooltipBox(
                        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
                        tooltip = { PlainTooltip { Text("Previous page") } },
                        state = rememberTooltipState()
                    ) {
                        IconButton(
                            onClick = { if (currentPage > 0) currentPage-- },
                            enabled = currentPage > 0
                        ) {
                            Icon(PhosphorIcons.Regular.CaretLeft, contentDescription = "Previous Page")
                        }
                    }
                    
                    Text(
                        text = "Page ${currentPage + 1} of ${pdfFile.pagesCount}",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    TooltipBox(
                        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
                        tooltip = { PlainTooltip { Text("Next page") } },
                        state = rememberTooltipState()
                    ) {
                        IconButton(
                            onClick = { if (currentPage < pdfFile.pagesCount - 1) currentPage++ },
                            enabled = currentPage < pdfFile.pagesCount - 1
                        ) {
                            Icon(PhosphorIcons.Regular.CaretRight, contentDescription = "Next Page")
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (showFontControls) {
                        FontSizeControl(
                            currentSize = fontSize,
                            onSizeChange = { fontSize = it }
                        )
                    }

                    SelectionContainer {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.surface)
                                .verticalScroll(scrollState)
                                .padding(16.dp)
                        ) {
                            Text(
                                text = textContent ?: "Failed to load text.",
                                fontSize = fontSize.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                lineHeight = (fontSize * 1.5).sp,
                                textAlign = TextAlign.Start
                            )
                        }
                    }
                }
            }
        }
    }
    
    // Speed Selector Dialog with Slider
    if (showSpeedSelector) {
        AlertDialog(
            onDismissRequest = { showSpeedSelector = false },
            title = { Text("Auto-Scroll Speed") },
            text = {
                Column(
                    modifier = Modifier.padding(vertical = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Slow", style = MaterialTheme.typography.bodySmall)
                        Text("Fast", style = MaterialTheme.typography.bodySmall)
                    }
                    Slider(
                        value = sliderSpeed,
                        onValueChange = { sliderSpeed = it },
                        valueRange = 1f..100f,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { 
                    scope.launch {
                        settingsRepository.setAutoScrollSpeed(sliderSpeed.roundToInt())
                    }
                    showSpeedSelector = false
                    isAutoScrolling = true
                }) {
                    Text("Start")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSpeedSelector = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun FontSizeControl(
    currentSize: Float,
    onSizeChange: (Float) -> Unit
) {
    Surface(
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("A-", modifier = Modifier.padding(end = 16.dp))
            Slider(
                value = currentSize,
                onValueChange = onSizeChange,
                valueRange = 12f..32f,
                steps = 10,
                modifier = Modifier.weight(1f)
            )
            Text("A+", modifier = Modifier.padding(start = 16.dp))
        }
    }
}
