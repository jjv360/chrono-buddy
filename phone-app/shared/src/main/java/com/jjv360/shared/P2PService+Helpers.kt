package com.jjv360.shared

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build

fun P2PService.Companion.start(context : Context, launchIntent : PendingIntent) {

    // Create the service intent
    val intent = Intent(context, P2PService::class.java)
    intent.putExtra("launch-intent", launchIntent)

    // Launch it
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        context.startForegroundService(intent)
    } else {
        context.startService(intent)
    }

}