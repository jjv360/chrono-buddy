package com.jjv360.chronobuddy.ui

import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import com.jjv360.chronobuddy.R
import com.jjv360.shared.P2PService
import com.jjv360.shared.start


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
        if (item?.itemId == R.id.mainmenu_manage_devices) {

            // Show pair helper screen
            startActivity(Intent(this, DeviceManagerActivity::class.java))
            return true

        }

        // Unknown menu item, let system deal with it
        return super.onOptionsItemSelected(item)

    }

    // Called by button onClick
    fun sendViaBluetooth() {



    }

}
