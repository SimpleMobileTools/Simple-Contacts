package com.simplemobiletools.contacts.pro.services

import android.annotation.TargetApi
import android.net.Uri
import android.os.Build
import android.telecom.*
import com.simplemobiletools.contacts.pro.helpers.MyConnection

@TargetApi(Build.VERSION_CODES.M)
class MyOutgoingCallService : ConnectionService() {
    override fun onCreateIncomingConnection(connectionManagerPhoneAccount: PhoneAccountHandle, request: ConnectionRequest): Connection {
        val connection = MyConnection(applicationContext)
        val phoneNumber = request.extras.get(TelecomManager.EXTRA_INCOMING_CALL_ADDRESS) as Uri
        connection.setAddress(phoneNumber, TelecomManager.PRESENTATION_ALLOWED)
        connection.setRinging()
        return connection
    }
}
