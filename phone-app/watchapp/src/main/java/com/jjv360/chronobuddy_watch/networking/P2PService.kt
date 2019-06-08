package com.jjv360.chronobuddy_watch.networking

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.BatteryManager
import android.os.Build
import android.os.IBinder
import com.github.salomonbrys.kotson.nullArray
import com.github.salomonbrys.kotson.obj
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.jjv360.chronobuddy_watch.MainActivity
import com.jjv360.chronobuddy_watch.NotificationFullscreenActivity
import com.jjv360.chronobuddy_watch.R
import com.jjv360.shared.PubSub
import nl.komponents.kovenant.Promise
import nl.komponents.kovenant.deferred
import java.util.*

class P2PService : Service() {

    /** Static fields */
    companion object {

        /** Current instance, allows accessing the service in-process without going through the Binder interface */
        var singleton: P2PService? = null

        /** Startup promise */
        var startupPromise = deferred<P2PService, Exception>()
        var whenReady = startupPromise.promise

    }

    /** Our device ID */
    var deviceID = ""

    /** Called on startup to set our device ID */
    private fun getOrCreateDeviceID() : String {

        // Read from storage
        val prefs = getSharedPreferences("privateinfo", Context.MODE_PRIVATE)
        var id = prefs.getString("device-id", "") ?: ""
        if (id.isNotEmpty())
            return id

        // Not found, we need to create one
        id = UUID.randomUUID().toString().substring(0, 20)

        // Write to system preferences
        with (prefs.edit()) {
            putString("device-id", id)
            apply()
        }

        // Done
        return id

    }

    /** RPC manager */
    var rpc : PubSub? = null

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

        // Get device ID
        deviceID = getOrCreateDeviceID()

        // Create RPC channel
        rpc = PubSub("chronobuddy-sync", deviceID)

        // Store singleton
        singleton = this

        // Done, inform anyone who's waiting for us to finish loading
        startupPromise.resolve(this)

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
            .build())

        // Add a state endpoint
        rpc?.register("state") {

            // Get battery state
            val battery = getSystemService(Context.BATTERY_SERVICE) as BatteryManager
            var batteryState = "unknown"
            if (battery.getIntProperty(BatteryManager.BATTERY_PROPERTY_STATUS) == BatteryManager.BATTERY_STATUS_CHARGING)
                batteryState = "charging"
            else if (battery.getIntProperty(BatteryManager.BATTERY_PROPERTY_STATUS) == BatteryManager.BATTERY_STATUS_DISCHARGING)
                batteryState = "discharging"
            else if (battery.getIntProperty(BatteryManager.BATTERY_PROPERTY_STATUS) == BatteryManager.BATTERY_STATUS_FULL)
                batteryState = "full"
            else if (battery.getIntProperty(BatteryManager.BATTERY_PROPERTY_STATUS) == BatteryManager.BATTERY_STATUS_NOT_CHARGING)
                batteryState = "not_charging"

            // Get battery level
            val batteryLevel = battery.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY).toDouble() / 100

            // Return various information about the device
            Promise.of(mapOf(
                "batteryState" to batteryState,
                "batteryLevel" to batteryLevel
            ))

        }

        // Add a notification endpoint. This is called by the phone app to give us the latest list of notifications
        rpc?.register("notifications") {

            // Show notification activity
            // TODO: Don't encode back into JSON here
            val intent = Intent(this, NotificationFullscreenActivity::class.java)
            intent.putExtra("notifications", Gson().toJson(it))
            startActivity(intent)

            // Done
            Promise.of(true)

        }

    }

}