package com.tkm.basic

import android.app.Application
import android.content.Context

lateinit var AppContext: Context

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        AppContext = this
    }
}