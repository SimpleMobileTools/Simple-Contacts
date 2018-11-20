package com.simplemobiletools.contacts.pro.helpers

import android.telephony.PhoneStateListener
import android.telephony.TelephonyManager

class MyPhoneStateListener : PhoneStateListener() {
    override fun onCallStateChanged(state: Int, phoneNumber: String?) {
        super.onCallStateChanged(state, phoneNumber)
        when (state) {
            TelephonyManager.CALL_STATE_IDLE -> {
            }
            TelephonyManager.CALL_STATE_RINGING -> {
            }
            TelephonyManager.CALL_STATE_OFFHOOK -> {
            }
        }
    }
}
