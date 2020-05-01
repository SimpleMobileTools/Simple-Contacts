package com.simplemobiletools.contacts.pro.helpers

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.telecom.Call
import android.telecom.VideoProfile
import com.simplemobiletools.commons.extensions.getNameFromPhoneNumber
import com.simplemobiletools.commons.extensions.getPhotoUriFromPhoneNumber
import com.simplemobiletools.contacts.pro.models.CallContact

// inspired by https://github.com/Chooloo/call_manage
@SuppressLint("NewApi")
class CallManager {
    companion object {
        var call: Call? = null

        fun accept() {
            call?.answer(VideoProfile.STATE_AUDIO_ONLY)
        }

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

        fun unregisterCallback(callback: Call.Callback) {
            call?.unregisterCallback(callback)
        }

        fun getState() = if (call == null) {
            Call.STATE_DISCONNECTED
        } else {
            call!!.state
        }

        fun getCallContact(context: Context): CallContact? {
            val callContact = CallContact("", "")
            if (call == null) {
                return callContact
            }

            val uri = Uri.decode(call!!.details.handle.toString())
            if (uri.startsWith("tel:")) {
                val number = uri.substringAfter("tel:")
                callContact.name = context.getNameFromPhoneNumber(number)
                callContact.photoUri = context.getPhotoUriFromPhoneNumber(number)
            }

            return callContact
        }
    }
}
