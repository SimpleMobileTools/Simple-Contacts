package com.simplemobiletools.contacts.models

import android.provider.ContactsContract
import com.simplemobiletools.contacts.R

data class Email(var value: String, var type: Int) {
    fun getTextId() = when (type) {
        ContactsContract.CommonDataKinds.Email.TYPE_HOME -> R.string.home
        ContactsContract.CommonDataKinds.Email.TYPE_WORK -> R.string.work
        ContactsContract.CommonDataKinds.Email.TYPE_MOBILE -> R.string.mobile
        else -> R.string.other
    }
}
