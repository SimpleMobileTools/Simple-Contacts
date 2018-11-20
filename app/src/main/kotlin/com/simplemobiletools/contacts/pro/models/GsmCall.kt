package com.simplemobiletools.contacts.pro.models

data class GsmCall(val status: GsmCall.Status) {

    enum class Status {
        CONNECTING,
        DIALING,
        RINGING,
        ACTIVE,
        DISCONNECTED,
        UNKNOWN
    }
}
