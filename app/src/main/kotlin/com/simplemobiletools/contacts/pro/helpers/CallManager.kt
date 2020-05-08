package com.simplemobiletools.contacts.pro.helpers

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.telecom.Call
import android.telecom.VideoProfile
import com.simplemobiletools.commons.helpers.SimpleContactsHelper
import com.simplemobiletools.commons.helpers.ensureBackgroundThread
import com.simplemobiletools.contacts.pro.extensions.contactsDB
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

        fun keypad(c: Char) {
            call?.playDtmfTone(c)
            call?.stopDtmfTone()
        }

        fun getCallContact(context: Context, callback: (CallContact?) -> Unit) {
            val callContact = CallContact("", "", "")
            if (call == null || call!!.details == null || call!!.details!!.handle == null) {
                callback(callContact)
                return
            }

            val uri = Uri.decode(call!!.details.handle.toString())
            if (uri.startsWith("tel:")) {
                val number = uri.substringAfter("tel:")
                callContact.number = number
                callContact.name = SimpleContactsHelper(context).getNameFromPhoneNumber(number)
                callContact.photoUri = SimpleContactsHelper(context).getPhotoUriFromPhoneNumber(number)

                if (callContact.name == callContact.number) {
                    ensureBackgroundThread {
                        val localContact = context.contactsDB.getContactWithNumber("%$number%")
                        if (localContact != null) {
                            val storedGroups = ContactsHelper(context).getStoredGroupsSync()
                            val newContact = LocalContactsHelper(context).convertLocalContactToContact(localContact, storedGroups)
                            callContact.name = newContact!!.getNameToDisplay()
                            callContact.photoUri = newContact.photoUri
                        }

                        callback(callContact)
                    }
                } else {
                    callback(callContact)
                }
            }
        }
    }
}
