package com.jjv360.chronobuddy_watch.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.*
import android.view.View.SYSTEM_UI_FLAG_FULLSCREEN
import android.view.WindowManager.LayoutParams.*
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.github.sumimakito.awesomeqr.AwesomeQrRenderer
import com.github.sumimakito.awesomeqr.option.RenderOption
import com.github.sumimakito.awesomeqr.option.color.Color
import com.google.gson.Gson
import com.jjv360.chronobuddy_watch.R
import com.jjv360.chronobuddy_watch.faces.Digital1
import com.jjv360.chronobuddy_watch.networking.P2PService
import nl.komponents.kovenant.then
import nl.komponents.kovenant.ui.promiseOnUi
import nl.komponents.kovenant.ui.successUi

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Show loading UI
        setContentView(R.layout.activity_main_loading)

        // Start our P2P service
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(Intent(this, P2PService::class.java))
        } else {
            startService(Intent(this, P2PService::class.java))
        }

    }

    override fun onResume() {
        super.onResume()

        // Wait for service to start
        P2PService.whenReady successUi {

            // Check if should show the Pair screen or the main screen
            val prefs = getPreferences(Context.MODE_PRIVATE)
            val hasPaired = prefs.getBoolean("has-paired", false)
            if (hasPaired)
                showFace()
            else
                setContentView(R.layout.activity_main_pair)

            // Register pair handler
            it.rpc?.register("pair") { promiseOnUi {

                // TODO: Confirm with the user

                // Update preferences
                val prefs = getPreferences(Context.MODE_PRIVATE)
                with (prefs.edit()) {
                    putBoolean("has-paired", true)
                    apply()
                }

                // Show the watch face
                showFace()

                // Done
                true

            }}

        }

    }

    override fun onPause() {
        super.onPause()

        // Remove pair request handler
        P2PService.whenReady then {
            it.rpc?.remove("pair")
        }

    }

    /** Called when the user presses the Pair button */
    fun onPairClick(view : View) {

        // Show QR code screen
        setContentView(R.layout.activity_main_paircode)

        // Create QR payload
        val json = Gson().toJson(mapOf(
            "action" to "pair",
            "mode" to "ws.in",
            "id" to P2PService.singleton?.deviceID
        ))

        // Generate a QR code
        // TODO: Make it look nice
        val renderOption = RenderOption()
        renderOption.content = json
        renderOption.roundedPatterns = true
        renderOption.borderWidth = 0
        renderOption.patternScale = 1f
        renderOption.color = Color()
        renderOption.color.dark = android.graphics.Color.BLACK
        renderOption.color.background = android.graphics.Color.WHITE
        val qr = AwesomeQrRenderer.render(renderOption)

        // Display QR code
        val qrContainer = findViewById<ImageView>(R.id.pair_qrcode_view)
        qrContainer.setImageBitmap(qr.bitmap)

    }

    /** Called when the user presses the Cancel button on the Pair QR screen */
    fun onPairCancelClick(view : View) {

        // Show pair screen again
        setContentView(R.layout.activity_main_pair)

    }

    /** Called when we want to replace the current UI with the user's selected watch face */
    fun showFace() {

        // Construct a new watch face view
        val face = Digital1(this)

        // Show it
        setContentView(face)

        // Add gesture recognizer to bring up the menu
        face.setOnClickListener {

            // Show menu
            startActivity(Intent(this, MenuActivity::class.java))

        }

    }

}
