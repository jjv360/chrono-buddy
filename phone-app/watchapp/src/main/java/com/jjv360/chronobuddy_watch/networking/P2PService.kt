package com.jjv360.chronobuddy_watch.networking

import android.app.*
import android.content.Intent
import android.os.Build
import android.os.IBinder
import com.jjv360.chronobuddy_watch.MainActivity
import com.jjv360.chronobuddy_watch.R
import com.jjv360.ipfs.IPFS
import com.jjv360.ipfs.IPFSServer
import nl.komponents.kovenant.task
import java.util.logging.Logger

class P2PService : Service() {

    /** Static fields */
    companion object {

        /** Current instance, allows accessing the service in-process without going through the Binder interface */
        var singleton: P2PService? = null

    }

    /** The IPFS instance */
    var ipfs = IPFS(this)

    /** The IPFS service that we receive connections on */
    private var service : IPFSServer? = null

    /** Called when someone tried to bind our service */
    override fun onBind(intent: Intent?): IBinder? {

        // We don't support binding
        return null

    }

    /** Called when someone tries to start our service with an Intent */
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        // We want to stay alive
        return START_STICKY

    }

    override fun onCreate() {

        // Store singleton
        singleton = this

        // Start promise chain
        task {

            // Create watch peer-to-peer service
            service = ipfs.createService("/x/chronobuddy-sync")
            Logger.getLogger("P2PService").info("Service ${service?.serviceName} started, requests coming in on port ${service?.serverSocket?.localPort}")

        }

        // Check which version of the notification to use
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            // Create notification channel
            val channel = NotificationChannel("p2p-service-channel", "P2P Service", NotificationManager.IMPORTANCE_LOW)
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)

        }

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
        builder.setSmallIcon(R.drawable.ic_launcher_foreground)

        // Become a foreground service by registering a user-visible notification
        startForeground(1, builder
            .setContentTitle("Connected to phone")
            .setContentText("Notifications are being received from your phone.")
            .setTicker("Connected to phone.")
            .build())

    }

}