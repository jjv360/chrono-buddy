package com.jjv360.chronobuddy.networking

import android.app.*
import android.content.Intent
import android.os.Build
import android.os.IBinder
import com.jjv360.chronobuddy.ui.MainActivity
import nl.komponents.kovenant.deferred
import nl.komponents.kovenant.task

class P2PService : Service() {

    /** Static fields */
    companion object {

        /** Current instance, allows accessing the service in-process without going through the Binder interface */
        var singleton: P2PService? = null

        /** Startup promise */
        var startupPromise = deferred<P2PService, Exception>()
        var whenReady = startupPromise.promise

    }

    /** The IPFS instance */
//    var ipfs = IPFS(this)

    /** Called when someone tried to bind our service */
    override fun onBind(intent: Intent?): IBinder? {

        // We don't support binding
        return null

    }

    /** Called when someone tries to start our service with an Intent */
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        // Fetch pending intent from the Intent and store it
//        pendingIntent = intent?.getParcelableExtra("launch-intent")
//        updateNotification()

        // We want to stay alive
        return START_STICKY

    }

    override fun onCreate() {

        // Store singleton
        singleton = this

        // Start IPFS
        task {

            // Start IPFS
//            ipfs.start()

            // Done, inform anyone who's waiting for us to finish loading
            startupPromise.resolve(this)

        }

        // TODO: Load existing watch peer IDs and attempt to connect

        // Check which version of the notification to use
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            // Create notification channel
            val channel = NotificationChannel("p2p-service-channel", "P2P Service", NotificationManager.IMPORTANCE_LOW)
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)

        }

        // Become a foreground service by registering a user-visible notification
        startForeground(1, createNotification())

    }

    /** Get current notification describing the state of our service */
    fun createNotification() : Notification {

        // Get notification manager
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        // Create builder
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, "p2p-service-channel")
        } else {
            Notification.Builder(this)
        }

        // Create intent to launch when the user taps the notification
        val pendingIntent = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), 0)
        builder.setContentIntent(pendingIntent)

        // Set fields which don't change
        builder.setSmallIcon(com.jjv360.chronobuddy.R.drawable.ic_launcher_foreground)

        // Show normal
        return builder
            .setContentTitle("Connected to watch")
            .setContentText("Notifications are being forwarded to your watch.")
            .setTicker("Connected to watch.")
            .build()

    }

}