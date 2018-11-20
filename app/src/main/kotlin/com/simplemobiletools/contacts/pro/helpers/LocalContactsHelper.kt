package com.simplemobiletools.contacts.pro.helpers

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.MediaStore
import com.simplemobiletools.contacts.pro.extensions.contactsDB
import com.simplemobiletools.contacts.pro.extensions.getByteArray
import com.simplemobiletools.contacts.pro.extensions.getEmptyContact
import com.simplemobiletools.contacts.pro.extensions.getPhotoThumbnailSize
import com.simplemobiletools.contacts.pro.models.Contact
import com.simplemobiletools.contacts.pro.models.Group
import com.simplemobiletools.contacts.pro.models.LocalContact
import com.simplemobiletools.contacts.pro.models.Organization

class LocalContactsHelper(val activity: Activity) {
    fun getAllContacts() = activity.contactsDB.getContacts().map { convertLocalContactToContact(it) }.toMutableList() as ArrayList<Contact>

    fun getContactWithId(id: Int) = convertLocalContactToContact(activity.contactsDB.getContactWithId(id))

    fun getContactWithNumber(number: String): Contact? {
        activity.contactsDB.getContacts().forEach {
            if (it.phoneNumbers.map { it.value }.contains(number)) {
                return convertLocalContactToContact(it)
            }
        }
        return null
    }

    fun insertOrUpdateContact(contact: Contact): Boolean {
        val localContact = convertContactToLocalContact(contact)
        return activity.contactsDB.insertOrUpdate(localContact) > 0
    }

    fun addContactsToGroup(contacts: ArrayList<Contact>, groupId: Long) {
        contacts.forEach {
            val localContact = convertContactToLocalContact(it)
            val newGroups = localContact.groups
            newGroups.add(groupId)
            newGroups.distinct()
            localContact.groups = newGroups
            activity.contactsDB.insertOrUpdate(localContact)
        }
    }

    fun removeContactsFromGroup(contacts: ArrayList<Contact>, groupId: Long) {
        contacts.forEach {
            val localContact = convertContactToLocalContact(it)
            val newGroups = localContact.groups
            newGroups.remove(groupId)
            localContact.groups = newGroups
            activity.contactsDB.insertOrUpdate(localContact)
        }
    }

    fun deleteContactIds(ids: Array<Int>) {
        ids.forEach {
            activity.contactsDB.deleteContactId(it)
        }
    }

    fun toggleFavorites(ids: Array<Int>, addToFavorites: Boolean) {
        val isStarred = if (addToFavorites) 1 else 0
        ids.forEach {
            activity.contactsDB.updateStarred(isStarred, it)
        }
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

    private fun convertLocalContactToContact(localContact: LocalContact?): Contact? {
        if (localContact == null) {
            return null
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

        val storedGroups = ContactsHelper(activity).getStoredGroupsSync()

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
            groups = storedGroups.filter { localContact.groups.contains(it.id) } as ArrayList<Group>
            organization = Organization(localContact.company, localContact.jobPosition)
            websites = localContact.websites
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
            groups = contact.groups.map { it.id }.toMutableList() as ArrayList<Long>
            company = contact.organization.company
            jobPosition = contact.organization.jobPosition
            websites = contact.websites
            IMs = contact.IMs
        }
    }
}
