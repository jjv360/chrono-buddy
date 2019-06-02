package com.jjv360.chronobuddy_watch

import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.Window
import android.view.WindowManager
import com.jjv360.shared.P2PService
import com.jjv360.shared.start

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Show loading UI
        setContentView(R.layout.activity_main_loading)

        // Start our P2P service
        val relaunchIntent = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), 0)
        P2PService.start(this, relaunchIntent)

    }
}
