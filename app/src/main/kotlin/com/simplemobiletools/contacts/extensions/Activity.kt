package com.simplemobiletools.contacts.extensions

import android.content.Intent
import android.net.Uri
import com.simplemobiletools.commons.activities.BaseSimpleActivity
import com.simplemobiletools.commons.dialogs.RadioGroupDialog
import com.simplemobiletools.commons.extensions.sharePathIntent
import com.simplemobiletools.commons.extensions.toast
import com.simplemobiletools.commons.helpers.PERMISSION_CALL_PHONE
import com.simplemobiletools.commons.models.RadioItem
import com.simplemobiletools.contacts.BuildConfig
import com.simplemobiletools.contacts.R
import com.simplemobiletools.contacts.activities.SimpleActivity
import com.simplemobiletools.contacts.helpers.ContactsHelper
import com.simplemobiletools.contacts.helpers.SMT_PRIVATE
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
        val sources = it.map { it.name }
        var currentSourceIndex = -1
        sources.forEachIndexed { index, account ->
            var publicAccount = account
            if (account == config.localAccountName) {
                publicAccount = getString(R.string.phone_storage)
            }

            items.add(RadioItem(index, publicAccount))
            if (account == currentSource) {
                currentSourceIndex = index
            } else if (currentSource == SMT_PRIVATE && account == getString(R.string.phone_storage_hidden)) {
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

fun SimpleActivity.getPublicContactSource(source: String): String {
    return when (source) {
        config.localAccountName -> getString(R.string.phone_storage)
        SMT_PRIVATE -> getString(R.string.phone_storage_hidden)
        else -> source
    }
}

fun BaseSimpleActivity.shareContacts(contacts: ArrayList<Contact>) {
    val file = getTempFile()
    if (file == null) {
        toast(R.string.unknown_error_occurred)
        return
    }

    VcfExporter().exportContacts(this, file, contacts) {
        if (it == VcfExporter.ExportResult.EXPORT_OK) {
            sharePathIntent(file.absolutePath, BuildConfig.APPLICATION_ID)
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
