package com.hyntix.android.pdfmanager.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp

@Composable
fun MarkdownText(
    markdown: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        val lines = markdown.lines()
        
        lines.forEach { rawLine ->
            val line = rawLine.trim()
            when {
                line.startsWith("### ") -> {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = processInlineStyles(line.removePrefix("### ")),
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                }
                line.startsWith("## ") -> {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = processInlineStyles(line.removePrefix("## ")),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                     Spacer(modifier = Modifier.height(4.dp))
                }
                line.startsWith("# ") -> {
                   Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = processInlineStyles(line.removePrefix("# ")),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                line.startsWith("* ") || line.startsWith("- ") -> {
                    val content = line.removePrefix("* ").removePrefix("- ")
                    Row(modifier = Modifier.padding(bottom = 4.dp, start = 8.dp)) {
                        Text(
                            text = "• ",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(end = 4.dp)
                        )
                        Text(
                            text = processInlineStyles(content),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                line == "---" -> {
                    Spacer(modifier = Modifier.height(8.dp))
                    androidx.compose.material3.HorizontalDivider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                line.isBlank() -> {
                    Spacer(modifier = Modifier.height(8.dp))
                }
                else -> {
                    Text(
                        text = processInlineStyles(line),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
            }
        }
    }
}

private fun processInlineStyles(text: String): androidx.compose.ui.text.AnnotatedString {
    return buildAnnotatedString {
        var cursor = 0
        // Simple regex for **bold** (simplified, doesn't handle nested/complex cases perfectly but enough for our legal text)
        val boldRegex = "\\*\\*(.*?)\\*\\*".toRegex()
        val matches = boldRegex.findAll(text)
        
        for (match in matches) {
            // Append text before match
            append(text.substring(cursor, match.range.first))
            
            // Append bold text
            withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                append(match.groupValues[1])
            }
            cursor = match.range.last + 1
        }
        
        // Append remaining text
        if (cursor < text.length) {
            append(text.substring(cursor))
        }
    }
}
