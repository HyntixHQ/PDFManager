@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.hyntix.android.pdfmanager.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.regular.ArrowLeft
import com.adamglin.phosphoricons.regular.FilePdf
import com.adamglin.phosphoricons.regular.MagnifyingGlass
import com.adamglin.phosphoricons.regular.X
import com.hyntix.android.pdfmanager.data.model.PdfFile

/**
 * Full-screen composable for selecting PDFs to add/remove from a folder.
 */
@Composable
fun PdfSelectionScreen(
    title: String,
    allPdfs: List<PdfFile>,
    preSelectedUris: Set<String> = emptySet(),
    onDone: (selectedUris: Set<String>) -> Unit,
    onCancel: () -> Unit
) {
    var selectedUris by remember { mutableStateOf(preSelectedUris) }
    var searchQuery by remember { mutableStateOf("") }
    var searchActive by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    
    // Handle system back button
    BackHandler { 
        if (searchActive) {
            searchActive = false
            searchQuery = ""
        } else {
            onCancel() 
        }
    }
    
    val filteredPdfs by remember(allPdfs, searchQuery) {
        derivedStateOf {
            if (searchQuery.isBlank()) allPdfs
            else allPdfs.filter { it.name.contains(searchQuery, ignoreCase = true) }
        }
    }
    
    val selectedCount by remember(selectedUris) {
        derivedStateOf { selectedUris.size }
    }
    
    Scaffold(
        topBar = {
            if (searchActive) {
                // Request focus when search becomes active
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
                                    onValueChange = { searchQuery = it },
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
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(PhosphorIcons.Regular.X, "Clear")
                                }
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { 
                            searchActive = false
                            searchQuery = ""
                        }) {
                            Icon(PhosphorIcons.Regular.ArrowLeft, "Close search")
                        }
                    }
                )
            } else {
                TopAppBar(
                    title = { 
                        Text(
                            text = title,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onCancel) {
                            Icon(PhosphorIcons.Regular.ArrowLeft, "Cancel")
                        }
                    },
                    actions = {
                        // Search button
                        IconButton(onClick = { searchActive = true }) {
                            Icon(PhosphorIcons.Regular.MagnifyingGlass, "Search")
                        }
                        // Done button
                        TextButton(
                            onClick = { onDone(selectedUris) },
                            enabled = true
                        ) {
                            Text("Done ($selectedCount)")
                        }
                    }
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            HorizontalDivider()
            
            // PDF list
            if (filteredPdfs.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (searchQuery.isNotBlank()) "No PDFs match your search" else "No PDFs found",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(
                        items = filteredPdfs,
                        key = { it.uri }
                    ) { pdf ->
                        val isSelected = selectedUris.contains(pdf.uri)
                        val isPreSelected = preSelectedUris.contains(pdf.uri)
                        
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedUris = if (isSelected) {
                                        selectedUris - pdf.uri
                                    } else {
                                        selectedUris + pdf.uri
                                    }
                                }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = { checked ->
                                    selectedUris = if (checked) {
                                        selectedUris + pdf.uri
                                    } else {
                                        selectedUris - pdf.uri
                                    }
                                },
                                enabled = !isPreSelected // Disable if already in folder
                            )
                            
                            Icon(
                                imageVector = PhosphorIcons.Regular.FilePdf,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = pdf.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (isPreSelected) {
                                    Text(
                                        text = "Already in folder",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
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
