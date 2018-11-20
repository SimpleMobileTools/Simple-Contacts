package com.simplemobiletools.contacts.pro.objects

import android.annotation.TargetApi
import android.os.Build
import android.telecom.Call

@TargetApi(Build.VERSION_CODES.M)
object CallManager {
    private var currentCall: Call? = null

    fun updateCall(call: Call?) {
        currentCall = call
    }

    fun declineCall() {
        currentCall?.apply {
            when (state) {
                Call.STATE_RINGING -> reject(false, "")
                else -> disconnect()
            }
        }
    }

    fun acceptCall() {
        currentCall?.apply {
            answer(details.videoState)
        }
    }
}
