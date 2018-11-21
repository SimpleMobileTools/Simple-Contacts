package com.simplemobiletools.contacts.pro.services

import android.annotation.TargetApi
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.simplemobiletools.commons.helpers.isOreoPlus
import com.simplemobiletools.contacts.pro.R
import com.simplemobiletools.contacts.pro.activities.DialerActivity
import com.simplemobiletools.contacts.pro.helpers.CALLER_NUMBER

class DialerCallService : Service() {
    private val CALL_NOTIFICATION_ID = 1
    private var number = ""

    override fun onBind(intent: Intent?) = null

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        number = intent.getStringExtra(CALLER_NUMBER)
        setupNotification()
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        stopForeground(true)
    }

    @TargetApi(Build.VERSION_CODES.O)
    private fun setupNotification() {
        val channelId = "simple_contacts_channel"
        if (isOreoPlus()) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val name = resources.getString(R.string.app_name)
            val importance = NotificationManager.IMPORTANCE_HIGH
            NotificationChannel(channelId, name, importance).apply {
                enableLights(false)
                enableVibration(false)
                notificationManager.createNotificationChannel(this)
            }
        }

        val notification = NotificationCompat.Builder(applicationContext, channelId)
                .setSmallIcon(R.drawable.ic_person)
                .setContentTitle(number)
                .setContentText(getString(R.string.calling))
                .setContentIntent(getLaunchDialerIntent())
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setOngoing(true)
                .setChannelId(channelId)

        startForeground(CALL_NOTIFICATION_ID, notification.build())
    }

    private fun getLaunchDialerIntent(): PendingIntent {
        val intent = Intent(this, DialerActivity::class.java)
        intent.action = Intent.ACTION_MAIN
        intent.addCategory(Intent.CATEGORY_LAUNCHER)
        return PendingIntent.getActivity(this, 0, intent, 0)
    }
}
