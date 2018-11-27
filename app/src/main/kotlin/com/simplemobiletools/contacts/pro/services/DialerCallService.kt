package com.simplemobiletools.contacts.pro.services

import android.annotation.TargetApi
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.telecom.Call
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import com.simplemobiletools.commons.extensions.getColoredBitmap
import com.simplemobiletools.commons.extensions.setText
import com.simplemobiletools.commons.helpers.isOreoPlus
import com.simplemobiletools.contacts.pro.R
import com.simplemobiletools.contacts.pro.activities.DialerActivity
import com.simplemobiletools.contacts.pro.helpers.*
import com.simplemobiletools.contacts.pro.objects.CallManager

class DialerCallService : Service() {
    private val REFRESH_REMINDER_PERIOD = 3000L
    private val CALL_NOTIFICATION_ID = 1
    private val LAUNCH_DIALER_INTENT_ID = 1
    private val DISMISS_CALL_INTENT_ID = 2
    private val ANSWER_CALL_INTENT_ID = 3

    private var callNumber = ""
    private var callStatus = Call.STATE_NEW
    private var isIncomingCall = false
    private var reminderRefreshHandler = Handler()

    override fun onBind(intent: Intent?) = null

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        if (intent.getBooleanExtra(DECLINE_CALL, false)) {
            CallManager.declineCall()
            stopForeground(true)
            stopSelf()
        } else {
            callNumber = intent.getStringExtra(CALL_NUMBER)
            callStatus = intent.getIntExtra(CALL_STATUS, Call.STATE_NEW)
            isIncomingCall = intent.getBooleanExtra(IS_INCOMING_CALL, false)
            setupNotification()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        stopForeground(true)
        reminderRefreshHandler.removeCallbacksAndMessages(null)
    }

    @TargetApi(Build.VERSION_CODES.O)
    private fun setupNotification() {
        val channelId = "incoming_call"
        if (isOreoPlus()) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val name = resources.getString(R.string.app_name)
            val importance = if (callStatus == Call.STATE_RINGING) NotificationManager.IMPORTANCE_HIGH else NotificationManager.IMPORTANCE_DEFAULT
            NotificationChannel(channelId, name, importance).apply {
                enableLights(false)
                enableVibration(false)
                setSound(null, null)
                notificationManager.createNotificationChannel(this)
            }
        }

        val notificationLayout = RemoteViews(packageName, R.layout.incoming_call_notification).apply {
            setText(R.id.incoming_call_caller, callNumber)
            setText(R.id.incoming_call_status, getCallStatusString())

            val resources = applicationContext.resources
            setImageViewBitmap(R.id.call_decline, resources.getColoredBitmap(R.drawable.ic_phone_down, resources.getColor(R.color.theme_dark_red_primary_color)))
            setImageViewBitmap(R.id.call_answer, resources.getColoredBitmap(R.drawable.ic_phone, resources.getColor(R.color.md_green_700)))

            setVisibleIf(R.id.call_answer, callStatus == Call.STATE_RINGING)

            setOnClickPendingIntent(R.id.call_decline, getDeclineCallIntent())
            setOnClickPendingIntent(R.id.call_answer, getAnswerCallIntent())
        }

        val notification = NotificationCompat.Builder(applicationContext, channelId)
                .setSmallIcon(R.drawable.ic_phone)
                .setContentIntent(getLaunchDialerIntent())
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCustomContentView(notificationLayout)
                .setCustomHeadsUpContentView(notificationLayout)
                .setCustomBigContentView(notificationLayout)
                .setOngoing(true)
                .setCategory(NotificationCompat.CATEGORY_CALL)
                .setChannelId(channelId)
                .setSound(null)
                .setOnlyAlertOnce(false)
                .setUsesChronometer(callStatus == Call.STATE_ACTIVE)

        startForeground(CALL_NOTIFICATION_ID, notification.build())

        reminderRefreshHandler.postDelayed({
            if (callStatus == Call.STATE_RINGING) {
                setupNotification()
            }
        }, REFRESH_REMINDER_PERIOD)
    }

    private fun getLaunchDialerIntent(): PendingIntent {
        val intent = Intent(this, DialerActivity::class.java).apply {
            action = RESUME_DIALER
            putExtra(CALL_NUMBER, callNumber)
            putExtra(CALL_STATUS, callStatus)
            putExtra(IS_INCOMING_CALL, isIncomingCall)
        }
        return PendingIntent.getActivity(this, LAUNCH_DIALER_INTENT_ID, intent, PendingIntent.FLAG_UPDATE_CURRENT)
    }

    private fun getDeclineCallIntent(): PendingIntent {
        Intent(this, DialerCallService::class.java).apply {
            putExtra(DECLINE_CALL, true)
            return PendingIntent.getService(this@DialerCallService, DISMISS_CALL_INTENT_ID, this, PendingIntent.FLAG_UPDATE_CURRENT)
        }
    }

    private fun getAnswerCallIntent(): PendingIntent {
        val intent = Intent(this, DialerActivity::class.java).apply {
            action = RESUME_DIALER
            putExtra(CALL_NUMBER, callNumber)
            putExtra(CALL_STATUS, callStatus)
            putExtra(IS_INCOMING_CALL, isIncomingCall)
            putExtra(ANSWER_CALL, true)
        }
        return PendingIntent.getActivity(this, ANSWER_CALL_INTENT_ID, intent, PendingIntent.FLAG_UPDATE_CURRENT)
    }

    private fun getCallStatusString(): String {
        val id = when (callStatus) {
            Call.STATE_DIALING -> R.string.calling
            Call.STATE_RINGING -> R.string.incoming_call
            Call.STATE_ACTIVE -> R.string.ongoing_call
            else -> if (isIncomingCall) R.string.incoming_call else R.string.calling
        }

        return applicationContext.getString(id)
    }
}
