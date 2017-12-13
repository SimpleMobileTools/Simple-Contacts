package com.simplemobiletools.contacts.models

import android.provider.ContactsContract

data class Emails(var home: String = "", var work: String = "", var mobile: String = "", var other: String = "") {
    fun fillEmail(email: String, type: Int) {
        when (type) {
            ContactsContract.CommonDataKinds.Email.TYPE_HOME -> home = email
            ContactsContract.CommonDataKinds.Email.TYPE_WORK -> work = email
            ContactsContract.CommonDataKinds.Email.TYPE_MOBILE -> mobile = email
            ContactsContract.CommonDataKinds.Email.TYPE_OTHER -> other = email
        }
    }
}
