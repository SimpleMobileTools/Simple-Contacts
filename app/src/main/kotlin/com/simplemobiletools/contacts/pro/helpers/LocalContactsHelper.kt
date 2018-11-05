package com.simplemobiletools.contacts.pro.helpers

import android.app.Activity
import com.simplemobiletools.contacts.pro.extensions.applyRegexFiltering
import com.simplemobiletools.contacts.pro.extensions.config
import com.simplemobiletools.contacts.pro.extensions.contactsDB
import com.simplemobiletools.contacts.pro.extensions.getEmptyContact
import com.simplemobiletools.contacts.pro.models.Contact
import com.simplemobiletools.contacts.pro.models.LocalContact
import com.simplemobiletools.contacts.pro.models.Organization
import com.simplemobiletools.contacts.pro.models.PhoneNumber

class LocalContactsHelper(val activity: Activity) {
    fun getAllContacts() = activity.contactsDB.getContacts().map { convertLocalContactToContact(it) }.toMutableList() as ArrayList<Contact>

    fun getContactWithId(id: Int) = convertLocalContactToContact(activity.contactsDB.getContactWithId(id))

    fun insertContact(contact: Contact): Boolean {
        val localContact = convertContactToLocalContact(contact)
        activity.contactsDB.insertOrUpdate(localContact)
        return true
    }

    private fun convertLocalContactToContact(localContact: LocalContact): Contact {
        val filterDuplicates = activity.config.filterDuplicates
        val filteredPhoneNumbers = ArrayList<PhoneNumber>()
        if (filterDuplicates) {
            localContact.phoneNumbers.mapTo(filteredPhoneNumbers) { PhoneNumber(it.value.applyRegexFiltering(), 0, "") }
        }

        return activity.getEmptyContact().apply {
            id = localContact.id!!
            prefix = localContact.prefix
            firstName = localContact.firstName
            middleName = localContact.middleName
            surname = localContact.surname
            suffix = localContact.suffix
            nickname = localContact.nickname
            photoUri = ""
            phoneNumbers = localContact.phoneNumbers
            emails = localContact.emails
            addresses = localContact.addresses
            events = localContact.events
            source = SMT_PRIVATE
            starred = localContact.starred
            contactId = localContact.id!!
            thumbnailUri = ""
            notes = localContact.notes
            groups = localContact.groups
            organization = Organization(localContact.company, localContact.jobPosition)
            websites = localContact.websites
            cleanPhoneNumbers = filteredPhoneNumbers
            IMs = localContact.IMs
        }
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
            IMs = contact.IMs
        }
    }
}
