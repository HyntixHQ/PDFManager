@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.hyntix.android.pdfmanager.ui.duplicates

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.regular.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DuplicateFinderScreen(
    onNavigateBack: () -> Unit,
    viewModel: DuplicateFinderViewModel = viewModel()
) {
    val scanState by viewModel.scanState.collectAsState()
    val selectedPaths by viewModel.selectedPaths.collectAsState()
    val isDeleting by viewModel.isDeleting.collectAsState()
    val context = LocalContext.current
    
    var showDeleteConfirm by remember { mutableStateOf(false) }
    
    // Start scan on first composition
    LaunchedEffect(Unit) {
        if (scanState is ScanState.Idle) {
            viewModel.startScan()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Find Duplicates") },
                navigationIcon = {
                    TooltipBox(
                        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
                        tooltip = { PlainTooltip { Text("Go back") } },
                        state = rememberTooltipState()
                    ) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(PhosphorIcons.Regular.ArrowLeft, "Back")
                        }
                    }
                },
                actions = {
                    if (scanState is ScanState.Complete) {
                        TooltipBox(
                            positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
                            tooltip = { PlainTooltip { Text("Scan for duplicates again") } },
                            state = rememberTooltipState()
                        ) {
                            IconButton(onClick = { viewModel.startScan() }) {
                                Icon(PhosphorIcons.Regular.ArrowsClockwise, "Rescan")
                            }
                        }
                    }
                }
            )
        },
        bottomBar = {
            // Show action bar when items selected
            AnimatedVisibility(
                visible = selectedPaths.isNotEmpty(),
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .windowInsetsPadding(WindowInsets.navigationBars)
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                "${selectedPaths.size} files selected",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                formatSize(viewModel.getSelectedSize()),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        
                        TooltipBox(
                            positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
                            tooltip = { PlainTooltip { Text("Delete selected duplicates") } },
                            state = rememberTooltipState()
                        ) {
                            Button(
                                onClick = { showDeleteConfirm = true },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error
                                ),
                                enabled = !isDeleting
                            ) {
                                if (isDeleting) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        color = MaterialTheme.colorScheme.onError,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(PhosphorIcons.Regular.Trash, null, Modifier.size(18.dp))
                                }
                                Spacer(Modifier.width(8.dp))
                                Text("Delete")
                            }
                        }
                    }
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (val state = scanState) {
                is ScanState.Idle, is ScanState.Scanning -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator()
                        Spacer(Modifier.height(16.dp))
                        Text("Scanning for duplicates...")
                        Text(
                            "This may take a while",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                is ScanState.Complete -> {
                    if (state.groups.isEmpty()) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                PhosphorIcons.Regular.Files,
                                null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.height(16.dp))
                            Text("No duplicates found!", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Your PDF collection is clean",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            item {
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            Text(
                                                "${state.groups.size} duplicate groups",
                                                style = MaterialTheme.typography.titleMedium
                                            )
                                            Text(
                                                "Potential savings: ${formatSize(state.totalDuplicateSize)}",
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        }
                                    }
                                }
                            }
                            
                            items(state.groups, key = { it.hash }) { group ->
                                DuplicateGroupCard(
                                    group = group,
                                    selectedPaths = selectedPaths,
                                    onToggleSelection = { viewModel.toggleSelection(it) },
                                    onKeepNewest = { viewModel.selectAllExceptNewest(group) },
                                    onKeepOldest = { viewModel.selectAllExceptOldest(group) }
                                )
                            }
                        }
                    }
                }
                
                is ScanState.Error -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("Error: ${state.message}")
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = { viewModel.startScan() }) {
                            Text("Retry")
                        }
                    }
                }
            }
        }
    }
    
    // Delete confirmation dialog
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete ${selectedPaths.size} files?") },
            text = { 
                Text("This will permanently delete ${formatSize(viewModel.getSelectedSize())} of duplicate files. This action cannot be undone.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        viewModel.deleteSelected { count ->
                            Toast.makeText(context, "Deleted $count files", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun DuplicateGroupCard(
    group: DuplicateGroupUi,
    selectedPaths: Set<String>,
    onToggleSelection: (String) -> Unit,
    onKeepNewest: () -> Unit,
    onKeepOldest: () -> Unit
) {
    Card {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "${group.files.size} copies • ${formatSize(group.totalSize)}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                
                Row {
                    TooltipBox(
                        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
                        tooltip = { PlainTooltip { Text("Select all but the newest copy") } },
                        state = rememberTooltipState()
                    ) {
                        TextButton(onClick = onKeepNewest) {
                            Text("Keep Newest", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    
                    TooltipBox(
                        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
                        tooltip = { PlainTooltip { Text("Select all but the oldest copy") } },
                        state = rememberTooltipState()
                    ) {
                        TextButton(onClick = onKeepOldest) {
                            Text("Keep Oldest", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
            
            Spacer(Modifier.height(8.dp))
            
            group.files.forEach { file ->
                val isSelected = selectedPaths.contains(file.path)
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onToggleSelection(file.path) }
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                            else MaterialTheme.colorScheme.surface,
                            RoundedCornerShape(8.dp)
                        )
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        if (isSelected) PhosphorIcons.Regular.CheckSquare else PhosphorIcons.Regular.Square,
                        null,
                        tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Spacer(Modifier.width(12.dp))
                    
                    Icon(
                        PhosphorIcons.Regular.File,
                        null,
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                        modifier = Modifier.size(24.dp)
                    )
                    
                    Spacer(Modifier.width(12.dp))
                    
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            file.name,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            "${formatSize(file.size)} • ${formatDate(file.lastModified)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            file.path.substringBeforeLast("/"),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                
                if (file != group.files.last()) {
                    Spacer(Modifier.height(4.dp))
                }
            }
        }
    }
}

private fun formatSize(bytes: Long): String {
    return when {
        bytes >= 1_073_741_824 -> "%.2f GB".format(bytes / 1_073_741_824.0)
        bytes >= 1_048_576 -> "%.1f MB".format(bytes / 1_048_576.0)
        bytes >= 1024 -> "%.1f KB".format(bytes / 1024.0)
        else -> "$bytes B"
    }
}

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
