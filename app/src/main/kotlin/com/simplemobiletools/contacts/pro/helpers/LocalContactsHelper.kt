package com.simplemobiletools.contacts.pro.helpers

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.MediaStore
import com.simplemobiletools.contacts.pro.extensions.*
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

    private fun getPhotoByteArray(uri: String): ByteArray {
        if (uri.isEmpty()) {
            return ByteArray(0)
        }

        val photoUri = Uri.parse(uri)
        val bitmap = MediaStore.Images.Media.getBitmap(activity.contentResolver, photoUri)

        val thumbnailSize = activity.getPhotoThumbnailSize()
        val scaledPhoto = Bitmap.createScaledBitmap(bitmap, thumbnailSize * 2, thumbnailSize * 2, false)
        val scaledSizePhotoData = scaledPhoto.getByteArray()
        scaledPhoto.recycle()
        return scaledSizePhotoData
    }

    private fun convertLocalContactToContact(localContact: LocalContact): Contact {
        val filterDuplicates = activity.config.filterDuplicates
        val filteredPhoneNumbers = ArrayList<PhoneNumber>()
        if (filterDuplicates) {
            localContact.phoneNumbers.mapTo(filteredPhoneNumbers) { PhoneNumber(it.value.applyRegexFiltering(), 0, "") }
        }

        val contactPhoto = if (localContact.photo == null) {
            null
        } else {
            try {
                BitmapFactory.decodeByteArray(localContact.photo, 0, localContact.photo!!.size)
            } catch (e: OutOfMemoryError) {
                null
            }
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
            photo = contactPhoto
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
            photo = getPhotoByteArray(contact.photoUri)
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
