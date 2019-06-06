package com.jjv360.chronobuddy_watch.faces

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Paint.ANTI_ALIAS_FLAG
import android.text.format.DateFormat
import android.view.View
import java.util.logging.Logger

class Digital1(context : Context) : BaseWatchface(context) {

    private val timeStyle = Paint(ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 50f
        textAlign = Paint.Align.CENTER
    }

    private val dateStyle = Paint(ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(150, 150, 150)
        textSize = 32f
        textAlign = Paint.Align.CENTER
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        canvas?.drawRGB(0, 0, 0)

        // Draw the time in the center
        val time = DateFormat.format("hh:mm", System.currentTimeMillis()).toString()
        canvas?.drawText(time, width / 2f, height / 2f - 12f, timeStyle)

        // Draw the date underneath
        val date = DateFormat.format("dd MMMM", System.currentTimeMillis()).toString()
        canvas?.drawText(date, width / 2f, height / 2f + 32f, dateStyle)

    }

}