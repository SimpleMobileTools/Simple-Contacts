package com.simplemobiletools.contacts.extensions

import android.content.Intent
import android.net.Uri
import com.simplemobiletools.commons.R
import com.simplemobiletools.commons.dialogs.RadioGroupDialog
import com.simplemobiletools.commons.extensions.toast
import com.simplemobiletools.commons.helpers.PERMISSION_CALL_PHONE
import com.simplemobiletools.commons.models.RadioItem
import com.simplemobiletools.contacts.activities.SimpleActivity
import com.simplemobiletools.contacts.models.Contact

fun SimpleActivity.startCallIntent(recipient: String) {
    handlePermission(PERMISSION_CALL_PHONE) {
        if (it) {
            Intent(Intent.ACTION_CALL).apply {
                data = Uri.fromParts("tel", recipient, null)
                if (resolveActivity(packageManager) != null) {
                    startActivity(this)
                } else {
                    toast(R.string.no_app_found)
                }
            }
        }
    }
}

fun SimpleActivity.tryStartCall(contact: Contact) {
    val numbers = contact.phoneNumbers
    if (numbers.size == 1) {
        startCallIntent(numbers.first().value)
    } else if (numbers.size > 1) {
        val items = ArrayList<RadioItem>()
        numbers.forEachIndexed { index, phoneNumber ->
            items.add(RadioItem(index, phoneNumber.value, phoneNumber.value))
        }

        RadioGroupDialog(this, items) {
            startCallIntent(it as String)
        }
    }
}
