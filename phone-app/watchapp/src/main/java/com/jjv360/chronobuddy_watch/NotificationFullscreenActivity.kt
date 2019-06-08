package com.jjv360.chronobuddy_watch

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.LinearLayout.VERTICAL
import android.widget.ScrollView
import android.widget.TextView
import com.github.salomonbrys.kotson.*
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import java.io.ByteArrayInputStream
import java.util.logging.Logger

class NotificationFullscreenActivity : Activity() {

    // Cache app icon bitmaps
    val bitmapCache = mutableMapOf<String, Bitmap>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Update UI
        updateUI(intent)

    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)

        // Update UI
        updateUI(intent)

    }

    /** Load UI based on intent data */
    private fun updateUI(intent : Intent?) {

        // Fetch notifications
        // TODO: Show empty state
        val txt = intent?.getStringExtra("notifications") ?: return
        val json : JsonObject = Gson().fromJson(txt)

        // Get app information
        val apps = json["apps"].nullObj ?: JsonObject()

        // Get notifications
        val notifications = json["notifications"].nullArray ?: return
        if (notifications.size() == 0)
            return

        // Create scroll view
        val scroll = ScrollView(this)
        setContentView(scroll)

        // Create linear layout
        val linear = LinearLayout(this)
        linear.orientation = VERTICAL
        linear.setPadding(0, 0, 0, 80)
        scroll.addView(linear)

        // Create an entry for each notification
        for (notification in notifications) {

            // Load view
            val view = layoutInflater.inflate(R.layout.activity_notification_fullscreen, null)
            view.setPadding(0, 0, 0, 40)
            linear.addView(view)

            // Set fields
            view.findViewById<TextView>(R.id.msgtitle).text = notification.obj["title"]?.nullString ?: "(no title)"
            view.findViewById<TextView>(R.id.msgtext).text = notification.obj["text"]?.nullString ?: "(no text)"

            // Set app info
            val appPackageID = notification.obj["source"]?.nullString ?: ""
            val app = apps.getAsJsonObject(appPackageID) ?: JsonObject()
            view.findViewById<TextView>(R.id.appname).text = app["label"]?.asString ?: "(untitled app)"

            // Get bitmap
            val bitmap = getAppIcon(appPackageID, app["icon"]?.nullString ?: "")
            if (bitmap != null)
                view.findViewById<ImageView>(R.id.appicon).setImageBitmap(bitmap)

        }

    }

    /** Decode base64 app icon */
    private fun getAppIcon(pkgID : String, base64icon : String) : Bitmap? {

        try {

            // Get existing cached icon
            val cachedBitmap = bitmapCache[pkgID]
            if (cachedBitmap != null)
                return cachedBitmap

            // Decode base64 into bitmap
            val bytes = Base64.decode(base64icon, Base64.DEFAULT)
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)

            // Cache it
            bitmapCache[pkgID] = bitmap

            // Done
            Logger.getLogger("notificationActivity").info("Decoded app icon for $pkgID, file size is ${base64icon.length / 1024} KB")
            return bitmap

        } catch (err : Exception) {

            // Failed
            Logger.getLogger("notificationActivity").warning("Unable to decode app icon for $pkgID: ${err.localizedMessage}")
            return null

        }

    }

}