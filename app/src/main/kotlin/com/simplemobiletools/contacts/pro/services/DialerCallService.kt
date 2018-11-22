package com.simplemobiletools.contacts.pro.services

import android.annotation.TargetApi
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.telecom.Call
import androidx.core.app.NotificationCompat
import com.simplemobiletools.commons.helpers.isOreoPlus
import com.simplemobiletools.contacts.pro.R
import com.simplemobiletools.contacts.pro.activities.DialerActivity
import com.simplemobiletools.contacts.pro.helpers.CALL_NUMBER
import com.simplemobiletools.contacts.pro.helpers.CALL_STATUS
import com.simplemobiletools.contacts.pro.helpers.IS_INCOMING_CALL
import com.simplemobiletools.contacts.pro.helpers.RESUME_DIALER

class DialerCallService : Service() {
    private val CALL_NOTIFICATION_ID = 1
    private var callNumber = ""
    private var callStatus = Call.STATE_NEW
    private var isIncomingCall = false

    override fun onBind(intent: Intent?) = null

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        callNumber = intent.getStringExtra(CALL_NUMBER)
        callStatus = intent.getIntExtra(CALL_STATUS, Call.STATE_NEW)
        isIncomingCall = intent.getBooleanExtra(IS_INCOMING_CALL, false)

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
                .setSmallIcon(R.drawable.ic_phone)
                .setContentTitle(callNumber)
                .setContentText(getCallStatusString())
                .setContentIntent(getLaunchDialerIntent())
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setOngoing(true)
                .setChannelId(channelId)
                .setUsesChronometer(callStatus == Call.STATE_ACTIVE)

        startForeground(CALL_NOTIFICATION_ID, notification.build())
    }

    private fun getLaunchDialerIntent(): PendingIntent {
        val intent = Intent(this, DialerActivity::class.java).apply {
            action = RESUME_DIALER
            putExtra(CALL_NUMBER, callNumber)
            putExtra(CALL_STATUS, callStatus)
            putExtra(IS_INCOMING_CALL, isIncomingCall)
        }
        return PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
    }

    private fun getCallStatusString(): String {
        return when (callStatus) {
            Call.STATE_NEW -> applicationContext.getString(if (isIncomingCall) R.string.incoming_call else R.string.calling)
            Call.STATE_ACTIVE -> applicationContext.getString(R.string.ongoing_call)
            else -> ""
        }
    }
}
