package com.blustream.app

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory

class BluStreamApplication : Application(), ImageLoaderFactory {
    override fun newImageLoader(): ImageLoader =
        ImageLoader.Builder(this)
            .allowHardware(false)
            .crossfade(false)
            .build()
}
