package com.simplemobiletools.contacts.pro.extensions

import android.telecom.Call
import com.simplemobiletools.contacts.pro.models.GsmCall

fun Int.toGsmCallStatus() = when (this) {
    Call.STATE_ACTIVE -> GsmCall.Status.ACTIVE
    Call.STATE_RINGING -> GsmCall.Status.RINGING
    Call.STATE_CONNECTING -> GsmCall.Status.CONNECTING
    Call.STATE_DIALING -> GsmCall.Status.DIALING
    Call.STATE_DISCONNECTED -> GsmCall.Status.DISCONNECTED
    else -> GsmCall.Status.UNKNOWN
}
