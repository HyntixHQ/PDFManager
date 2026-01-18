@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.hyntix.android.pdfmanager.ui.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Bottom sheet for creating a new folder.
 * First step: Enter folder name.
 */
@Composable
fun CreateFolderSheet(
    onDismiss: () -> Unit,
    onCreateFolder: (name: String) -> Unit,
    folderNameValidator: suspend (String) -> Boolean = { false } // Returns true if name already exists
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var folderName by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isChecking by remember { mutableStateOf(false) }
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "Create Folder",
                style = MaterialTheme.typography.headlineSmall
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            OutlinedTextField(
                value = folderName,
                onValueChange = { 
                    folderName = it
                    errorMessage = null
                },
                label = { Text("Folder name") },
                placeholder = { Text("Enter folder name") },
                singleLine = true,
                isError = errorMessage != null,
                supportingText = errorMessage?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Button(
                onClick = {
                    val trimmedName = folderName.trim()
                    when {
                        trimmedName.isEmpty() -> {
                            errorMessage = "Folder name cannot be empty"
                        }
                        trimmedName.length > 50 -> {
                            errorMessage = "Folder name is too long"
                        }
                        else -> {
                            onCreateFolder(trimmedName)
                        }
                    }
                },
                enabled = folderName.isNotBlank() && !isChecking,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (isChecking) "Checking..." else "Create")
            }
        }
    }
}
