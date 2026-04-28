package com.thestudypath.pdfviewer

import android.graphics.Bitmap
import android.util.LruCache

/**
 * Process-scoped LruCache for PDF page-0 thumbnails.
 * Size = 1/8 of available heap, capped at 64 MB.
 */
object ThumbnailCache {
    private val maxBytes = minOf(
        Runtime.getRuntime().maxMemory() / 8,
        64L * 1024 * 1024
    ).toInt()

    private val cache = object : LruCache<String, Bitmap>(maxBytes) {
        override fun sizeOf(key: String, value: Bitmap) = value.byteCount
    }

    fun get(key: String): Bitmap? = cache.get(key)
    fun put(key: String, bitmap: Bitmap) { cache.put(key, bitmap) }
}

