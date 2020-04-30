package com.simplemobiletools.contacts.pro.services

import android.os.Build
import android.telecom.Call
import android.telecom.InCallService
import androidx.annotation.RequiresApi

@RequiresApi(Build.VERSION_CODES.M)
class CallService : InCallService() {
    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)
    }
}
