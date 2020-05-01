package com.simplemobiletools.contacts.pro.services

import android.content.Intent
import android.os.Build
import android.telecom.Call
import android.telecom.InCallService
import androidx.annotation.RequiresApi
import com.simplemobiletools.contacts.pro.activities.CallActivity
import com.simplemobiletools.contacts.pro.helpers.CallManager

@RequiresApi(Build.VERSION_CODES.M)
class CallService : InCallService() {
    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)
        val intent = Intent(this, CallActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        CallManager.call = call
    }

    override fun onCallRemoved(call: Call) {
        super.onCallRemoved(call)
        CallManager.call = null
    }
}
