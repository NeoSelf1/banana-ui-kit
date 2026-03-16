package com.neon.core.ui.image

import android.content.Context
import coil.ImageLoader
import coil.decode.SvgDecoder

object SvgImageLoader {
    @Volatile
    private var cachedImageLoader: ImageLoader? = null

    fun get(context: Context): ImageLoader {
        val existing = cachedImageLoader
        if (existing != null) return existing

        return ImageLoader.Builder(context.applicationContext)
            .components {
                add(SvgDecoder.Factory())
            }
            .build()
            .also { cachedImageLoader = it }
    }
}




