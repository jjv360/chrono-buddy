package com.jjv360.chronobuddy_watch

import android.app.Application
import nl.komponents.kovenant.android.startKovenant
import nl.komponents.kovenant.android.stopKovenant

class MyApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Configure Kovenant with standard dispatchers suitable for an Android environment.
        startKovenant()

    }

    override fun onTerminate() {
        super.onTerminate()

        // Dispose of the Kovenant thread pools.
        stopKovenant()

    }

}