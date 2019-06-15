package com.jjv360.chronobuddy_watch.ui

import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.jjv360.chronobuddy_watch.R
import com.jjv360.chronobuddy_watch.networking.P2PService
import nl.komponents.kovenant.Promise
import nl.komponents.kovenant.then
import nl.komponents.kovenant.ui.failUi
import nl.komponents.kovenant.ui.successUi

class MusicControlActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Load UI
        setContentView(R.layout.activity_music)

    }

    override fun onResume() {
        super.onResume()

        // Wait for P2P service
        P2PService.whenReady then {

            // Create music RPC handler
            it.rpc!!.register("music-update") {

                System.out.println("" + it.toString())

                // Success
                Promise.of(true)

            }

            // Tell phone companion that we're ready to receive music updates
            it.rpc!!.call("start-music-updates").get()

        } successUi {

            // Music control is ready
            System.out.println("Phone is now sending music updates")

        } failUi {

            // Failed! Show alert
            // TODO: Show disconnected phone UI instead
            AlertDialog.Builder(this)
                .setTitle("Phone not connected")
                .setMessage(it.localizedMessage)
                .setNeutralButton("Close", null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setOnDismissListener {
                    finish()
                }
                .show()

        }

    }

    override fun onPause() {
        super.onPause()

        // Wait for P2P service
        P2PService.whenReady then {

            // Remove music RPC handler
            it.rpc!!.remove("music-update")

        }

    }

}