@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.hyntix.android.pdfmanager.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.regular.Folder
import com.adamglin.phosphoricons.regular.PencilSimple
import com.adamglin.phosphoricons.regular.Plus
import com.adamglin.phosphoricons.regular.Minus
import com.adamglin.phosphoricons.regular.Trash
import com.hyntix.android.pdfmanager.data.model.Folder

/**
 * Bottom sheet for managing a folder (long-press on folder chip).
 */
@Composable
fun FolderManagementSheet(
    folder: Folder,
    onDismiss: () -> Unit,
    onRename: () -> Unit,
    onAddPdfs: () -> Unit,
    onRemovePdfs: () -> Unit,
    onDelete: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            // Header with folder name
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 24.dp, bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = PhosphorIcons.Regular.Folder,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = folder.name,
                    style = MaterialTheme.typography.titleLarge
                )
            }
            
            HorizontalDivider()
            
            // Rename option
            ManagementOption(
                icon = PhosphorIcons.Regular.PencilSimple,
                label = "Rename folder",
                onClick = onRename  // Don't call onDismiss - sheet auto-hides via condition
            )
            
            // Add PDFs option
            ManagementOption(
                icon = PhosphorIcons.Regular.Plus,
                label = "Add PDFs",
                onClick = onAddPdfs
            )
            
            // Remove PDFs option
            ManagementOption(
                icon = PhosphorIcons.Regular.Minus,
                label = "Remove PDFs",
                onClick = onRemovePdfs
            )
            
            HorizontalDivider()
            
            // Delete option (destructive)
            ManagementOption(
                icon = PhosphorIcons.Regular.Trash,
                label = "Delete folder",
                isDestructive = true,
                onClick = onDelete
            )
        }
    }
}

@Composable
private fun ManagementOption(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isDestructive: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
        )
    }
}
