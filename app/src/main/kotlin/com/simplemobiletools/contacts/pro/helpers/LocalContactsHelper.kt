package com.simplemobiletools.contacts.pro.helpers

import android.app.Activity
import com.simplemobiletools.contacts.pro.extensions.contactsDB
import com.simplemobiletools.contacts.pro.models.Contact
import com.simplemobiletools.contacts.pro.models.LocalContact

class LocalContactsHelper(val activity: Activity) {
    fun insertContact(contact: Contact): Boolean {
        val localContact = convertContactToLocalContact(contact)
        activity.contactsDB.insertOrUpdate(localContact)
        return true
    }

    private fun convertContactToLocalContact(contact: Contact): LocalContact {
        return getEmptyLocalContact().apply {
            id = if (contact.id == 0) null else contact.id
            prefix = contact.prefix
            firstName = contact.firstName
            middleName = contact.middleName
            surname = contact.surname
            suffix = contact.suffix
            nickname = contact.nickname
            phoneNumbers = contact.phoneNumbers
            emails = contact.emails
            events = contact.events
            starred = contact.starred
            addresses = contact.addresses
            notes = contact.notes
            groups = contact.groups
            company = contact.organization.company
            jobPosition = contact.organization.jobPosition
            websites = contact.websites
            ims = contact.IMs
        }
    }
}
