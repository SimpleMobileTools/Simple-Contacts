package com.simplemobiletools.contacts.pro.services

import android.annotation.TargetApi
import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.telecom.Call
import android.telecom.InCallService
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.simplemobiletools.contacts.pro.activities.DialerActivity
import com.simplemobiletools.contacts.pro.helpers.*
import com.simplemobiletools.contacts.pro.objects.CallManager

@TargetApi(Build.VERSION_CODES.M)
class MyIncomingCallService : InCallService() {
    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)
        call.registerCallback(callCallback)

        val handle = Uri.decode(call.details.handle.toString())
        val callerNumber = if (handle.contains("tel:")) {
            handle.substringAfter("tel:")
        } else {
            handle
        }

        val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        if (keyguardManager.isKeyguardLocked) {
            Intent(this, DialerActivity::class.java).apply {
                action = RESUME_DIALER
                flags = flags or Intent.FLAG_ACTIVITY_NEW_TASK
                putExtra(CALL_NUMBER, callerNumber)
                putExtra(CALL_STATUS, call.state)
                putExtra(IS_INCOMING_CALL, true)
                startActivity(this)
            }
        } else {
            Intent(this, DialerCallService::class.java).apply {
                putExtra(CALL_STATUS, call.state)
                putExtra(CALL_NUMBER, callerNumber)
                putExtra(IS_INCOMING_CALL, true)
                startService(this)
            }
        }

        CallManager.updateCall(call)
    }

    override fun onCallRemoved(call: Call) {
        super.onCallRemoved(call)
        call.unregisterCallback(callCallback)
        CallManager.updateCall(null)
    }

    private val callCallback = object : Call.Callback() {
        override fun onStateChanged(call: Call, state: Int) {
            CallManager.updateCall(call)
            sendCallToActivity(call)
            if (state == Call.STATE_DISCONNECTED) {
                Intent(applicationContext, DialerCallService::class.java).apply {
                    stopService(this)
                }
            }
        }
    }

    private fun sendCallToActivity(call: Call) {
        Intent(DIALER_INTENT_FILTER).apply {
            putExtra(CALL_STATUS, call.state)
            LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(this)
        }
    }
}
