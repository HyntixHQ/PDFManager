package com.hyntix.android.pdfmanager.ui.viewer.reflow

import com.hyntix.pdf.viewer.PdfFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object ReflowHelper {

    suspend fun extractCleanedText(pdfFile: PdfFile, pageIndex: Int): String = withContext(Dispatchers.IO) {
        if (pageIndex < 0 || pageIndex >= pdfFile.pagesCount) {
            return@withContext ""
        }

        try {
            // Use PdfFile's thread-safe getPageText method
            val rawText = pdfFile.getPageText(pageIndex)
            cleanText(rawText)
        } catch (e: Exception) {
            e.printStackTrace()
            "Error extracting text from page ${pageIndex + 1}"
        }
    }

    private fun cleanText(rawText: String): String {
        if (rawText.isBlank()) return "No text found on this page."

        val lines = rawText.split('\n', '\r')
        val processed = StringBuilder()

        for (i in lines.indices) {
            val line = lines[i].trim()
            if (line.isEmpty()) {
                // Preserve explicit paragraph breaks (double newlines mostly)
                if (processed.isNotEmpty() && !processed.endsWith("\n\n")) {
                    processed.append("\n\n")
                }
                continue
            }

            if (processed.isNotEmpty() && !processed.endsWith("\n\n") && !processed.endsWith(" ")) {
                val prev = processed.last()
                // Simple heuristic: Join lines unless the previous line ends with a period or current starts with bullet?
                // Actually, reliable pdf text extraction is hard. 
                // Let's stick to a safe default: Append space to join lines in a paragraph.
                // If the previous char was a hyphen, maybe remove it (word-breaking).
                if (prev == '-') {
                     // Delete hyphen and join
                     processed.deleteCharAt(processed.length - 1)
                } else {
                     processed.append(" ")
                }
            }

            processed.append(line)
        }

        return processed.toString().trim()
    }
}
