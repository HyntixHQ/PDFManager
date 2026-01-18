@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.hyntix.android.pdfmanager.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.regular.Folder
import com.adamglin.phosphoricons.regular.Plus
import com.hyntix.android.pdfmanager.data.model.Folder

/**
 * Horizontal scrollable row of folder filter chips.
 * Contains "All" chip, user-created folder chips, and "+" button to add new.
 */
@Composable
fun FolderChipsRow(
    folders: List<Folder>,
    selectedFolderId: Long?,
    onSelectFolder: (Long?) -> Unit,
    onAddFolderClick: () -> Unit,
    onFolderLongPress: (Folder) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // "All" chip - always first
        item(key = "all") {
            FilterChip(
                selected = selectedFolderId == null,
                onClick = { onSelectFolder(null) },
                label = { Text("All") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
        
        // User-created folder chips
        items(
            items = folders,
            key = { folder -> folder.id }
        ) { folder ->
            FilterChip(
                selected = selectedFolderId == folder.id,
                onClick = { onSelectFolder(folder.id) },
                label = { Text(folder.name) },
                leadingIcon = {
                    Icon(
                        imageVector = PhosphorIcons.Regular.Folder,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
        
        // "+" button to add new folder
        item(key = "add") {
            AssistChip(
                onClick = onAddFolderClick,
                label = {
                    Icon(
                        imageVector = PhosphorIcons.Regular.Plus,
                        contentDescription = "Add folder",
                        modifier = Modifier.size(18.dp)
                    )
                }
            )
        }
    }
}

