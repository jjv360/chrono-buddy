package com.jjv360.chronobuddy_watch

import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.ImageView
import com.github.sumimakito.awesomeqr.AwesomeQrRenderer
import com.github.sumimakito.awesomeqr.option.RenderOption
import com.github.sumimakito.awesomeqr.option.color.Color
import com.jjv360.shared.*
import nl.komponents.kovenant.ui.successUi

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Show loading UI
        setContentView(R.layout.activity_main_loading)

        // Start our P2P service
        val relaunchIntent = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), 0)
        P2PService.start(this, relaunchIntent)

    }

    override fun onResume() {
        super.onResume()

        // Check if paired
        P2PService.whenRunning successUi {

            // Check if should show the Pair screen or the main screen
            if (P2PService.companions.isEmpty())
                setContentView(R.layout.activity_main_pair)

        }

    }

    /** Called when the user presses the Pair button */
    fun onPairClick(view : View) {

        // Show QR code screen
        setContentView(R.layout.activity_main_paircode)

        // Generate a QR code
        // TODO: Make it look nice
        val renderOption = RenderOption()
        renderOption.content = P2PService.accountAddress
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

}
