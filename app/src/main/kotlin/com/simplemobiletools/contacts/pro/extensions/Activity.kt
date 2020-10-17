package com.simplemobiletools.contacts.pro.extensions

import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract.CommonDataKinds.BaseTypes
import android.provider.ContactsContract.CommonDataKinds.Phone
import com.simplemobiletools.commons.activities.BaseSimpleActivity
import com.simplemobiletools.commons.dialogs.RadioGroupDialog
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.helpers.PERMISSION_CALL_PHONE
import com.simplemobiletools.commons.models.RadioItem
import com.simplemobiletools.contacts.pro.BuildConfig
import com.simplemobiletools.contacts.pro.R
import com.simplemobiletools.contacts.pro.activities.SimpleActivity
import com.simplemobiletools.contacts.pro.dialogs.CallConfirmationDialog
import com.simplemobiletools.contacts.pro.helpers.*
import com.simplemobiletools.contacts.pro.models.Contact
import java.io.BufferedReader
import java.io.ByteArrayOutputStream

fun SimpleActivity.startCallIntent(recipient: String) {
    handlePermission(PERMISSION_CALL_PHONE) {
        val action = if (it) Intent.ACTION_CALL else Intent.ACTION_DIAL
        Intent(action).apply {
            data = Uri.fromParts("tel", recipient, null)

            if (resolveActivity(packageManager) != null) {
                startActivity(this)
            } else {
                toast(R.string.no_app_found)
            }
        }
    }
}

fun SimpleActivity.tryStartCall(contact: Contact) {
    if (config.showCallConfirmation) {
        CallConfirmationDialog(this, contact.getNameToDisplay()) {
            startCall(contact)
        }
    } else {
        startCall(contact)
    }
}

fun SimpleActivity.startCall(contact: Contact) {
    val numbers = contact.phoneNumbers
    if (numbers.size == 1) {
        startCallIntent(numbers.first().value)
    } else if (numbers.size > 1) {
        val items = ArrayList<RadioItem>()
        numbers.forEachIndexed { index, phoneNumber ->
            items.add(RadioItem(index, "${phoneNumber.value} (${getPhoneNumberTypeText(phoneNumber.type, phoneNumber.label)})", phoneNumber.value))
        }

        RadioGroupDialog(this, items) {
            startCallIntent(it as String)
        }
    }
}

fun SimpleActivity.showContactSourcePicker(currentSource: String, callback: (newSource: String) -> Unit) {
    ContactsHelper(this).getSaveableContactSources { sources ->
        val items = ArrayList<RadioItem>()
        var sourceNames = sources.map { it.name }
        var currentSourceIndex = sourceNames.indexOfFirst { it == currentSource }
        sourceNames = sources.map { it.publicName }

        sourceNames.forEachIndexed { index, account ->
            items.add(RadioItem(index, account))
            if (currentSource == SMT_PRIVATE && account == getString(R.string.phone_storage_hidden)) {
                currentSourceIndex = index
            }
        }

        runOnUiThread {
            RadioGroupDialog(this, items, currentSourceIndex) {
                callback(sources[it as Int].name)
            }
        }
    }
}

fun BaseSimpleActivity.shareContacts(contacts: ArrayList<Contact>) {
    val file = getTempFile()
    if (file == null) {
        toast(R.string.unknown_error_occurred)
        return
    }

    getFileOutputStream(file.toFileDirItem(this), true) {
        VcfExporter().exportContacts(this, it, contacts, false) {
            if (it == VcfExporter.ExportResult.EXPORT_OK) {
                sharePathIntent(file.absolutePath, BuildConfig.APPLICATION_ID)
            } else {
                showErrorToast("$it")
            }
        }
    }
}

fun BaseSimpleActivity.shareQRContact(contact: Contact):String {
    val temp = VcfExporter().exportContact(this, contact, false)

    if (temp.isNullOrEmpty()) {
        return "Error"
    } else {
        return temp
    }
}

fun SimpleActivity.handleGenericContactClick(contact: Contact) {
    when (config.onContactClick) {
        ON_CLICK_CALL_CONTACT -> callContact(contact)
        ON_CLICK_VIEW_CONTACT -> viewContact(contact)
        ON_CLICK_EDIT_CONTACT -> editContact(contact)
    }
}

fun SimpleActivity.callContact(contact: Contact) {
    if (contact.phoneNumbers.isNotEmpty()) {
        tryStartCall(contact)
    } else {
        toast(R.string.no_phone_number_found)
    }
}

fun SimpleActivity.getPhoneNumberTypeText(type: Int, label: String): String {
    return if (type == BaseTypes.TYPE_CUSTOM) {
        label
    } else {
        getString(when (type) {
            Phone.TYPE_MOBILE -> R.string.mobile
            Phone.TYPE_HOME -> R.string.home
            Phone.TYPE_WORK -> R.string.work
            Phone.TYPE_MAIN -> R.string.main_number
            Phone.TYPE_FAX_WORK -> R.string.work_fax
            Phone.TYPE_FAX_HOME -> R.string.home_fax
            Phone.TYPE_PAGER -> R.string.pager
            else -> R.string.other
        })
    }
}
