package com.simplemobiletools.contacts.pro.helpers

import android.annotation.SuppressLint
import android.telecom.Call

// inspired by https://github.com/Chooloo/call_manage
@SuppressLint("NewApi")
class CallManager {
    companion object {
        var call: Call? = null

        fun reject() {
            if (call != null) {
                if (call!!.state == Call.STATE_RINGING) {
                    call!!.reject(false, null)
                } else {
                    call!!.disconnect()
                }
            }
        }

        fun registerCallback(callback: Call.Callback) {
            if (call != null) {
                call!!.registerCallback(callback)
            }
        }

        fun getState() = if (call == null) {
            Call.STATE_DISCONNECTED
        } else {
            call!!.state
        }
    }
}
