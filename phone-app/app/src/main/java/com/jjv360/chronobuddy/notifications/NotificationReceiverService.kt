package com.jjv360.chronobuddy.notifications

import android.app.Notification.*
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Base64
import com.github.salomonbrys.kotson.nullString
import com.github.salomonbrys.kotson.obj
import com.jjv360.chronobuddy.networking.P2PService
import com.jjv360.shared.PubSub
import com.jjv360.shared.active
import com.jjv360.shared.onConnectionAdded
import com.jjv360.shared.open
import nl.komponents.kovenant.Promise
import nl.komponents.kovenant.then
import java.io.ByteArrayOutputStream
import java.util.logging.Logger

class NotificationReceiverService : NotificationListenerService() {

    // List of notifications we have already alerted
    var currentIDs = listOf<String>()

    /** True if connected */
    var isConnected = false

    init {

        // Listen for connections being added
        PubSub.onConnectionAdded {
            if (isConnected) setupRPC()
        }

    }

    override fun onListenerConnected() {
        super.onListenerConnected()

        // Forward to watch
        updateState()

        // Setup RPC listener
        isConnected = true
        setupRPC()

    }

    private fun setupRPC() {

        // Add RPC to all active connections
        PubSub.active.forEach {
            it.register("notification-action") {

                // Get notification
                val id = it.obj["id"]?.nullString ?: throw Exception("No ID specified")
                val notification = activeNotifications.find { it.key == id } ?: throw Exception("No notification found with key: $id")

                // Get action
                val actionTitle = it.obj["action"]?.nullString ?: throw Exception("No action specified")
                val action = notification.notification.actions?.find { it.title == actionTitle }

                // Check if we found a specific action
                if (action == null) {

                    // Just dismiss the notification
                    Logger.getLogger("notificationSvc").info("Dismissing notification ${notification.key}")
                    cancelNotification(notification.key)

                    // Check if it has a dismiss intent
                    if (notification.notification.deleteIntent != null)
                        notification.notification.deleteIntent.send()

                } else {

                    // Dismiss the notification if needed
                    Logger.getLogger("notificationSvc").info("Performing notification action ${action.title} on ${notification.key}")
                    if (notification.notification.flags and FLAG_AUTO_CANCEL != 0)
                        cancelNotification(notification.key)

                    // Found a specific action to perform, perform it
                    action.actionIntent?.send()

                }

                // Done
                Promise.of(true)

            }
        }

    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()

        // Cancel RPC
        isConnected = false
        PubSub.active.forEach {
            it.remove("notification-action")
        }

    }

    override fun onNotificationPosted(sbn: StatusBarNotification?, rankingMap: RankingMap?) {
        super.onNotificationPosted(sbn, rankingMap)

        // Forward to watch
        updateState()

    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?, rankingMap: RankingMap?, reason: Int) {
        super.onNotificationRemoved(sbn, rankingMap, reason)

        // Forward to watch
        updateState()

    }

    /** Send the current notifications to the watch */
    private fun updateState() {

        // Get current watch ID
        val prefs = getSharedPreferences("secret", Context.MODE_PRIVATE)
        val watchID = prefs.getString("watch-id", null) ?: ""
        if (watchID.isBlank())
            return

        // Get current notifications
        val infos = mutableListOf<Any>()
        val apps = mutableMapOf<String, Any>()
        var shouldNotify = false
        val newIDs = mutableListOf<String>()
        for (notification in activeNotifications) {

            // Skip unclearable notifications. These are typically long-running tasks, music player controls etc
            if (!notification.isClearable)
                continue

            // Skip message groups
            if (notification.notification.flags and FLAG_GROUP_SUMMARY != 0)
                continue

            // Get list of actions
            val actions = mutableListOf<Any>()
            notification.notification.actions?.forEach {

                // Add action info
                actions.add(mapOf(
                    "title" to it.title
                ))

            }

            // Add notification info
            val title = notification.notification.extras.get(EXTRA_TITLE)?.toString() ?: notification.notification.extras.get(EXTRA_TITLE_BIG)?.toString() ?: ""
            val text = notification.notification.extras.get(EXTRA_TEXT)?.toString() ?: notification.notification.extras.get(EXTRA_INFO_TEXT)?.toString() ?: ""
            val info = mutableMapOf<String, Any>(
                "id" to notification.key,
                "title" to title,
                "text" to text,
                "source" to notification.packageName,
                "actions" to actions
            )

            // Skip if this notification is blank
            if (title.isBlank() && text.isBlank()) {
                Logger.getLogger("notificationSvc").warning("Skipped blank notification from ${notification.packageName}")
                continue
            }

            // Add it
            infos.add(info)
            newIDs.add(notification.key)

            // Get app info if needed
            if (apps.get(notification.packageName) == null) {

                // Get app info
                val appInfo = packageManager.getApplicationInfo(notification.packageName, PackageManager.GET_UNINSTALLED_PACKAGES) ?: continue

                // Get app icon as a bitmap
                val desiredSize = 64
                val drawable = packageManager.getApplicationIcon(appInfo)
                val bitmap = if (drawable is BitmapDrawable && drawable.intrinsicWidth <= desiredSize && drawable.intrinsicHeight <= desiredSize) {

                    // Drawable is a bitmap already, and is already small enough, just extract it
                    drawable.bitmap

                } else {

                    // Create bitmap and draw drawable into it
                    val bmp = Bitmap.createBitmap(desiredSize, desiredSize, Bitmap.Config.ARGB_8888)
                    val canvas = Canvas(bmp)
                    drawable.setBounds(0, 0, canvas.width, canvas.height)
                    drawable.draw(canvas)

                    // Done
                    bmp

                }

                // Convert icon bitmap to PNG data
                val stream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                val iconStr = Base64.encodeToString(stream.toByteArray(), Base64.DEFAULT)

                // Get package
                apps[notification.packageName] = mapOf(
                    "label" to packageManager.getApplicationLabel(appInfo),
                    "icon" to iconStr
                )

            }

            // Check if the watch should alert, if this is a new notification
            if (!currentIDs.contains(notification.key))
                shouldNotify = true

        }

        // Store IDs
        currentIDs = newIDs

        // Send to watch
        PubSub.open("chronobuddy-sync", watchID).call("notifications", mapOf(
            "apps" to apps,
            "notifications" to infos,
            "alert" to shouldNotify
        )) success {
            Logger.getLogger("notificationSvc").info("Watch has received our ${infos.size} notifications.")
        } fail {
            Logger.getLogger("notificationSvc").warning("Unable to send notifications to the watch. ${it.localizedMessage}")
        }

    }

}