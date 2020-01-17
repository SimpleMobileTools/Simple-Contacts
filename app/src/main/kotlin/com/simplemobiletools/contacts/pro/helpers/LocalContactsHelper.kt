package com.simplemobiletools.contacts.pro.helpers

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.MediaStore
import com.simplemobiletools.commons.extensions.getChoppedList
import com.simplemobiletools.contacts.pro.extensions.contactsDB
import com.simplemobiletools.contacts.pro.extensions.getByteArray
import com.simplemobiletools.contacts.pro.extensions.getEmptyContact
import com.simplemobiletools.contacts.pro.extensions.getPhotoThumbnailSize
import com.simplemobiletools.contacts.pro.models.Contact
import com.simplemobiletools.contacts.pro.models.Group
import com.simplemobiletools.contacts.pro.models.LocalContact
import com.simplemobiletools.contacts.pro.models.Organization

class LocalContactsHelper(val context: Context) {
    fun getAllContacts(): ArrayList<Contact> {
        val contacts = context.contactsDB.getContacts()
        val storedGroups = ContactsHelper(context).getStoredGroupsSync()
        return contacts.map { convertLocalContactToContact(it, storedGroups) }.toMutableList() as ArrayList<Contact>
    }

    fun getContactWithId(id: Int): Contact? {
        val storedGroups = ContactsHelper(context).getStoredGroupsSync()
        return convertLocalContactToContact(context.contactsDB.getContactWithId(id), storedGroups)
    }

    fun insertOrUpdateContact(contact: Contact): Boolean {
        val localContact = convertContactToLocalContact(contact)
        return context.contactsDB.insertOrUpdate(localContact) > 0
    }

    fun addContactsToGroup(contacts: ArrayList<Contact>, groupId: Long) {
        contacts.forEach {
            val localContact = convertContactToLocalContact(it)
            val newGroups = localContact.groups
            newGroups.add(groupId)
            newGroups.distinct()
            localContact.groups = newGroups
            context.contactsDB.insertOrUpdate(localContact)
        }
    }

    fun removeContactsFromGroup(contacts: ArrayList<Contact>, groupId: Long) {
        contacts.forEach {
            val localContact = convertContactToLocalContact(it)
            val newGroups = localContact.groups
            newGroups.remove(groupId)
            localContact.groups = newGroups
            context.contactsDB.insertOrUpdate(localContact)
        }
    }

    fun deleteContactIds(ids: MutableList<Long>) {
        ids.getChoppedList().forEach {
            context.contactsDB.deleteContactIds(it)
        }
    }

    fun toggleFavorites(ids: Array<Int>, addToFavorites: Boolean) {
        val isStarred = if (addToFavorites) 1 else 0
        ids.forEach {
            context.contactsDB.updateStarred(isStarred, it)
        }
    }

    private fun getPhotoByteArray(uri: String): ByteArray {
        if (uri.isEmpty()) {
            return ByteArray(0)
        }

        val photoUri = Uri.parse(uri)
        val bitmap = MediaStore.Images.Media.getBitmap(context.contentResolver, photoUri)

        val thumbnailSize = context.getPhotoThumbnailSize()
        val scaledPhoto = Bitmap.createScaledBitmap(bitmap, thumbnailSize * 2, thumbnailSize * 2, false)
        val scaledSizePhotoData = scaledPhoto.getByteArray()
        scaledPhoto.recycle()
        return scaledSizePhotoData
    }

    private fun convertLocalContactToContact(localContact: LocalContact?, storedGroups: ArrayList<Group>): Contact? {
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

        return context.getEmptyContact().apply {
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
        val photoByteArray = if (contact.photoUri.isNotEmpty()) {
            getPhotoByteArray(contact.photoUri)
        } else {
            contact.photo?.getByteArray()
        }

        return getEmptyLocalContact().apply {
            id = if (contact.id == 0) null else contact.id
            prefix = contact.prefix
            firstName = contact.firstName
            middleName = contact.middleName
            surname = contact.surname
            suffix = contact.suffix
            nickname = contact.nickname
            photo = photoByteArray
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
