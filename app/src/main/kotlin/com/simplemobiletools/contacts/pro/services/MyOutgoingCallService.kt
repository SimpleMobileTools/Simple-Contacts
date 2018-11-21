package com.simplemobiletools.contacts.pro.services

import android.annotation.TargetApi
import android.os.Build
import android.telecom.ConnectionService

@TargetApi(Build.VERSION_CODES.M)
class MyOutgoingCallService : ConnectionService() {

}
