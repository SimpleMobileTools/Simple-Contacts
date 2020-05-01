package com.simplemobiletools.contacts.pro.activities

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import android.telecom.Call
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.helpers.isOreoPlus
import com.simplemobiletools.contacts.pro.R
import com.simplemobiletools.contacts.pro.helpers.ACCEPT_CALL
import com.simplemobiletools.contacts.pro.helpers.CallManager
import com.simplemobiletools.contacts.pro.helpers.DECLINE_CALL
import com.simplemobiletools.contacts.pro.receivers.CallActionReceiver
import kotlinx.android.synthetic.main.activity_call.*

class CallActivity : SimpleActivity() {
    private val CALL_NOTIFICATION_ID = 1

    private var isSpeakerOn = false
    private var isMicrophoneOn = true

    override fun onCreate(savedInstanceState: Bundle?) {
        supportActionBar?.hide()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_call)

        updateTextColors(call_holder)
        initButtons()
        showNotification()

        CallManager.registerCallback(getCallCallback())
        updateCallState(CallManager.getState())
        updateOtherPersonsInfo()
    }

    override fun onDestroy() {
        super.onDestroy()
        notificationManager.cancel(CALL_NOTIFICATION_ID)
    }

    private fun initButtons() {
        call_decline.setOnClickListener {
            endCall()
        }

        call_accept.setOnClickListener {
            acceptCall()
        }

        call_toggle_microphone.setOnClickListener {
            toggleMicrophone()
        }

        call_toggle_speaker.setOnClickListener {
            toggleSpeaker()
        }

        call_dialpad.setOnClickListener { }
        call_end.setOnClickListener {
            endCall()
        }
    }

    private fun toggleSpeaker() {
        isSpeakerOn = !isSpeakerOn
        val drawable = if (isSpeakerOn) R.drawable.ic_speaker_on_vector else R.drawable.ic_speaker_off_vector
        call_toggle_speaker.setImageDrawable(getDrawable(drawable))
    }

    private fun toggleMicrophone() {
        isMicrophoneOn = !isMicrophoneOn
        val drawable = if (isMicrophoneOn) R.drawable.ic_microphone_vector else R.drawable.ic_microphone_off_vector
        call_toggle_microphone.setImageDrawable(getDrawable(drawable))
    }

    private fun updateOtherPersonsInfo() {
        val callContact = CallManager.getCallContact(applicationContext) ?: return
        caller_name_label.text = callContact.name
        caller_number_label.text = callContact.number
        caller_number_label.beVisibleIf(callContact.number.isNotEmpty())

        val options = RequestOptions()
            .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
            .centerCrop()

        Glide.with(this)
            .load(callContact.photoUri)
            .transition(DrawableTransitionOptions.withCrossFade())
            .apply(options)
            .apply(RequestOptions.circleCropTransform())
            .into(caller_avatar)
    }

    private fun updateCallState(state: Int) {
        when (state) {
            Call.STATE_ACTIVE -> callStarted()
            Call.STATE_DISCONNECTED -> endCall()
        }
    }

    private fun acceptCall() {
        CallManager.accept()
    }

    private fun callStarted() {
        incoming_call_holder.beGone()
        ongoing_call_holder.beVisible()
    }

    private fun endCall() {
        CallManager.reject()
        finish()
    }

    @SuppressLint("NewApi")
    fun getCallCallback() = object : Call.Callback() {
        override fun onStateChanged(call: Call, state: Int) {
            super.onStateChanged(call, state)
            updateCallState(state)
        }
    }

    @SuppressLint("NewApi")
    private fun showNotification() {
        val channelId = "simple_contacts_call"
        if (isOreoPlus()) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val name = "call_notification_channel"

            NotificationChannel(channelId, name, importance).apply {
                notificationManager.createNotificationChannel(this)
            }
        }

        val openAppIntent = Intent(this, CallActivity::class.java)
        openAppIntent.flags = Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT
        val openAppPendingIntent = PendingIntent.getActivity(this, 0, openAppIntent, 0)

        val acceptCallIntent = Intent(this, CallActionReceiver::class.java)
        acceptCallIntent.action = ACCEPT_CALL
        val acceptPendingIntent = PendingIntent.getBroadcast(this, 0, acceptCallIntent, PendingIntent.FLAG_CANCEL_CURRENT)

        val declineCallIntent = Intent(this, CallActionReceiver::class.java)
        declineCallIntent.action = DECLINE_CALL
        val declinePendingIntent = PendingIntent.getBroadcast(this, 1, declineCallIntent, PendingIntent.FLAG_CANCEL_CURRENT)

        val callerName = "Caller name"
        val contentText = getString(R.string.incoming_call)

        val collapsedView = RemoteViews(packageName, R.layout.call_notification).apply {
            setText(R.id.notification_caller_name, callerName)
            setText(R.id.notification_caller_number, contentText)

            setOnClickPendingIntent(R.id.notification_decline_call, declinePendingIntent)
            setOnClickPendingIntent(R.id.notification_accept_call, acceptPendingIntent)
        }

        val builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_phone_vector)
            .setContentIntent(openAppPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(Notification.CATEGORY_CALL)
            .setCustomContentView(collapsedView)
            .setOngoing(true)
            .setAutoCancel(true)
            .setChannelId(channelId)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())

        val notification = builder.build()
        notificationManager.notify(CALL_NOTIFICATION_ID, notification)
    }
}
