package com.hyntix.android.pdfmanager.ui.viewer.components

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.regular.*
import com.hyntix.pdfium.PdfAttachment
import com.hyntix.pdfium.PdfDocument
import com.hyntix.pdfium.PdfSignature
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Bottom sheet displaying document information, attachments, and signatures.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentInfoSheet(
    document: PdfDocument,
    fileName: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Info", "Attachments", "Signatures")
    
    // Load metadata
    val pageCount = remember { document.pageCount }
    val title = remember { document.title.ifBlank { fileName } }
    val author = remember { document.author }
    val subject = remember { document.subject }
    val creator = remember { document.creator }
    val creationDate = remember { document.creationDate }
    
    // Load attachments and signatures
    val attachments = remember { document.getAllAttachments() }
    val signatures = remember { document.getAllSignatures() }
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
        ) {
            // Title
            Text(
                text = "Document Info",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            
            // Tabs
            SecondaryTabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(title)
                                if (index == 1 && attachments.isNotEmpty()) {
                                    Spacer(Modifier.width(4.dp))
                                    Badge { Text("${attachments.size}") }
                                }
                                if (index == 2 && signatures.isNotEmpty()) {
                                    Spacer(Modifier.width(4.dp))
                                    Badge { Text("${signatures.size}") }
                                }
                            }
                        }
                    )
                }
            }
            
            // Content
            when (selectedTab) {
                0 -> InfoTab(
                    title = title,
                    author = author,
                    subject = subject,
                    creator = creator,
                    creationDate = creationDate,
                    pageCount = pageCount,
                    isSigned = signatures.isNotEmpty()
                )
                1 -> AttachmentsTab(
                    attachments = attachments,
                    document = document,
                    context = context,
                    onSave = { attachment ->
                        scope.launch {
                            saveAttachment(context, attachment)
                        }
                    }
                )
                2 -> SignaturesTab(signatures = signatures)
            }
        }
    }
}

@Composable
private fun InfoTab(
    title: String,
    author: String,
    subject: String,
    creator: String,
    creationDate: String,
    pageCount: Int,
    isSigned: Boolean
) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            InfoRow(label = "Title", value = title.ifBlank { "—" })
        }
        item {
            InfoRow(label = "Pages", value = pageCount.toString())
        }
        if (author.isNotBlank()) {
            item {
                InfoRow(label = "Author", value = author)
            }
        }
        if (subject.isNotBlank()) {
            item {
                InfoRow(label = "Subject", value = subject)
            }
        }
        if (creator.isNotBlank()) {
            item {
                InfoRow(label = "Creator", value = creator)
            }
        }
        if (creationDate.isNotBlank()) {
            item {
                InfoRow(label = "Created", value = formatPdfDate(creationDate))
            }
        }
        item {
            InfoRow(
                label = "Signed",
                value = if (isSigned) "Yes ✓" else "No"
            )
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f, fill = false),
            textAlign = androidx.compose.ui.text.style.TextAlign.End,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun AttachmentsTab(
    attachments: List<PdfAttachment>,
    document: PdfDocument,
    context: Context,
    onSave: (PdfAttachment) -> Unit
) {
    if (attachments.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    PhosphorIcons.Regular.Paperclip,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "No attachments",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(attachments) { attachment ->
                AttachmentItem(
                    attachment = attachment,
                    onSave = { onSave(attachment) }
                )
            }
        }
    }
}

@Composable
private fun AttachmentItem(
    attachment: PdfAttachment,
    onSave: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                PhosphorIcons.Regular.File,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = attachment.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                attachment.data?.let {
                    Text(
                        text = formatFileSize(it.size.toLong()),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            IconButton(onClick = onSave) {
                Icon(PhosphorIcons.Regular.DownloadSimple, contentDescription = "Save")
            }
        }
    }
}

@Composable
private fun SignaturesTab(signatures: List<PdfSignature>) {
    if (signatures.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    PhosphorIcons.Regular.ShieldCheck,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "No digital signatures",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(signatures) { signature ->
                SignatureItem(signature = signature)
            }
        }
    }
}

@Composable
private fun SignatureItem(signature: PdfSignature) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                PhosphorIcons.Regular.SealCheck,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Signature ${signature.index + 1}",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                if (signature.hasReason) {
                    Text(
                        text = "Reason: ${signature.reason}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (signature.hasSigningTime) {
                    Text(
                        text = "Signed: ${formatPdfDate(signature.signingTime)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// Helper functions

private fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        else -> "${bytes / (1024 * 1024)} MB"
    }
}

private fun formatPdfDate(pdfDate: String): String {
    // PDF dates: D:YYYYMMDDHHmmSS or similar
    return try {
        if (pdfDate.startsWith("D:") && pdfDate.length >= 10) {
            val year = pdfDate.substring(2, 6)
            val month = pdfDate.substring(6, 8)
            val day = pdfDate.substring(8, 10)
            "$day/$month/$year"
        } else {
            pdfDate
        }
    } catch (e: Exception) {
        pdfDate
    }
}

private suspend fun saveAttachment(context: Context, attachment: PdfAttachment) {
    withContext(Dispatchers.IO) {
        try {
            val data = attachment.data ?: return@withContext
            val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(
                android.os.Environment.DIRECTORY_DOWNLOADS
            )
            val file = File(downloadsDir, attachment.name)
            file.writeBytes(data)
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Saved to Downloads: ${attachment.name}", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Failed to save: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
