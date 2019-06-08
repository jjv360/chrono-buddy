package com.jjv360.chronobuddy.notifications

import android.app.Notification.*
import android.content.Context
import android.content.pm.PackageManager
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.jjv360.shared.PubSub
import com.jjv360.shared.open
import java.util.logging.Logger

class NotificationReceiverService : NotificationListenerService() {

    override fun onListenerConnected() {
        super.onListenerConnected()

        // Forward to watch
        updateState()

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
        for (notification in activeNotifications) {

            // Skip unclearable notifications. These are typically long-running tasks, music player controls etc
            if (!notification.isClearable)
                continue

            // Add notification info
            val title = notification.notification.extras.get(EXTRA_TITLE)?.toString() ?: notification.notification.extras.get(EXTRA_TITLE_BIG)?.toString() ?: ""
            val text = notification.notification.extras.get(EXTRA_TEXT)?.toString() ?: notification.notification.extras.get(EXTRA_INFO_TEXT)?.toString() ?: ""
            val info = mutableMapOf<String, Any>(
                "id" to notification.key,
                "title" to title,
                "text" to text,
                "source" to notification.packageName
            )

            // Skip if this notification is blank
            if (title.isBlank() && text.isBlank()) {
                Logger.getLogger("notificationSvc").warning("Skipped blank notification from ${notification.packageName}")
            }

            // Add it
            infos.add(info)

            // Get app info if needed
            if (apps.get(notification.packageName) == null) {

                // Get app info
                val appInfo = packageManager.getApplicationInfo(notification.packageName, PackageManager.GET_UNINSTALLED_PACKAGES) ?: continue

                // Get package
                apps[notification.packageName] = mapOf(
                    "label" to packageManager.getApplicationLabel(appInfo)
                )

            }

        }

        // Send to watch
        Logger.getLogger("notificationSvc").info("Sending ${infos.size} notifications to the watch.")
        PubSub.open("chronobuddy-sync", watchID).call("notifications", mapOf(
            "apps" to apps,
            "notifications" to infos
        )) success {
            Logger.getLogger("notificationSvc").info("Watch has received our ${infos.size} notifications.")
        } fail {
            Logger.getLogger("notificationSvc").warning("Unable to send notifications to the watch. ${it.localizedMessage}")
        }

    }

}