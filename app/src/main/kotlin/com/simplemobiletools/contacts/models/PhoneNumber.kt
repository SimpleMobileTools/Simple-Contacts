package com.simplemobiletools.contacts.models

import android.provider.ContactsContract
import com.simplemobiletools.contacts.R

data class PhoneNumber(var value: String, var type: Int) {
    fun getTextId() = when (type) {
        ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE -> R.string.mobile
        ContactsContract.CommonDataKinds.Phone.TYPE_HOME -> R.string.home
        ContactsContract.CommonDataKinds.Phone.TYPE_WORK -> R.string.work
        ContactsContract.CommonDataKinds.Phone.TYPE_MAIN -> R.string.main_number
        ContactsContract.CommonDataKinds.Phone.TYPE_FAX_WORK -> R.string.work_fax
        ContactsContract.CommonDataKinds.Phone.TYPE_FAX_HOME -> R.string.home_fax
        ContactsContract.CommonDataKinds.Phone.TYPE_PAGER -> R.string.pager
        else -> R.string.other
    }
}
