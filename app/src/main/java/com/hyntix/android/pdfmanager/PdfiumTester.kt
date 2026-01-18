package com.hyntix.android.pdfmanager

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.hyntix.pdfium.PdfiumCore
import com.hyntix.pdfium.PdfDocument
import java.io.File

object PdfiumTester {
    private const val TAG = "PdfiumTester"

    fun runTest(context: Context) {
        Log.d(TAG, "Starting Pdfium Test...")
        try {
            val core = PdfiumCore()
            core.initLibrary() // Ensure library is initialized
            
            // 1. Create a new Document
            Log.d(TAG, "Creating new document...")
            val doc = core.newDocument()
            if (doc == null) {
                Log.e(TAG, "Failed to create new document")
                return
            }
            Log.d(TAG, "New document created successfully")

            // 2. Add a page (A4 size: 595 x 842 points)
            Log.d(TAG, "Adding page...")
            val page = doc.addNewPage(0, 595.0, 842.0)
            Log.d(TAG, "Page added: ${page.width} x ${page.height}")
            page.close() 

            // 3. Save to file
            val file = File(context.cacheDir, "test_generated.pdf")
            Log.d(TAG, "Saving to ${file.absolutePath}...")
            val saved = doc.saveAs(file.absolutePath)
            Log.d(TAG, "Document saved: $saved")
            
            Log.d(TAG, "Closing write-doc...")
            doc.close()
            Log.d(TAG, "Write-doc closed.")

            if (!saved) {
                Log.e(TAG, "Failed to save document")
                return
            }

            // 4. Open the saved document to verify reading
            Log.d(TAG, "Opening saved document: ${file.absolutePath}")
            val readDoc: PdfDocument? = core.openDocument(file.absolutePath)
            if (readDoc == null) {
                Log.e(TAG, "Failed to open generated document")
                return
            }
            // Smart cast might fail if not local variable or mutable, but it is val.
            // Using safe call or !! to be sure
            Log.d(TAG, "Opened generated document. Page count: ${readDoc.pageCount}")

            // 5. Render the first page
            Log.d(TAG, "Opening page 0...")
            val readPage = readDoc.openPage(0)
            val width = readPage.width.toInt()
            val height = readPage.height.toInt()
            Log.d(TAG, "Page 0 opened. Size: $width x $height. Creating bitmap...")

            // Test Links
            val links = readPage.getLinks()
            Log.d(TAG, "Page 0 Links Count: ${links.size}")

            // Test Text
            val textPage = readPage.openTextPage()
            val charCount = textPage.charCount
            Log.d(TAG, "Page 0 Char Count: $charCount")

            // Test Search (even if empty)
            val searcher = textPage.search("test", matchCase = false, matchWholeWord = false)
            Log.d(TAG, "Search 'test' matches: ${searcher.size}")
            
            // Test Text Rects (if chars exist)
            if (charCount > 0) {
                val rects = textPage.getTextRects(0, 1)
                Log.d(TAG, "First char rects: ${rects.size}")
            }
            textPage.close()

            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            
            Log.d(TAG, "Rendering page...")
            readPage.render(bitmap)
            Log.d(TAG, "Rendered page to bitmap: ${bitmap.width} x ${bitmap.height}")
            
            readPage.close()
            readDoc.close()
            Log.d(TAG, "Pdfium Test COMPLETED SUCCESSFULLY")

        } catch (e: Exception) {
            Log.e(TAG, "Pdfium Test FAILED", e)
        }
    }
}
