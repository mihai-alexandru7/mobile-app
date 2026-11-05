package com.florilegia

import android.app.Application
import com.florilegia.di.AppContainer

class FlorilegiaApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}