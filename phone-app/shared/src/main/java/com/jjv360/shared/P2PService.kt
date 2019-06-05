package com.jjv360.shared

import android.app.*
import android.content.Intent
import android.os.Build
import android.os.IBinder
import com.jjv360.ipfs.IPFS
import com.jjv360.ipfs.IPFSServer
import io.textile.pb.Model
import io.textile.pb.View
import io.textile.textile.Textile
import io.textile.textile.TextileEventListener
import nl.komponents.kovenant.task
import nl.komponents.kovenant.then
import java.lang.Exception
import java.util.logging.Logger

class P2PService : Service(), TextileEventListener {

    /** Static fields */
    companion object {

        /** Current instance, allows accessing the service in-process without going through the Binder interface */
        var singleton : P2PService? = null

        /** True if the textile instance has been inited */
        var hasInitedTextile = false

        /** Startup listeners */
        val startupListeners = mutableListOf<(Exception?) -> Unit>()

        /** Contact query listeners */
        val contactQueryListeners = mutableMapOf<String, (Model.Contact?, Exception?) -> Unit>()

    }

    /** The IPFS instance */
    var ipfs = IPFS(this)

    /** The PendingIntent to use to launch the main activity */
    private var pendingIntent : PendingIntent? = null

    /** Last error from Textile */
    private var textileError : Exception? = null

    /** True if textile has stopped itself */
    private var textileStopped = true

    /** The IPFS service that we receive connections on */
    private var service : IPFSServer? = null

    /** Called when someone tried to bind our service */
    override fun onBind(intent: Intent?): IBinder? {

        // We don't support binding
        return null

    }

    /** Called when someone tries to start our service with an Intent */
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        // Fetch pending intent from the Intent and store it
        pendingIntent = intent?.getParcelableExtra("launch-intent")
        updateNotification()

        // We want to stay alive
        return START_STICKY

    }

    override fun onCreate() {
        System.out.println("P2PService: Starting")

        // Start IPFS daemon
        task {

            // Register P2P service
            service = ipfs.createService("/x/chronobuddy-sync")
            Logger.getLogger("P2PService").info("Service ${service?.serviceName} started, requests coming in on port ${service?.serverSocket?.localPort}")

        }

        // Store instance
        singleton = this

        // Check which version of the notification to use
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            // Create notification channel
            val channel = NotificationChannel("p2p-service-channel", "P2P Service", NotificationManager.IMPORTANCE_LOW)
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)

            // Become a foreground service
            startForeground(1, Notification.Builder(this, "p2p-service-channel")
                .setContentTitle("Connected to network")
                .setContentText("You are currently connected to the P2P network.")
                .setSmallIcon(R.drawable.ic_p2p_icon)
                .setTicker("Connected to network.")
                .setContentIntent(pendingIntent)
                .build())

        } else {

            // Become a foreground service
            startForeground(1, Notification.Builder(this)
                .setContentTitle("Connecting...")
                .setContentText("Attempting to connect to the peer-to-peer network...")
                .setSmallIcon(R.drawable.ic_p2p_icon)
                .setTicker("Connecting to the network...")
                .setContentIntent(pendingIntent)
                .build())

        }

        // Start Textile instance if needed
//        if (!hasInitedTextile) Textile.initialize(this.applicationContext, true, false)
//        hasInitedTextile = true

        // Add listener
        Textile.instance().addEventListener(this)

    }

    /** Called when the system is destroying our service */
    override fun onDestroy() {

        // Remove instance
        singleton = null

        // Remove listener
        Textile.instance().removeEventListener(this)

    }

    /** Updates the Foreground notification with the current state of the network */
    private fun updateNotification() {

        // Get notification manager
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        // Create builder
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, "p2p-service-channel")
        } else {
            Notification.Builder(this)
        }

        // Check for initialization error
        if (textileError != null) {

            // Show failed
            notificationManager.notify(
                1, builder
                    .setSmallIcon(R.drawable.ic_p2p_icon)
                    .setContentIntent(pendingIntent)
                    .setContentTitle("Connection failed")
                    .setContentText(textileError!!.localizedMessage)
                    .setTicker("Unable to connect to your watch.")
                    .build()
            )

        } else if (textileStopped) {

            // Show textile is suspended
            notificationManager.notify(
                1, builder
                    .setSmallIcon(R.drawable.ic_p2p_icon)
                    .setContentIntent(pendingIntent)
                    .setContentTitle("Connection suspended")
                    .setContentText("The peer-to-peer connection is sleeping.")
                    .setTicker("Connection suspended.")
                    .build()
            )

        } else {

            // Show normal
            notificationManager.notify(1, builder
                .setSmallIcon(R.drawable.ic_p2p_icon)
                .setContentIntent(pendingIntent)
                .setContentTitle("Connected to watch")
                .setContentText("Notifications are being forwarded to your watch.")
                .setTicker("Connected to watch.")
                .build())

        }

    }

    override fun threadRemoved(threadId: String?) {}
    override fun accountPeerAdded(peerId: String?) {}
    override fun nodeStarted() {
        System.out.println("P2PService: Started")

        // Update UI
        textileStopped = false
        textileError = null
        updateNotification()

        // Notify listeners
        startupListeners.forEach {
            it(null)
        }

        // Clear listeners
        startupListeners.clear()

    }
    override fun clientThreadQueryResult(queryId: String?, thread: Model.Thread?) {}
    override fun willStopNodeInBackgroundAfterDelay(seconds: Int) {}
    override fun queryDone(queryId: String?) {}
    override fun threadAdded(threadId: String?) {}
    override fun nodeFailedToStop(e: Exception?) {
        textileError = e
        updateNotification()
    }
    override fun queryError(queryId: String?, e: Exception?) {

        // Call callback
        val callback = contactQueryListeners.get(queryId)
        if (callback != null)
            callback(null, e)

        // Remove it
        contactQueryListeners.remove(queryId)

    }
    override fun nodeOnline() {
        textileStopped = false
        textileError = null
        updateNotification()
    }
    override fun notificationReceived(notification: Model.Notification?) {}
    override fun accountPeerRemoved(peerId: String?) {}
    override fun contactQueryResult(queryId: String?, contact: Model.Contact?) {

        // Call callback
        val callback = contactQueryListeners.get(queryId)
        if (callback != null)
            callback(contact, null)

        // Remove it
        contactQueryListeners.remove(queryId)

    }
    override fun nodeFailedToStart(e: Exception?) {
        System.out.println("P2PService: Failed to start")

        // Update UI
        textileError = e
        updateNotification()

        // Notify listeners
        startupListeners.forEach {
            it(e)
        }

        // Clear listeners
        startupListeners.clear()

    }
    override fun canceledPendingNodeStop() {
        textileError = null
        updateNotification()
    }
    override fun nodeStopped() {
        textileStopped = true
        updateNotification()
    }
    override fun threadUpdateReceived(feedItem: View.FeedItem?) {}

}
