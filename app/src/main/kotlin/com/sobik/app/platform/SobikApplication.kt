package com.sobik.app.platform

import android.app.Application
import com.sobik.app.AppContainer

class SobikApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(filesDir, AssetCatalogFileSystem(assets), AssetImageLoader(assets))
    }
}
