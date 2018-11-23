package com.simplemobiletools.contacts.pro.helpers

import android.annotation.TargetApi
import android.content.Context
import android.os.Build
import android.telecom.Connection
import android.telecom.DisconnectCause

@TargetApi(Build.VERSION_CODES.M)
class MyConnection(val context: Context) : Connection() {
    override fun onAnswer() {
        super.onAnswer()
        setActive()
    }

    override fun onReject() {
        super.onReject()
        setDisconnected(DisconnectCause(DisconnectCause.REJECTED))
        destroy()
    }

    override fun onAbort() {
        super.onAbort()
        setDisconnected(DisconnectCause(DisconnectCause.REMOTE))
        destroy()
    }

    override fun onDisconnect() {
        super.onDisconnect()
        setDisconnected(DisconnectCause(DisconnectCause.CANCELED))
        destroy()
    }
}
