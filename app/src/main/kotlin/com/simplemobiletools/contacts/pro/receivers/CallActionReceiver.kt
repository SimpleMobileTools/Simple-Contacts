package com.simplemobiletools.contacts.pro.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.simplemobiletools.contacts.pro.helpers.CallManager
import com.simplemobiletools.contacts.pro.helpers.DECLINE_CALL

class CallActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            DECLINE_CALL -> CallManager.reject()
        }
    }
}
