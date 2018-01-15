package com.simplemobiletools.contacts.extensions

import android.content.Intent
import android.net.Uri
import com.simplemobiletools.commons.R
import com.simplemobiletools.commons.activities.BaseSimpleActivity
import com.simplemobiletools.commons.dialogs.RadioGroupDialog
import com.simplemobiletools.commons.extensions.getFilePublicUri
import com.simplemobiletools.commons.extensions.shareUri
import com.simplemobiletools.commons.extensions.toast
import com.simplemobiletools.commons.helpers.PERMISSION_CALL_PHONE
import com.simplemobiletools.commons.models.RadioItem
import com.simplemobiletools.contacts.BuildConfig
import com.simplemobiletools.contacts.activities.SimpleActivity
import com.simplemobiletools.contacts.helpers.ContactsHelper
import com.simplemobiletools.contacts.helpers.VcfExporter
import com.simplemobiletools.contacts.models.Contact
import java.io.File

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

fun SimpleActivity.showContactSourcePicker(currentSource: String, callback: (newSource: String) -> Unit) {
    ContactsHelper(this).getContactSources {
        val items = ArrayList<RadioItem>()
        val sources = it
        var currentSourceIndex = -1
        sources.forEachIndexed { index, account ->
            items.add(RadioItem(index, account))
            if (account == currentSource) {
                currentSourceIndex = index
            }
        }

        runOnUiThread {
            RadioGroupDialog(this, items, currentSourceIndex) {
                callback(sources[it as Int])
            }
        }
    }
}

fun BaseSimpleActivity.shareContact(contact: Contact) {
    val file = getTempFile()
    if (file == null) {
        toast(R.string.unknown_error_occurred)
        return
    }

    VcfExporter().exportContacts(this, file, arrayListOf(contact)) {
        if (it == VcfExporter.ExportResult.EXPORT_OK) {
            val uri = getFilePublicUri(file, BuildConfig.APPLICATION_ID)
            shareUri(uri, BuildConfig.APPLICATION_ID)
        }
    }
}

fun BaseSimpleActivity.getTempFile(): File? {
    val folder = File(cacheDir, "contacts")
    if (!folder.exists()) {
        if (!folder.mkdir()) {
            toast(R.string.unknown_error_occurred)
            return null
        }
    }

    return File(folder, "contacts.vcf")
}
