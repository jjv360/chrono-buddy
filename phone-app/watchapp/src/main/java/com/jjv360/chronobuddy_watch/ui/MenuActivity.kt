package com.jjv360.chronobuddy_watch.ui

import android.content.Intent
import android.os.Bundle
import android.os.PersistableBundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.jjv360.chronobuddy_watch.R

class MenuActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Load UI
        setContentView(R.layout.activity_appmenu)

    }

    fun openMusic(view : View) {

        // Show music control
        startActivity(Intent(this, MusicControlActivity::class.java))

    }

}