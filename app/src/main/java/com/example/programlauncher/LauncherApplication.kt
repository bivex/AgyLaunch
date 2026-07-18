package com.example.programlauncher

import android.app.Application

class LauncherApplication : Application() {
    lateinit var dependencyContainer: DependencyContainer
        private set

    override fun onCreate() {
        super.onCreate()
        dependencyContainer = DependencyContainer(this)
    }
}
