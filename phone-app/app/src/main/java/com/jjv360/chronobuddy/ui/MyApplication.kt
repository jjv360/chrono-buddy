package com.jjv360.chronobuddy.ui

import android.app.Application
import nl.komponents.kovenant.android.startKovenant
import nl.komponents.kovenant.android.stopKovenant

class MyApplication : Application() {

    companion object {

        // When the user starts the app for the first time, this will contain their wallet recovery phrase.
        var walletRecoveryPhrase = ""

    }

    override fun onCreate() {
        super.onCreate()

        // Configure Kovenant with standard dispatchers suitable for an Android environment.
        startKovenant()

        // Initialize Textile
//        walletRecoveryPhrase = Textile.initialize(this, true, false)

    }

    override fun onTerminate() {
        super.onTerminate()

        // Dispose of the Kovenant thread pools.
        stopKovenant()

    }

}