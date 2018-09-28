package com.simplemobiletools.contacts.extensions

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import com.simplemobiletools.commons.activities.BaseSimpleActivity
import com.simplemobiletools.commons.dialogs.RadioGroupDialog
import com.simplemobiletools.commons.extensions.sharePathIntent
import com.simplemobiletools.commons.extensions.showErrorToast
import com.simplemobiletools.commons.extensions.toast
import com.simplemobiletools.commons.helpers.PERMISSION_CALL_PHONE
import com.simplemobiletools.commons.models.RadioItem
import com.simplemobiletools.contacts.BuildConfig
import com.simplemobiletools.contacts.R
import com.simplemobiletools.contacts.activities.SimpleActivity
import com.simplemobiletools.contacts.dialogs.CallConfirmationDialog
import com.simplemobiletools.contacts.helpers.*
import com.simplemobiletools.contacts.models.Contact
import com.simplemobiletools.contacts.models.ContactSource
import java.io.File

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
        CallConfirmationDialog(this, contact.getFullName()) {
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
            items.add(RadioItem(index, phoneNumber.value, phoneNumber.value))
        }

        RadioGroupDialog(this, items) {
            startCallIntent(it as String)
        }
    }
}

fun SimpleActivity.showContactSourcePicker(currentSource: String, callback: (newSource: String) -> Unit) {
    ContactsHelper(this).getContactSources {
        val ignoredTypes = arrayListOf(
                "org.thoughtcrime.securesms",   // Signal
                "org.telegram.messenger"        // Telegram
        )

        val items = ArrayList<RadioItem>()
        val sources = it.filter { !ignoredTypes.contains(it.type) }.map { it.name }
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

    VcfExporter().exportContacts(this, file, contacts, false) {
        if (it == VcfExporter.ExportResult.EXPORT_OK) {
            sharePathIntent(file.absolutePath, BuildConfig.APPLICATION_ID)
        } else {
            showErrorToast("$it")
        }
    }
}

fun BaseSimpleActivity.sendSMSToContacts(contacts: ArrayList<Contact>) {
    val numbers = StringBuilder()
    contacts.forEach {
        val number = it.phoneNumbers.firstOrNull { it.type == ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE }
                ?: it.phoneNumbers.firstOrNull()
        if (number != null) {
            numbers.append("${number.value};")
        }

        val uriString = "smsto:${numbers.toString().trimEnd(';')}"
        Intent(Intent.ACTION_SENDTO, Uri.parse(uriString)).apply {
            if (resolveActivity(packageManager) != null) {
                startActivity(this)
            } else {
                toast(R.string.no_app_found)
            }
        }
    }
}

fun BaseSimpleActivity.sendEmailToContacts(contacts: ArrayList<Contact>) {
    val emails = ArrayList<String>()
    contacts.forEach {
        it.emails.forEach {
            if (it.value.isNotEmpty()) {
                emails.add(it.value)
            }
        }
    }

    Intent(Intent.ACTION_SEND_MULTIPLE).apply {
        type = "message/rfc822"
        putExtra(Intent.EXTRA_EMAIL, emails.toTypedArray())
        if (resolveActivity(packageManager) != null) {
            startActivity(this)
        } else {
            toast(R.string.no_app_found)
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

fun BaseSimpleActivity.addContactsToGroup(contacts: ArrayList<Contact>, groupId: Long) {
    val publicContacts = contacts.filter { it.source != SMT_PRIVATE }
    val privateContacts = contacts.filter { it.source == SMT_PRIVATE }
    if (publicContacts.isNotEmpty()) {
        ContactsHelper(this).addContactsToGroup(contacts, groupId)
    }

    if (privateContacts.isNotEmpty()) {
        dbHelper.addContactsToGroup(contacts, groupId)
    }
}

fun BaseSimpleActivity.removeContactsFromGroup(contacts: ArrayList<Contact>, groupId: Long) {
    val publicContacts = contacts.filter { it.source != SMT_PRIVATE }
    val privateContacts = contacts.filter { it.source == SMT_PRIVATE }
    if (publicContacts.isNotEmpty() && hasContactPermissions()) {
        ContactsHelper(this).removeContactsFromGroup(contacts, groupId)
    }

    if (privateContacts.isNotEmpty()) {
        dbHelper.removeContactsFromGroup(contacts, groupId)
    }
}

fun BaseSimpleActivity.getContactPublicUri(contact: Contact): Uri {
    val lookupKey = ContactsHelper(this).getContactLookupKey(contact.id.toString())
    return Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_LOOKUP_URI, lookupKey)
}

fun Activity.getVisibleContactSources(): ArrayList<String> {
    val sources = ContactsHelper(this).getDeviceContactSources()
    sources.add(ContactSource(getString(R.string.phone_storage_hidden), SMT_PRIVATE))
    val sourceNames = ArrayList(sources).map { if (it.type == SMT_PRIVATE) SMT_PRIVATE else it.name }.toMutableList() as ArrayList<String>
    sourceNames.removeAll(config.ignoredContactSources)
    return sourceNames
}

fun SimpleActivity.contactClicked(contact: Contact) {
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
