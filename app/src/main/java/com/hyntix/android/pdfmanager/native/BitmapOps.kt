package com.hyntix.android.pdfmanager.native

import android.graphics.Bitmap
import android.util.Log

/**
 * Native bitmap operations with SIMD acceleration.
 * Uses NEON intrinsics on ARM devices for 4-8x faster operations.
 * Falls back to Java implementation if native library not available.
 */
object BitmapOps {
    
    private const val TAG = "BitmapOps"
    
    private var nativeAvailable = false
    
    init {
        try {
            System.loadLibrary("bitmap_ops")
            nativeAvailable = true
            Log.i(TAG, "Native bitmap ops loaded successfully")
        } catch (e: UnsatisfiedLinkError) {
            nativeAvailable = false
            Log.i(TAG, "Native bitmap ops not available, using Java fallback")
        }
    }
    
    /**
     * Check if native operations are available.
     */
    fun isAvailable(): Boolean = nativeAvailable
    
    /**
     * Invert bitmap colors (for night mode).
     * Uses NEON SIMD on supported devices for 4x speedup.
     */
    fun invertBitmap(bitmap: Bitmap) {
        if (nativeAvailable && bitmap.config == Bitmap.Config.ARGB_8888) {
            try {
                val startTime = System.nanoTime()
                invertBitmapNeon(bitmap)
                val elapsed = (System.nanoTime() - startTime) / 1_000_000.0
                Log.d(TAG, "Native invert: ${bitmap.width}x${bitmap.height} in ${elapsed}ms")
            } catch (e: Exception) {
                Log.w(TAG, "Native invert failed, using fallback", e)
                invertBitmapJava(bitmap)
            }
        } else {
            invertBitmapJava(bitmap)
        }
    }
    
    /**
     * Convert bitmap to grayscale.
     * Uses NEON SIMD on supported devices.
     */
    fun grayscaleBitmap(bitmap: Bitmap) {
        if (nativeAvailable && bitmap.config == Bitmap.Config.ARGB_8888) {
            try {
                grayscaleBitmapNeon(bitmap)
            } catch (e: Exception) {
                Log.w(TAG, "Native grayscale failed, using fallback", e)
                grayscaleBitmapJava(bitmap)
            }
        } else {
            grayscaleBitmapJava(bitmap)
        }
    }
    
    /**
     * Apply sepia tone to bitmap.
     * Uses NEON SIMD on supported devices.
     */
    fun sepiaBitmap(bitmap: Bitmap) {
        if (nativeAvailable && bitmap.config == Bitmap.Config.ARGB_8888) {
            try {
                sepiaBitmapNeon(bitmap)
            } catch (e: Exception) {
                Log.w(TAG, "Native sepia failed, using fallback", e)
                sepiaBitmapJava(bitmap)
            }
        } else {
            sepiaBitmapJava(bitmap)
        }
    }
    
    // Native methods (implemented in C++ with NEON intrinsics)
    private external fun invertBitmapNeon(bitmap: Bitmap)
    private external fun grayscaleBitmapNeon(bitmap: Bitmap)
    private external fun sepiaBitmapNeon(bitmap: Bitmap)
    
    /**
     * Java fallback: Invert bitmap colors.
     */
    private fun invertBitmapJava(bitmap: Bitmap) {
        if (!bitmap.isMutable) {
            Log.w(TAG, "Cannot invert immutable bitmap")
            return
        }
        val startTime = System.nanoTime()
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        
        for (i in pixels.indices) {
            val color = pixels[i]
            val a = color and 0xFF000000.toInt()
            val r = 255 - ((color shr 16) and 0xFF)
            val g = 255 - ((color shr 8) and 0xFF)
            val b = 255 - (color and 0xFF)
            pixels[i] = a or (r shl 16) or (g shl 8) or b
        }
        
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        val elapsed = (System.nanoTime() - startTime) / 1_000_000.0
        Log.d(TAG, "Java invert: ${width}x${height} in ${elapsed}ms")
    }
    
    /**
     * Java fallback: Convert to grayscale.
     * Uses weighted luminance: R*0.299 + G*0.587 + B*0.114
     */
    private fun grayscaleBitmapJava(bitmap: Bitmap) {
        if (!bitmap.isMutable) {
            Log.w(TAG, "Cannot grayscale immutable bitmap")
            return
        }
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        
        for (i in pixels.indices) {
            val color = pixels[i]
            val a = color and 0xFF000000.toInt()
            val r = (color shr 16) and 0xFF
            val g = (color shr 8) and 0xFF
            val b = color and 0xFF
            // Weighted luminance (scaled by 256)
            val gray = (r * 77 + g * 150 + b * 29) shr 8
            pixels[i] = a or (gray shl 16) or (gray shl 8) or gray
        }
        
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
    }
    
    /**
     * Java fallback: Apply sepia tone.
     */
    private fun sepiaBitmapJava(bitmap: Bitmap) {
        if (!bitmap.isMutable) {
            Log.w(TAG, "Cannot apply sepia to immutable bitmap")
            return
        }
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        
        for (i in pixels.indices) {
            val color = pixels[i]
            val a = color and 0xFF000000.toInt()
            val r = (color shr 16) and 0xFF
            val g = (color shr 8) and 0xFF
            val b = color and 0xFF
            
            // Sepia transformation
            val newR = ((r * 0.393 + g * 0.769 + b * 0.189).toInt()).coerceAtMost(255)
            val newG = ((r * 0.349 + g * 0.686 + b * 0.168).toInt()).coerceAtMost(255)
            val newB = ((r * 0.272 + g * 0.534 + b * 0.131).toInt()).coerceAtMost(255)
            
            pixels[i] = a or (newR shl 16) or (newG shl 8) or newB
        }
        
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
    }
}
