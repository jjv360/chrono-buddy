package com.jjv360.chronobuddy_watch

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.LinearLayout.VERTICAL
import android.widget.ScrollView
import android.widget.TextView
import com.github.salomonbrys.kotson.*
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject

class NotificationFullscreenActivity : Activity() {

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
        scroll.addView(linear)

        // Create an entry for each notification
        for (notification in notifications) {

            // Load view
            val view = layoutInflater.inflate(R.layout.activity_notification_fullscreen, null)
            linear.addView(view)

            // Set fields
            view.findViewById<TextView>(R.id.msgtitle)?.text = notification.obj["title"]?.nullString ?: "(no title)"
            view.findViewById<TextView>(R.id.msgtext)?.text = notification.obj["text"]?.nullString ?: "(no text)"

            // Set app info
            val app = apps.getAsJsonObject(notification.obj["source"]?.nullString ?: "") ?: JsonObject()
            view.findViewById<TextView>(R.id.appname)?.text = app["label"]?.asString ?: "(untitled app)"

        }

    }

}