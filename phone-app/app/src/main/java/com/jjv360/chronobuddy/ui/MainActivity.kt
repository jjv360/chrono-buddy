package com.jjv360.chronobuddy.ui

import android.app.AlertDialog
import android.app.PendingIntent
import android.app.ProgressDialog
import android.content.Intent
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import com.github.salomonbrys.kotson.fromJson
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.ivan200.photobarcodelib.PhotoBarcodeScannerBuilder
import com.jjv360.chronobuddy.R
import com.jjv360.chronobuddy.networking.P2PService
import khttp.post
import nl.komponents.kovenant.Promise
import nl.komponents.kovenant.task
import nl.komponents.kovenant.then
import nl.komponents.kovenant.ui.failUi
import nl.komponents.kovenant.ui.promiseOnUi
import nl.komponents.kovenant.ui.successUi
import java.net.HttpURLConnection
import java.net.URL
import java.util.logging.Logger


class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Start our P2P service
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(Intent(this, P2PService::class.java))
        } else {
            startService(Intent(this, P2PService::class.java))
        }

    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {

        // Check which menu option was selected
        if (item?.itemId == R.id.mainmenu_pair) {

            // Start pairing
            pairWatch()

        }

        // Unknown menu item, let system deal with it
        return super.onOptionsItemSelected(item)

    }

    // Called by button onClick
    fun sendViaBluetooth() {



    }

    /** Called to pair a new watch */
    fun pairWatch() {

        // TODO: Ask for camera permission access

        // Scan QR code
        PhotoBarcodeScannerBuilder()
            .withActivity(this)
            .withText("Scan the QR code on the watch")
            .withOnlyQRCodeScanning()
            .withResultListener { barcode ->

                // Check result
                Logger.getLogger("MainActivity").info("Scanned pairing info " + barcode.rawValue)
                promiseOnUi {
                    pairWatchWithPayload(barcode.rawValue)
                }

            }
            .build()
            .start()

    }

    /** Called once a pairing payload has been scanned */
    fun pairWatchWithPayload(txt : String) {

        // Show progress dialog
        val dlg = ProgressDialog(this)
        dlg.setTitle("Pairing watch")
        dlg.setMessage("Connecting to device...")
        dlg.setCancelable(false)
        dlg.show()

        // Start task
        P2PService.whenReady then {

            // Decode the JSON
            val json = Gson().fromJson<Map<String, String>>(txt)

            // Check pairing mode
            val mode = json["mode"]
            if (mode == "ipfs") {

                // Extract peer ID
                val peerID = json["id"] ?: throw Exception("No peer ID found in the pair request.")

                // Connect to the remote device
                promiseOnUi { dlg.setMessage("Connecting via IPFS...") }

                // Pair with IPFS service
                pairWatchWithIPFS(peerID)

            } else {

                // Unknown pairing mode
                throw Exception("Unknown pairing mode '$mode'.")

            }

        } successUi {

            // Done, hide loader
            dlg.dismiss()

        } failUi {

            // Failed! Show alert
            dlg.dismiss()
            AlertDialog.Builder(this)
                .setTitle("Unable to Pair")
                .setMessage(it.localizedMessage)
                .setNeutralButton("Close", null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show()

        }

    }

    /** Pairs to the watch via an IPFS peer to peer channel */
    private fun pairWatchWithIPFS(peerID : String) {

        // Connect to remote service
        val service = P2PService.singleton!!.ipfs.connectService("/x/chronobuddy-sync", peerID)

        // Send pair request
        Logger.getLogger("MainActivity").info("POSTing to http://127.0.0.1:${service.port}/v1/pair")
        val response = post("http://127.0.0.1:${service.port}/v1/pair", data = Gson().toJson(mapOf(
            "device_name" to "Android Phone"
        )))

        // Process response
        val json = Gson().fromJson<JsonObject>(response.text)
        val status = json["status"].asString
        if (status != "ok")
            throw Exception(json["error_text"].asString ?: "Couldn't pair to the device.")

    }

}
