package com.simplemobiletools.contacts.helpers

import android.content.ContentProviderOperation
import android.content.ContentProviderResult
import android.content.ContentUris
import android.content.ContentValues
import android.database.Cursor
import android.graphics.Bitmap
import android.net.Uri
import android.provider.ContactsContract
import android.provider.ContactsContract.CommonDataKinds
import android.provider.ContactsContract.CommonDataKinds.Note
import android.provider.MediaStore
import android.util.SparseArray
import com.simplemobiletools.commons.activities.BaseSimpleActivity
import com.simplemobiletools.commons.extensions.getIntValue
import com.simplemobiletools.commons.extensions.getStringValue
import com.simplemobiletools.commons.extensions.showErrorToast
import com.simplemobiletools.commons.extensions.toast
import com.simplemobiletools.commons.helpers.SORT_BY_FIRST_NAME
import com.simplemobiletools.commons.helpers.SORT_BY_MIDDLE_NAME
import com.simplemobiletools.commons.helpers.SORT_BY_SURNAME
import com.simplemobiletools.commons.helpers.SORT_DESCENDING
import com.simplemobiletools.contacts.R
import com.simplemobiletools.contacts.extensions.config
import com.simplemobiletools.contacts.extensions.dbHelper
import com.simplemobiletools.contacts.extensions.getByteArray
import com.simplemobiletools.contacts.extensions.getPhotoThumbnailSize
import com.simplemobiletools.contacts.models.*

class ContactsHelper(val activity: BaseSimpleActivity) {
    fun getContacts(callback: (ArrayList<Contact>) -> Unit) {
        val contacts = SparseArray<Contact>()
        Thread {
            val uri = ContactsContract.Data.CONTENT_URI
            val projection = getContactProjection()
            val selection = "${ContactsContract.Data.MIMETYPE} = ?"
            val selectionArgs = arrayOf(CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
            val sortOrder = getSortString()

            var cursor: Cursor? = null
            try {
                cursor = activity.contentResolver.query(uri, projection, selection, selectionArgs, sortOrder)
                if (cursor?.moveToFirst() == true) {
                    do {
                        val id = cursor.getIntValue(ContactsContract.Data.RAW_CONTACT_ID)
                        val firstName = cursor.getStringValue(CommonDataKinds.StructuredName.GIVEN_NAME) ?: ""
                        val middleName = cursor.getStringValue(CommonDataKinds.StructuredName.MIDDLE_NAME) ?: ""
                        val surname = cursor.getStringValue(CommonDataKinds.StructuredName.FAMILY_NAME) ?: ""
                        val photoUri = cursor.getStringValue(CommonDataKinds.StructuredName.PHOTO_URI) ?: ""
                        val number = ArrayList<PhoneNumber>()       // proper value is obtained below
                        val emails = ArrayList<Email>()
                        val addresses = ArrayList<Address>()
                        val events = ArrayList<Event>()
                        val accountName = cursor.getStringValue(ContactsContract.RawContacts.ACCOUNT_NAME) ?: ""
                        val starred = cursor.getIntValue(CommonDataKinds.StructuredName.STARRED)
                        val contactId = cursor.getIntValue(ContactsContract.Data.CONTACT_ID)
                        val thumbnailUri = cursor.getStringValue(CommonDataKinds.StructuredName.PHOTO_THUMBNAIL_URI) ?: ""
                        val notes = ""
                        val groups = ArrayList<Group>()
                        val contact = Contact(id, firstName, middleName, surname, photoUri, number, emails, addresses, events, accountName,
                                starred, contactId, thumbnailUri, null, notes, groups)
                        contacts.put(id, contact)
                    } while (cursor.moveToNext())
                }
            } catch (e: Exception) {
                activity.showErrorToast(e)
            } finally {
                cursor?.close()
            }

            val phoneNumbers = getPhoneNumbers()
            var size = phoneNumbers.size()
            for (i in 0 until size) {
                val key = phoneNumbers.keyAt(i)
                contacts[key]?.phoneNumbers = phoneNumbers.valueAt(i)
            }

            val emails = getEmails()
            size = emails.size()
            for (i in 0 until size) {
                val key = emails.keyAt(i)
                contacts[key]?.emails = emails.valueAt(i)
            }

            val addresses = getAddresses()
            size = addresses.size()
            for (i in 0 until size) {
                val key = addresses.keyAt(i)
                contacts[key]?.addresses = addresses.valueAt(i)
            }

            val events = getEvents()
            size = events.size()
            for (i in 0 until size) {
                val key = events.keyAt(i)
                contacts[key]?.events = events.valueAt(i)
            }

            val notes = getNotes()
            size = notes.size()
            for (i in 0 until size) {
                val key = notes.keyAt(i)
                contacts[key]?.notes = notes.valueAt(i)
            }

            val groups = getContactGroups(getStoredGroups())
            size = groups.size()
            for (i in 0 until size) {
                val key = groups.keyAt(i)
                contacts[key]?.groups = groups.valueAt(i)
            }

            activity.dbHelper.getContacts().forEach {
                contacts.put(it.id, it)
            }

            val contactsSize = contacts.size()
            var resultContacts = ArrayList<Contact>(contactsSize)
            (0 until contactsSize).mapTo(resultContacts) { contacts.valueAt(it) }
            resultContacts = resultContacts.distinctBy { it.contactId } as ArrayList<Contact>
            callback(resultContacts)
        }.start()
    }

    private fun getPhoneNumbers(contactId: Int? = null): SparseArray<ArrayList<PhoneNumber>> {
        val phoneNumbers = SparseArray<ArrayList<PhoneNumber>>()
        val uri = CommonDataKinds.Phone.CONTENT_URI
        val projection = arrayOf(
                ContactsContract.Data.RAW_CONTACT_ID,
                CommonDataKinds.Phone.NUMBER,
                CommonDataKinds.Phone.TYPE
        )

        val selection = if (contactId == null) null else "${ContactsContract.Data.RAW_CONTACT_ID} = ?"
        val selectionArgs = if (contactId == null) null else arrayOf(contactId.toString())

        var cursor: Cursor? = null
        try {
            cursor = activity.contentResolver.query(uri, projection, selection, selectionArgs, null)
            if (cursor?.moveToFirst() == true) {
                do {
                    val id = cursor.getIntValue(ContactsContract.Data.RAW_CONTACT_ID)
                    val number = cursor.getStringValue(CommonDataKinds.Phone.NUMBER) ?: continue
                    val type = cursor.getIntValue(CommonDataKinds.Phone.TYPE)

                    if (phoneNumbers[id] == null) {
                        phoneNumbers.put(id, ArrayList())
                    }

                    val phoneNumber = PhoneNumber(number, type)
                    phoneNumbers[id].add(phoneNumber)
                } while (cursor.moveToNext())
            }
        } catch (e: Exception) {
            activity.showErrorToast(e)
        } finally {
            cursor?.close()
        }
        return phoneNumbers
    }

    private fun getEmails(contactId: Int? = null): SparseArray<ArrayList<Email>> {
        val emails = SparseArray<ArrayList<Email>>()
        val uri = CommonDataKinds.Email.CONTENT_URI
        val projection = arrayOf(
                ContactsContract.Data.RAW_CONTACT_ID,
                CommonDataKinds.Email.DATA,
                CommonDataKinds.Email.TYPE
        )

        val selection = if (contactId == null) null else "${ContactsContract.Data.RAW_CONTACT_ID} = ?"
        val selectionArgs = if (contactId == null) null else arrayOf(contactId.toString())

        var cursor: Cursor? = null
        try {
            cursor = activity.contentResolver.query(uri, projection, selection, selectionArgs, null)
            if (cursor?.moveToFirst() == true) {
                do {
                    val id = cursor.getIntValue(ContactsContract.Data.RAW_CONTACT_ID)
                    val email = cursor.getStringValue(CommonDataKinds.Email.DATA) ?: continue
                    val type = cursor.getIntValue(CommonDataKinds.Email.TYPE)

                    if (emails[id] == null) {
                        emails.put(id, ArrayList())
                    }

                    emails[id]!!.add(Email(email, type))
                } while (cursor.moveToNext())
            }

        } catch (e: Exception) {
            activity.showErrorToast(e)
        } finally {
            cursor?.close()
        }

        return emails
    }

    private fun getAddresses(contactId: Int? = null): SparseArray<ArrayList<Address>> {
        val addresses = SparseArray<ArrayList<Address>>()
        val uri = CommonDataKinds.StructuredPostal.CONTENT_URI
        val projection = arrayOf(
                ContactsContract.Data.RAW_CONTACT_ID,
                CommonDataKinds.StructuredPostal.FORMATTED_ADDRESS,
                CommonDataKinds.StructuredPostal.TYPE
        )

        val selection = if (contactId == null) null else "${ContactsContract.Data.RAW_CONTACT_ID} = ?"
        val selectionArgs = if (contactId == null) null else arrayOf(contactId.toString())

        var cursor: Cursor? = null
        try {
            cursor = activity.contentResolver.query(uri, projection, selection, selectionArgs, null)
            if (cursor?.moveToFirst() == true) {
                do {
                    val id = cursor.getIntValue(ContactsContract.Data.RAW_CONTACT_ID)
                    val address = cursor.getStringValue(CommonDataKinds.StructuredPostal.FORMATTED_ADDRESS)
                    val type = cursor.getIntValue(CommonDataKinds.StructuredPostal.TYPE)

                    if (addresses[id] == null) {
                        addresses.put(id, ArrayList())
                    }

                    addresses[id]!!.add(Address(address, type))
                } while (cursor.moveToNext())
            }

        } catch (e: Exception) {
            activity.showErrorToast(e)
        } finally {
            cursor?.close()
        }

        return addresses
    }

    private fun getEvents(contactId: Int? = null): SparseArray<ArrayList<Event>> {
        val events = SparseArray<ArrayList<Event>>()
        val uri = ContactsContract.Data.CONTENT_URI
        val projection = arrayOf(
                ContactsContract.Data.RAW_CONTACT_ID,
                CommonDataKinds.Event.START_DATE,
                CommonDataKinds.Event.TYPE
        )

        var selection = "${ContactsContract.Data.MIMETYPE} = ?"
        var selectionArgs = arrayOf(CommonDataKinds.Event.CONTENT_ITEM_TYPE)

        if (contactId != null) {
            selection += " AND ${ContactsContract.Data.RAW_CONTACT_ID} = ?"
            selectionArgs = arrayOf(CommonDataKinds.Event.CONTENT_ITEM_TYPE, contactId.toString())
        }

        var cursor: Cursor? = null
        try {
            cursor = activity.contentResolver.query(uri, projection, selection, selectionArgs, null)
            if (cursor?.moveToFirst() == true) {
                do {
                    val id = cursor.getIntValue(ContactsContract.Data.RAW_CONTACT_ID)
                    val startDate = cursor.getStringValue(CommonDataKinds.Event.START_DATE) ?: continue
                    val type = cursor.getIntValue(CommonDataKinds.Event.TYPE)

                    if (events[id] == null) {
                        events.put(id, ArrayList())
                    }

                    events[id]!!.add(Event(startDate, type))
                } while (cursor.moveToNext())
            }
        } catch (e: Exception) {
            activity.showErrorToast(e)
        } finally {
            cursor?.close()
        }

        return events
    }

    private fun getNotes(contactId: Int? = null): SparseArray<String> {
        val notes = SparseArray<String>()
        val uri = ContactsContract.Data.CONTENT_URI
        val projection = arrayOf(
                ContactsContract.Data.RAW_CONTACT_ID,
                Note.NOTE
        )

        var selection = "${ContactsContract.Data.MIMETYPE} = ?"
        var selectionArgs = arrayOf(Note.CONTENT_ITEM_TYPE)

        if (contactId != null) {
            selection += " AND ${ContactsContract.Data.RAW_CONTACT_ID} = ?"
            selectionArgs = arrayOf(Note.CONTENT_ITEM_TYPE, contactId.toString())
        }

        var cursor: Cursor? = null
        try {
            cursor = activity.contentResolver.query(uri, projection, selection, selectionArgs, null)
            if (cursor?.moveToFirst() == true) {
                do {
                    val id = cursor.getIntValue(ContactsContract.Data.RAW_CONTACT_ID)
                    val note = cursor.getStringValue(CommonDataKinds.Note.NOTE) ?: continue
                    notes.put(id, note)
                } while (cursor.moveToNext())
            }
        } catch (e: Exception) {
            activity.showErrorToast(e)
        } finally {
            cursor?.close()
        }

        return notes
    }

    private fun getContactGroups(storedGroups: ArrayList<Group>, contactId: Int? = null): SparseArray<ArrayList<Group>> {
        val groups = SparseArray<ArrayList<Group>>()
        val uri = ContactsContract.Data.CONTENT_URI
        val projection = arrayOf(
                ContactsContract.Data.CONTACT_ID,
                ContactsContract.Data.DATA1
        )

        var selection = "${ContactsContract.Data.MIMETYPE} = ?"
        var selectionArgs = arrayOf(CommonDataKinds.GroupMembership.CONTENT_ITEM_TYPE)

        if (contactId != null) {
            selection += " AND ${ContactsContract.Data.CONTACT_ID} = ?"
            selectionArgs = arrayOf(CommonDataKinds.GroupMembership.CONTENT_ITEM_TYPE, contactId.toString())
        }

        var cursor: Cursor? = null
        try {
            cursor = activity.contentResolver.query(uri, projection, selection, selectionArgs, null)
            if (cursor?.moveToFirst() == true) {
                do {
                    val id = cursor.getIntValue(ContactsContract.Data.CONTACT_ID)
                    val newRowId = cursor.getIntValue(ContactsContract.Data.DATA1)

                    if (groups[id] == null) {
                        groups.put(id, ArrayList())
                    }

                    val groupTitle = storedGroups.firstOrNull { it.id == newRowId }?.title ?: continue
                    val group = Group(newRowId, groupTitle)
                    groups[id]!!.add(group)
                } while (cursor.moveToNext())
            }
        } catch (e: Exception) {
            activity.showErrorToast(e)
        } finally {
            cursor?.close()
        }

        return groups
    }

    fun getStoredGroups(): ArrayList<Group> {
        val groups = ArrayList<Group>()
        val uri = ContactsContract.Groups.CONTENT_URI
        val projection = arrayOf(
                ContactsContract.Groups._ID,
                ContactsContract.Groups.TITLE
        )

        val selection = "${ContactsContract.Groups.AUTO_ADD} = ? AND ${ContactsContract.Groups.FAVORITES} = ?"
        val selectionArgs = arrayOf("0", "0")

        var cursor: Cursor? = null
        try {
            cursor = activity.contentResolver.query(uri, projection, selection, selectionArgs, null)
            if (cursor?.moveToFirst() == true) {
                do {
                    val id = cursor.getIntValue(ContactsContract.Groups._ID)
                    val title = cursor.getStringValue(ContactsContract.Groups.TITLE)
                    groups.add(Group(id, title))
                } while (cursor.moveToNext())
            }
        } catch (e: Exception) {
            activity.showErrorToast(e)
        } finally {
            cursor?.close()
        }

        return groups
    }

    fun getContactWithId(id: Int, isLocalPrivate: Boolean): Contact? {
        if (id == 0) {
            return null
        } else if (isLocalPrivate) {
            return activity.dbHelper.getContactWithId(id)
        }

        val selection = "${ContactsContract.Data.MIMETYPE} = ? AND ${ContactsContract.Data.RAW_CONTACT_ID} = ?"
        val selectionArgs = arrayOf(CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE, id.toString())
        return parseContactCursor(selection, selectionArgs)
    }

    fun getContactWithLookupKey(key: String): Contact? {
        val selection = "${ContactsContract.Data.MIMETYPE} = ? AND ${ContactsContract.Data.LOOKUP_KEY} = ?"
        val selectionArgs = arrayOf(CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE, key)
        return parseContactCursor(selection, selectionArgs)
    }

    private fun parseContactCursor(selection: String, selectionArgs: Array<String>): Contact? {
        val storedGroups = getStoredGroups()
        val uri = ContactsContract.Data.CONTENT_URI
        val projection = getContactProjection()
        var cursor: Cursor? = null
        try {
            cursor = activity.contentResolver.query(uri, projection, selection, selectionArgs, null)
            if (cursor?.moveToFirst() == true) {
                val id = cursor.getIntValue(ContactsContract.Data.RAW_CONTACT_ID)
                val firstName = cursor.getStringValue(CommonDataKinds.StructuredName.GIVEN_NAME) ?: ""
                val middleName = cursor.getStringValue(CommonDataKinds.StructuredName.MIDDLE_NAME) ?: ""
                val surname = cursor.getStringValue(CommonDataKinds.StructuredName.FAMILY_NAME) ?: ""
                val photoUri = cursor.getStringValue(CommonDataKinds.Phone.PHOTO_URI) ?: ""
                val number = getPhoneNumbers(id)[id] ?: ArrayList()
                val emails = getEmails(id)[id] ?: ArrayList()
                val addresses = getAddresses(id)[id] ?: ArrayList()
                val events = getEvents(id)[id] ?: ArrayList()
                val notes = getNotes(id)[id] ?: ""
                val accountName = cursor.getStringValue(ContactsContract.RawContacts.ACCOUNT_NAME) ?: ""
                val starred = cursor.getIntValue(CommonDataKinds.StructuredName.STARRED)
                val contactId = cursor.getIntValue(ContactsContract.Data.CONTACT_ID)
                val groups = getContactGroups(storedGroups, contactId)[contactId] ?: ArrayList()
                val thumbnailUri = cursor.getStringValue(CommonDataKinds.StructuredName.PHOTO_THUMBNAIL_URI) ?: ""
                return Contact(id, firstName, middleName, surname, photoUri, number, emails, addresses, events, accountName, starred, contactId,
                        thumbnailUri, null, notes, groups)
            }
        } finally {
            cursor?.close()
        }

        return null
    }

    fun getContactSources(callback: (ArrayList<ContactSource>) -> Unit) {
        val sources = LinkedHashSet<ContactSource>()
        Thread {
            val uri = ContactsContract.RawContacts.CONTENT_URI
            val projection = arrayOf(ContactsContract.RawContacts.ACCOUNT_NAME, ContactsContract.RawContacts.ACCOUNT_TYPE)

            var cursor: Cursor? = null
            try {
                cursor = activity.contentResolver.query(uri, projection, null, null, null)
                if (cursor?.moveToFirst() == true) {
                    do {
                        val name = cursor.getStringValue(ContactsContract.RawContacts.ACCOUNT_NAME) ?: continue
                        val type = cursor.getStringValue(ContactsContract.RawContacts.ACCOUNT_TYPE) ?: continue
                        val contactSource = ContactSource(name, type)
                        sources.add(contactSource)
                    } while (cursor.moveToNext())
                }
            } catch (e: Exception) {
                activity.showErrorToast(e)
            } finally {
                cursor?.close()
            }

            sources.add(ContactSource(activity.getString(R.string.phone_storage_hidden), SMT_PRIVATE))
            callback(ArrayList(sources))
        }.start()
    }

    private fun getContactSourceType(accountName: String): String {
        if (accountName.isEmpty()) {
            return ""
        }

        val uri = ContactsContract.RawContacts.CONTENT_URI
        val projection = arrayOf(ContactsContract.RawContacts.ACCOUNT_TYPE)
        val selection = "${ContactsContract.RawContacts.ACCOUNT_NAME} = ?"
        val selectionArgs = arrayOf(accountName)

        var cursor: Cursor? = null
        try {
            cursor = activity.contentResolver.query(uri, projection, selection, selectionArgs, null)
            if (cursor?.moveToFirst() == true) {
                return cursor.getStringValue(ContactsContract.RawContacts.ACCOUNT_TYPE)
            }
        } finally {
            cursor?.close()
        }
        return ""
    }

    private fun getContactProjection() = arrayOf(
            ContactsContract.Data.CONTACT_ID,
            ContactsContract.Data.RAW_CONTACT_ID,
            CommonDataKinds.StructuredName.GIVEN_NAME,
            CommonDataKinds.StructuredName.MIDDLE_NAME,
            CommonDataKinds.StructuredName.FAMILY_NAME,
            CommonDataKinds.StructuredName.PHOTO_URI,
            CommonDataKinds.StructuredName.PHOTO_THUMBNAIL_URI,
            CommonDataKinds.StructuredName.STARRED,
            ContactsContract.RawContacts.ACCOUNT_NAME
    )

    private fun getSortString(): String {
        val sorting = activity.config.sorting
        var sort = when {
            sorting and SORT_BY_FIRST_NAME != 0 -> "${CommonDataKinds.StructuredName.GIVEN_NAME} COLLATE NOCASE"
            sorting and SORT_BY_MIDDLE_NAME != 0 -> "${CommonDataKinds.StructuredName.MIDDLE_NAME} COLLATE NOCASE"
            sorting and SORT_BY_SURNAME != 0 -> "${CommonDataKinds.StructuredName.FAMILY_NAME} COLLATE NOCASE"
            else -> CommonDataKinds.Phone.NUMBER
        }

        if (sorting and SORT_DESCENDING != 0) {
            sort += " DESC"
        }

        return sort
    }

    private fun getRealContactId(id: Long): Int {
        val uri = ContactsContract.Data.CONTENT_URI
        val projection = getContactProjection()
        val selection = "${ContactsContract.Data.MIMETYPE} = ? AND ${ContactsContract.Data.RAW_CONTACT_ID} = ?"
        val selectionArgs = arrayOf(CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE, id.toString())
        var cursor: Cursor? = null
        try {
            cursor = activity.contentResolver.query(uri, projection, selection, selectionArgs, null)
            if (cursor?.moveToFirst() == true) {
                return cursor.getIntValue(ContactsContract.Data.CONTACT_ID)
            }
        } finally {
            cursor?.close()
        }

        return 0
    }

    fun updateContact(contact: Contact, photoUpdateStatus: Int): Boolean {
        return if (contact.source == SMT_PRIVATE) {
            activity.dbHelper.update(contact)
        } else try {
            activity.toast(R.string.updating)
            val operations = ArrayList<ContentProviderOperation>()
            ContentProviderOperation.newUpdate(ContactsContract.Data.CONTENT_URI).apply {
                val selection = "${ContactsContract.Data.RAW_CONTACT_ID} = ? AND ${ContactsContract.Data.MIMETYPE} = ?"
                val selectionArgs = arrayOf(contact.id.toString(), CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                withSelection(selection, selectionArgs)
                withValue(CommonDataKinds.StructuredName.GIVEN_NAME, contact.firstName)
                withValue(CommonDataKinds.StructuredName.MIDDLE_NAME, contact.middleName)
                withValue(CommonDataKinds.StructuredName.FAMILY_NAME, contact.surname)
                operations.add(build())
            }

            // delete phone numbers
            ContentProviderOperation.newDelete(ContactsContract.Data.CONTENT_URI).apply {
                val selection = "${ContactsContract.Data.RAW_CONTACT_ID} = ? AND ${ContactsContract.Data.MIMETYPE} = ? "
                val selectionArgs = arrayOf(contact.id.toString(), CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                withSelection(selection, selectionArgs)
                operations.add(build())
            }

            // add phone numbers
            contact.phoneNumbers.forEach {
                ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI).apply {
                    withValue(ContactsContract.Data.RAW_CONTACT_ID, contact.id)
                    withValue(ContactsContract.Data.MIMETYPE, CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                    withValue(CommonDataKinds.Phone.NUMBER, it.value)
                    withValue(CommonDataKinds.Phone.TYPE, it.type)
                    operations.add(build())
                }
            }

            // delete emails
            ContentProviderOperation.newDelete(ContactsContract.Data.CONTENT_URI).apply {
                val selection = "${ContactsContract.Data.RAW_CONTACT_ID} = ? AND ${ContactsContract.Data.MIMETYPE} = ? "
                val selectionArgs = arrayOf(contact.id.toString(), CommonDataKinds.Email.CONTENT_ITEM_TYPE)
                withSelection(selection, selectionArgs)
                operations.add(build())
            }

            // add emails
            contact.emails.forEach {
                ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI).apply {
                    withValue(ContactsContract.Data.RAW_CONTACT_ID, contact.id)
                    withValue(ContactsContract.Data.MIMETYPE, CommonDataKinds.Email.CONTENT_ITEM_TYPE)
                    withValue(CommonDataKinds.Email.DATA, it.value)
                    withValue(CommonDataKinds.Email.TYPE, it.type)
                    operations.add(build())
                }
            }

            // delete addresses
            ContentProviderOperation.newDelete(ContactsContract.Data.CONTENT_URI).apply {
                val selection = "${ContactsContract.Data.RAW_CONTACT_ID} = ? AND ${ContactsContract.Data.MIMETYPE} = ? "
                val selectionArgs = arrayOf(contact.id.toString(), CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE)
                withSelection(selection, selectionArgs)
                operations.add(build())
            }

            // add addresses
            contact.addresses.forEach {
                ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI).apply {
                    withValue(ContactsContract.Data.RAW_CONTACT_ID, contact.id)
                    withValue(ContactsContract.Data.MIMETYPE, CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE)
                    withValue(CommonDataKinds.StructuredPostal.FORMATTED_ADDRESS, it.value)
                    withValue(CommonDataKinds.StructuredPostal.TYPE, it.type)
                    operations.add(build())
                }
            }

            // delete events
            ContentProviderOperation.newDelete(ContactsContract.Data.CONTENT_URI).apply {
                val selection = "${ContactsContract.Data.RAW_CONTACT_ID} = ? AND ${ContactsContract.Data.MIMETYPE} = ? "
                val selectionArgs = arrayOf(contact.id.toString(), CommonDataKinds.Event.CONTENT_ITEM_TYPE)
                withSelection(selection, selectionArgs)
                operations.add(build())
            }

            // add events
            contact.events.forEach {
                ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI).apply {
                    withValue(ContactsContract.Data.RAW_CONTACT_ID, contact.id)
                    withValue(ContactsContract.Data.MIMETYPE, CommonDataKinds.Event.CONTENT_ITEM_TYPE)
                    withValue(CommonDataKinds.Event.START_DATE, it.value)
                    withValue(CommonDataKinds.Event.TYPE, it.type)
                    operations.add(build())
                }
            }

            // notes
            ContentProviderOperation.newUpdate(ContactsContract.Data.CONTENT_URI).apply {
                val selection = "${ContactsContract.Data.RAW_CONTACT_ID} = ? AND ${ContactsContract.Data.MIMETYPE} = ?"
                val selectionArgs = arrayOf(contact.id.toString(), Note.CONTENT_ITEM_TYPE)
                withSelection(selection, selectionArgs)
                withValue(Note.NOTE, contact.notes)
                operations.add(build())
            }

            // favorite
            try {
                val uri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_URI, contact.contactId.toString())
                val contentValues = ContentValues(1)
                contentValues.put(ContactsContract.Contacts.STARRED, contact.starred)
                activity.contentResolver.update(uri, contentValues, null, null)
            } catch (e: Exception) {
                activity.showErrorToast(e)
            }

            // photo
            when (photoUpdateStatus) {
                PHOTO_ADDED, PHOTO_CHANGED -> addPhoto(contact, operations)
                PHOTO_REMOVED -> removePhoto(contact, operations)
            }

            activity.contentResolver.applyBatch(ContactsContract.AUTHORITY, operations)
            true
        } catch (e: Exception) {
            activity.showErrorToast(e)
            false
        }
    }

    private fun addPhoto(contact: Contact, operations: ArrayList<ContentProviderOperation>): ArrayList<ContentProviderOperation> {
        if (contact.photoUri.isNotEmpty()) {
            val photoUri = Uri.parse(contact.photoUri)
            val bitmap = MediaStore.Images.Media.getBitmap(activity.contentResolver, photoUri)

            val thumbnailSize = activity.getPhotoThumbnailSize()
            val scaledPhoto = Bitmap.createScaledBitmap(bitmap, thumbnailSize, thumbnailSize, false)
            val scaledSizePhotoData = scaledPhoto.getByteArray()
            scaledPhoto.recycle()

            val fullSizePhotoData = bitmap.getByteArray()
            bitmap.recycle()

            ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI).apply {
                withValue(ContactsContract.Data.RAW_CONTACT_ID, contact.id)
                withValue(ContactsContract.Data.MIMETYPE, CommonDataKinds.Photo.CONTENT_ITEM_TYPE)
                withValue(CommonDataKinds.Photo.PHOTO, scaledSizePhotoData)
                operations.add(build())
            }

            addFullSizePhoto(contact.id.toLong(), fullSizePhotoData)
        }
        return operations
    }

    private fun removePhoto(contact: Contact, operations: ArrayList<ContentProviderOperation>): ArrayList<ContentProviderOperation> {
        ContentProviderOperation.newDelete(ContactsContract.Data.CONTENT_URI).apply {
            val selection = "${ContactsContract.Data.RAW_CONTACT_ID} = ? AND ${ContactsContract.Data.MIMETYPE} = ?"
            val selectionArgs = arrayOf(contact.id.toString(), CommonDataKinds.Photo.CONTENT_ITEM_TYPE)
            withSelection(selection, selectionArgs)
            operations.add(build())
        }

        return operations
    }

    fun insertContact(contact: Contact): Boolean {
        return if (contact.source == SMT_PRIVATE) {
            insertLocalContact(contact)
        } else {
            try {
                val operations = ArrayList<ContentProviderOperation>()
                ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI).apply {
                    withValue(ContactsContract.RawContacts.ACCOUNT_NAME, contact.source)
                    withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, getContactSourceType(contact.source))
                    operations.add(build())
                }

                // names
                ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI).apply {
                    withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                    withValue(ContactsContract.Data.MIMETYPE, CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                    withValue(CommonDataKinds.StructuredName.GIVEN_NAME, contact.firstName)
                    withValue(CommonDataKinds.StructuredName.MIDDLE_NAME, contact.middleName)
                    withValue(CommonDataKinds.StructuredName.FAMILY_NAME, contact.surname)
                    operations.add(build())
                }

                // phone numbers
                contact.phoneNumbers.forEach {
                    ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI).apply {
                        withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                        withValue(ContactsContract.Data.MIMETYPE, CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                        withValue(CommonDataKinds.Phone.NUMBER, it.value)
                        withValue(CommonDataKinds.Phone.TYPE, it.type)
                        operations.add(build())
                    }
                }

                // emails
                contact.emails.forEach {
                    ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI).apply {
                        withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                        withValue(ContactsContract.Data.MIMETYPE, CommonDataKinds.Email.CONTENT_ITEM_TYPE)
                        withValue(CommonDataKinds.Email.DATA, it.value)
                        withValue(CommonDataKinds.Email.TYPE, it.type)
                        operations.add(build())
                    }
                }

                // addresses
                contact.addresses.forEach {
                    ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI).apply {
                        withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                        withValue(ContactsContract.Data.MIMETYPE, CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE)
                        withValue(CommonDataKinds.StructuredPostal.FORMATTED_ADDRESS, it.value)
                        withValue(CommonDataKinds.StructuredPostal.TYPE, it.type)
                        operations.add(build())
                    }
                }

                // events
                contact.events.forEach {
                    ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI).apply {
                        withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                        withValue(ContactsContract.Data.MIMETYPE, CommonDataKinds.Event.CONTENT_ITEM_TYPE)
                        withValue(CommonDataKinds.Event.START_DATE, it.value)
                        withValue(CommonDataKinds.Event.TYPE, it.type)
                        operations.add(build())
                    }
                }

                // notes
                ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI).apply {
                    withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                    withValue(ContactsContract.Data.MIMETYPE, Note.CONTENT_ITEM_TYPE)
                    withValue(Note.NOTE, contact.notes)
                    operations.add(build())
                }

                // photo (inspired by https://gist.github.com/slightfoot/5985900)
                var fullSizePhotoData: ByteArray? = null
                var scaledSizePhotoData: ByteArray?
                if (contact.photoUri.isNotEmpty()) {
                    val photoUri = Uri.parse(contact.photoUri)
                    val bitmap = MediaStore.Images.Media.getBitmap(activity.contentResolver, photoUri)

                    val thumbnailSize = activity.getPhotoThumbnailSize()
                    val scaledPhoto = Bitmap.createScaledBitmap(bitmap, thumbnailSize, thumbnailSize, false)
                    scaledSizePhotoData = scaledPhoto.getByteArray()

                    fullSizePhotoData = bitmap.getByteArray()
                    scaledPhoto.recycle()
                    bitmap.recycle()

                    ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI).apply {
                        withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                        withValue(ContactsContract.Data.MIMETYPE, CommonDataKinds.Photo.CONTENT_ITEM_TYPE)
                        withValue(CommonDataKinds.Photo.PHOTO, scaledSizePhotoData)
                        operations.add(build())
                    }
                }

                val results: Array<ContentProviderResult>
                try {
                    results = activity.contentResolver.applyBatch(ContactsContract.AUTHORITY, operations)
                } finally {
                    scaledSizePhotoData = null
                }

                // fullsize photo
                val rawId = ContentUris.parseId(results[0].uri)
                if (contact.photoUri.isNotEmpty() && fullSizePhotoData != null) {
                    addFullSizePhoto(rawId, fullSizePhotoData)
                }

                // favorite
                val userId = getRealContactId(rawId)
                if (userId != 0 && contact.starred == 1) {
                    val uri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_URI, userId.toString())
                    val contentValues = ContentValues(1)
                    contentValues.put(ContactsContract.Contacts.STARRED, contact.starred)
                    activity.contentResolver.update(uri, contentValues, null, null)
                }

                true
            } catch (e: Exception) {
                activity.showErrorToast(e)
                false
            }
        }
    }

    private fun insertLocalContact(contact: Contact) = activity.dbHelper.insert(contact)

    private fun addFullSizePhoto(contactId: Long, fullSizePhotoData: ByteArray) {
        val baseUri = ContentUris.withAppendedId(ContactsContract.RawContacts.CONTENT_URI, contactId)
        val displayPhotoUri = Uri.withAppendedPath(baseUri, ContactsContract.RawContacts.DisplayPhoto.CONTENT_DIRECTORY)
        val fileDescriptor = activity.contentResolver.openAssetFileDescriptor(displayPhotoUri, "rw")
        val photoStream = fileDescriptor.createOutputStream()
        photoStream.write(fullSizePhotoData)
        photoStream.close()
        fileDescriptor.close()
    }

    fun getContactLookupKey(contactId: String): String {
        val uri = ContactsContract.Data.CONTENT_URI
        val projection = arrayOf(ContactsContract.Data.CONTACT_ID, ContactsContract.Data.LOOKUP_KEY)
        val selection = "${ContactsContract.Data.MIMETYPE} = ? AND ${ContactsContract.Data.RAW_CONTACT_ID} = ?"
        val selectionArgs = arrayOf(CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE, contactId)
        var cursor: Cursor? = null
        try {
            cursor = activity.contentResolver.query(uri, projection, selection, selectionArgs, null)
            if (cursor?.moveToFirst() == true) {
                val id = cursor.getIntValue(ContactsContract.Data.CONTACT_ID)
                val lookupKey = cursor.getStringValue(ContactsContract.Data.LOOKUP_KEY)
                return "$lookupKey/$id"
            }
        } finally {
            cursor?.close()
        }
        return ""
    }

    fun getContactDataId(contactId: String): String {
        val uri = ContactsContract.Data.CONTENT_URI
        val projection = arrayOf(ContactsContract.Data._ID, ContactsContract.Data.RAW_CONTACT_ID, ContactsContract.Data.MIMETYPE)
        val selection = "${ContactsContract.Data.MIMETYPE} = ? AND ${ContactsContract.Data.RAW_CONTACT_ID} = ?"
        val selectionArgs = arrayOf(CommonDataKinds.Email.CONTENT_ITEM_TYPE, contactId)

        var cursor: Cursor? = null
        try {
            cursor = activity.contentResolver.query(uri, projection, selection, selectionArgs, null)
            if (cursor?.moveToFirst() == true) {
                return cursor.getStringValue(ContactsContract.Data._ID)
            }
        } finally {
            cursor?.close()
        }
        return ""
    }

    fun addFavorites(contacts: ArrayList<Contact>) {
        toggleLocalFavorites(contacts, true)
        toggleFavorites(contacts, true)
    }

    fun removeFavorites(contacts: ArrayList<Contact>) {
        toggleLocalFavorites(contacts, false)
        toggleFavorites(contacts, false)
    }

    private fun toggleFavorites(contacts: ArrayList<Contact>, addToFavorites: Boolean) {
        val applyBatchSize = 100
        try {
            val operations = ArrayList<ContentProviderOperation>()
            contacts.filter { it.source != SMT_PRIVATE }.map { it.contactId.toString() }.forEach {
                val uri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_URI, it)
                ContentProviderOperation.newUpdate(uri).apply {
                    withValue(ContactsContract.Contacts.STARRED, if (addToFavorites) 1 else 0)
                    operations.add(build())
                }

                if (operations.size % applyBatchSize == 0) {
                    activity.contentResolver.applyBatch(ContactsContract.AUTHORITY, operations)
                    operations.clear()
                }
            }
            activity.contentResolver.applyBatch(ContactsContract.AUTHORITY, operations)
        } catch (e: Exception) {
            activity.showErrorToast(e)
        }
    }

    private fun toggleLocalFavorites(contacts: ArrayList<Contact>, addToFavorites: Boolean) {
        val localContacts = contacts.filter { it.source == SMT_PRIVATE }.map { it.id.toString() }.toTypedArray()
        activity.dbHelper.toggleFavorites(localContacts, addToFavorites)
    }

    fun deleteContact(contact: Contact) {
        if (contact.source == SMT_PRIVATE) {
            activity.dbHelper.deleteContact(contact.id)
        } else {
            deleteContacts(arrayListOf(contact))
        }
    }

    fun deleteContacts(contacts: ArrayList<Contact>) {
        val localContacts = contacts.filter { it.source == SMT_PRIVATE }.map { it.id.toString() }.toTypedArray()
        activity.dbHelper.deleteContacts(localContacts)

        try {
            val contactIDs = HashSet<String>()
            val operations = ArrayList<ContentProviderOperation>()
            val selection = "${ContactsContract.Data.RAW_CONTACT_ID} = ?"
            contacts.filter { it.source != SMT_PRIVATE }.forEach {
                ContentProviderOperation.newDelete(ContactsContract.Data.CONTENT_URI).apply {
                    val selectionArgs = arrayOf(it.id.toString())
                    withSelection(selection, selectionArgs)
                    operations.add(this.build())
                }
                contactIDs.add(it.id.toString())
            }

            activity.contentResolver.applyBatch(ContactsContract.AUTHORITY, operations)
        } catch (e: Exception) {
            activity.showErrorToast(e)
        }
    }
}
