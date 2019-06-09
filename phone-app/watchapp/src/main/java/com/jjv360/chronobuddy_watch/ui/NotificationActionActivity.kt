package com.jjv360.chronobuddy_watch.ui

import android.app.Activity
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import com.google.android.flexbox.FlexboxLayout
import com.jjv360.chronobuddy_watch.R
import com.jjv360.chronobuddy_watch.networking.P2PService
import nl.komponents.kovenant.then
import java.util.logging.Logger

class NotificationActionActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Load layout
        setContentView(R.layout.activity_notification_fullscreenaction)

        // Setup fields
        findViewById<TextView>(R.id.msgtitle).text = intent.getStringExtra("title")
        findViewById<ImageView>(R.id.appicon).setImageBitmap(intent.getParcelableExtra("icon"))

        // Setup action buttons
        intent.getStringArrayExtra("actions").forEach { actionTitle ->

            // Create button
            val btn = Button(this, null, android.R.style.Widget_Material_Button_Borderless)
            btn.text = actionTitle
            btn.textSize = 26f
            btn.setPadding(20, 20, 20, 20)
            findViewById<FlexboxLayout>(R.id.actioncontainer).addView(btn)
            btn.setOnClickListener {

                // Send dismiss event
                P2PService.whenReady then {
                    it.rpc!!.call("notification-action", mapOf(
                        "id" to intent.getStringExtra("id"),
                        "action" to actionTitle
                    )) fail {
                        Logger.getLogger("actions").warning("Unable to send user's action response to the phone: ${it.localizedMessage}")
                    }
                }

                // Close this activity
                setResult(RESULT_OK)
                finish()

            }

        }

        // Setup dismiss button
        val btn = Button(this, null, android.R.style.Widget_Material_Button_Borderless)
        btn.text = "Dismiss"
        btn.textSize = 26f
        btn.setPadding(20, 20, 20, 20)
        findViewById<FlexboxLayout>(R.id.actioncontainer).addView(btn)
        btn.setOnClickListener {

            // Send dismiss event
            P2PService.whenReady then {
                it.rpc!!.call("notification-action", mapOf(
                    "id" to intent.getStringExtra("id"),
                    "action" to "Dismiss"
                )) fail {
                    Logger.getLogger("actions").warning("Unable to send user's action response to the phone: ${it.localizedMessage}")
                }
            }

            // Close this activity
            setResult(RESULT_OK)
            finish()

        }

    }

}