package com.jjv360.chronobuddy.ui

import android.app.AlertDialog
import android.app.PendingIntent
import android.app.ProgressDialog
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import com.ivan200.photobarcodelib.PhotoBarcodeScannerBuilder
import com.jjv360.chronobuddy.R
import com.jjv360.shared.P2PService
import com.jjv360.shared.registerContact
import com.jjv360.shared.start
import nl.komponents.kovenant.ui.failUi
import nl.komponents.kovenant.ui.successUi


class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Start our P2P service
        val relaunchIntent = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), 0)
        P2PService.start(this, relaunchIntent)

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
            .withResultListener {

                // Check result
                pairWatchWithCode(it.rawValue)

            }
            .build()
            .start()

    }

    fun pairWatchWithCode(txt : String) {

        val dlg = ProgressDialog(this)
        dlg.setTitle("Pairing watch")
        dlg.setMessage("Searching for peer...")
        dlg.setCancelable(false)
        dlg.show()

        // Decode account
        P2PService.registerContact(txt) successUi {

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

}
