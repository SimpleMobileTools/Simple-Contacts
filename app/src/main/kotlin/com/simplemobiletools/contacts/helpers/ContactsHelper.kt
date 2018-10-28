package com.simplemobiletools.contacts.helpers

import android.accounts.Account
import android.accounts.AccountManager
import android.annotation.SuppressLint
import android.app.Activity
import android.content.*
import android.database.Cursor
import android.graphics.Bitmap
import android.net.Uri
import android.provider.CallLog
import android.provider.ContactsContract
import android.provider.ContactsContract.CommonDataKinds
import android.provider.ContactsContract.CommonDataKinds.Nickname
import android.provider.ContactsContract.CommonDataKinds.Note
import android.provider.MediaStore
import android.text.TextUtils
import android.util.SparseArray
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.helpers.*
import com.simplemobiletools.contacts.R
import com.simplemobiletools.contacts.extensions.*
import com.simplemobiletools.contacts.models.*
import com.simplemobiletools.contacts.overloads.times
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class ContactsHelper(val activity: Activity) {
    private val BATCH_SIZE = 100
    private var displayContactSources = ArrayList<String>()

    fun getContacts(callback: (ArrayList<Contact>) -> Unit) {
        Thread {
            val contacts = SparseArray<Contact>()
            displayContactSources = activity.getVisibleContactSources()
            getDeviceContacts(contacts)

            if (displayContactSources.contains(SMT_PRIVATE)) {
                activity.dbHelper.getContacts(activity).forEach {
                    contacts.put(it.id, it)
                }
            }

            val contactsSize = contacts.size()
            val showOnlyContactsWithNumbers = activity.config.showOnlyContactsWithNumbers
            var tempContacts = ArrayList<Contact>(contactsSize)
            val resultContacts = ArrayList<Contact>(contactsSize)

            (0 until contactsSize).filter {
                if (showOnlyContactsWithNumbers) {
                    contacts.valueAt(it).phoneNumbers.isNotEmpty()
                } else {
                    true
                }
            }.mapTo(tempContacts) {
                contacts.valueAt(it)
            }

            if (activity.config.filterDuplicates) {
                tempContacts = tempContacts.distinctBy {
                    it.getHashToCompare()
                } as ArrayList<Contact>

                tempContacts.groupBy { "${it.getNameToDisplay().toLowerCase()}${it.emails}" }.values.forEach {
                    if (it.size == 1) {
                        resultContacts.add(it.first())
                    } else {
                        val sorted = it.sortedByDescending { it.getStringToCompare().length }
                        resultContacts.add(sorted.first())
                    }
                }
            } else {
                resultContacts.addAll(tempContacts)
            }

            // groups are obtained with contactID, not rawID, so assign them to proper contacts like this
            val groups = getContactGroups(getStoredGroups())
            val size = groups.size()
            for (i in 0 until size) {
                val key = groups.keyAt(i)
                resultContacts.firstOrNull { it.contactId == key }?.groups = groups.valueAt(i)
            }

            activity.runOnUiThread {
                callback(resultContacts)
            }
        }.start()
    }

    private fun getContentResolverAccounts(): HashSet<ContactSource> {
        val uri = ContactsContract.Data.CONTENT_URI
        val projection = arrayOf(
                ContactsContract.RawContacts.ACCOUNT_NAME,
                ContactsContract.RawContacts.ACCOUNT_TYPE
        )

        val sources = HashSet<ContactSource>()
        var cursor: Cursor? = null
        try {
            cursor = activity.contentResolver.query(uri, projection, null, null, null)
            if (cursor?.moveToFirst() == true) {
                do {
                    val name = cursor.getStringValue(ContactsContract.RawContacts.ACCOUNT_NAME) ?: ""
                    val type = cursor.getStringValue(ContactsContract.RawContacts.ACCOUNT_TYPE) ?: ""
                    val source = ContactSource(name, type)
                    sources.add(source)
                } while (cursor.moveToNext())
            }
        } catch (e: Exception) {
        } finally {
            cursor?.close()
        }

        return sources
    }

    private fun getDeviceContacts(contacts: SparseArray<Contact>) {
        if (!activity.hasContactPermissions()) {
            return
        }

        val uri = ContactsContract.Data.CONTENT_URI
        val projection = getContactProjection()
        val selection = getSourcesSelection(true)
        val selectionArgs = getSourcesSelectionArgs(CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
        val sortOrder = getSortString()

        var cursor: Cursor? = null
        try {
            cursor = activity.contentResolver.query(uri, projection, selection, selectionArgs, sortOrder)
            if (cursor?.moveToFirst() == true) {
                do {
                    val id = cursor.getIntValue(ContactsContract.Data.RAW_CONTACT_ID)
                    val prefix = cursor.getStringValue(CommonDataKinds.StructuredName.PREFIX) ?: ""
                    val firstName = cursor.getStringValue(CommonDataKinds.StructuredName.GIVEN_NAME) ?: ""
                    val middleName = cursor.getStringValue(CommonDataKinds.StructuredName.MIDDLE_NAME) ?: ""
                    val surname = cursor.getStringValue(CommonDataKinds.StructuredName.FAMILY_NAME) ?: ""
                    val suffix = cursor.getStringValue(CommonDataKinds.StructuredName.SUFFIX) ?: ""
                    val nickname = ""
                    val photoUri = cursor.getStringValue(CommonDataKinds.StructuredName.PHOTO_URI) ?: ""
                    val numbers = ArrayList<PhoneNumber>()          // proper value is obtained below
                    val emails = ArrayList<Email>()
                    val addresses = ArrayList<Address>()
                    val events = ArrayList<Event>()
                    val accountName = cursor.getStringValue(ContactsContract.RawContacts.ACCOUNT_NAME) ?: ""
                    val starred = cursor.getIntValue(CommonDataKinds.StructuredName.STARRED)
                    val contactId = cursor.getIntValue(ContactsContract.Data.CONTACT_ID)
                    val thumbnailUri = cursor.getStringValue(CommonDataKinds.StructuredName.PHOTO_THUMBNAIL_URI) ?: ""
                    val notes = ""
                    val groups = ArrayList<Group>()
                    val organization = Organization("", "")
                    val websites = ArrayList<String>()
                    val cleanNumbers = ArrayList<PhoneNumber>()
                    val ims = ArrayList<IM>()
                    val contact = Contact(id, prefix, firstName, middleName, surname, suffix, nickname, photoUri, numbers, emails, addresses,
                            events, accountName, starred, contactId, thumbnailUri, null, notes, groups, organization, websites, cleanNumbers, ims)

                    contacts.put(id, contact)
                } while (cursor.moveToNext())
            }
        } catch (e: Exception) {
            activity.showErrorToast(e)
        } finally {
            cursor?.close()
        }

        val filterDuplicates = activity.config.filterDuplicates
        val phoneNumbers = getPhoneNumbers(null)
        var size = phoneNumbers.size()
        for (i in 0 until size) {
            val key = phoneNumbers.keyAt(i)
            if (contacts[key] != null) {
                val numbers = phoneNumbers.valueAt(i)
                contacts[key].phoneNumbers = numbers

                if (filterDuplicates) {
                    // remove all spaces, dashes etc from numbers for easier comparing, used only at list views
                    numbers.forEach {
                        numbers.mapTo(contacts[key].cleanPhoneNumbers) { PhoneNumber(it.value.applyRegexFiltering(), 0, "") }
                    }
                }
            }
        }

        val nicknames = getNicknames()
        size = nicknames.size()
        for (i in 0 until size) {
            val key = nicknames.keyAt(i)
            contacts[key]?.nickname = nicknames.valueAt(i)
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

        val IMs = getIMs()
        size = IMs.size()
        for (i in 0 until size) {
            val key = IMs.keyAt(i)
            contacts[key]?.IMs = IMs.valueAt(i)
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

        val organizations = getOrganizations()
        size = organizations.size()
        for (i in 0 until size) {
            val key = organizations.keyAt(i)
            contacts[key]?.organization = organizations.valueAt(i)
        }

        val websites = getWebsites()
        size = websites.size()
        for (i in 0 until size) {
            val key = websites.keyAt(i)
            contacts[key]?.websites = websites.valueAt(i)
        }
    }

    private fun getPhoneNumbers(contactId: Int? = null): SparseArray<ArrayList<PhoneNumber>> {
        val phoneNumbers = SparseArray<ArrayList<PhoneNumber>>()
        val uri = CommonDataKinds.Phone.CONTENT_URI
        val projection = arrayOf(
                ContactsContract.Data.RAW_CONTACT_ID,
                CommonDataKinds.Phone.NUMBER,
                CommonDataKinds.Phone.TYPE,
                CommonDataKinds.Phone.LABEL
        )

        val selection = if (contactId == null) getSourcesSelection() else "${ContactsContract.Data.RAW_CONTACT_ID} = ?"
        val selectionArgs = if (contactId == null) getSourcesSelectionArgs() else arrayOf(contactId.toString())

        var cursor: Cursor? = null
        try {
            cursor = activity.contentResolver.query(uri, projection, selection, selectionArgs, null)
            if (cursor?.moveToFirst() == true) {
                do {
                    val id = cursor.getIntValue(ContactsContract.Data.RAW_CONTACT_ID)
                    val number = cursor.getStringValue(CommonDataKinds.Phone.NUMBER) ?: continue
                    val type = cursor.getIntValue(CommonDataKinds.Phone.TYPE)
                    val label = cursor.getStringValue(CommonDataKinds.Phone.LABEL) ?: ""

                    if (phoneNumbers[id] == null) {
                        phoneNumbers.put(id, ArrayList())
                    }

                    val phoneNumber = PhoneNumber(number, type, label)
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

    private fun getNicknames(contactId: Int? = null): SparseArray<String> {
        val nicknames = SparseArray<String>()
        val uri = ContactsContract.Data.CONTENT_URI
        val projection = arrayOf(
                ContactsContract.Data.RAW_CONTACT_ID,
                Nickname.NAME
        )

        val selection = getSourcesSelection(true, contactId != null)
        val selectionArgs = getSourcesSelectionArgs(Nickname.CONTENT_ITEM_TYPE, contactId)

        var cursor: Cursor? = null
        try {
            cursor = activity.contentResolver.query(uri, projection, selection, selectionArgs, null)
            if (cursor?.moveToFirst() == true) {
                do {
                    val id = cursor.getIntValue(ContactsContract.Data.RAW_CONTACT_ID)
                    val nickname = cursor.getStringValue(Nickname.NAME) ?: continue
                    nicknames.put(id, nickname)
                } while (cursor.moveToNext())
            }
        } catch (e: Exception) {
            activity.showErrorToast(e)
        } finally {
            cursor?.close()
        }

        return nicknames
    }

    private fun getEmails(contactId: Int? = null): SparseArray<ArrayList<Email>> {
        val emails = SparseArray<ArrayList<Email>>()
        val uri = CommonDataKinds.Email.CONTENT_URI
        val projection = arrayOf(
                ContactsContract.Data.RAW_CONTACT_ID,
                CommonDataKinds.Email.DATA,
                CommonDataKinds.Email.TYPE,
                CommonDataKinds.Email.LABEL
        )

        val selection = if (contactId == null) getSourcesSelection() else "${ContactsContract.Data.RAW_CONTACT_ID} = ?"
        val selectionArgs = if (contactId == null) getSourcesSelectionArgs() else arrayOf(contactId.toString())

        var cursor: Cursor? = null
        try {
            cursor = activity.contentResolver.query(uri, projection, selection, selectionArgs, null)
            if (cursor?.moveToFirst() == true) {
                do {
                    val id = cursor.getIntValue(ContactsContract.Data.RAW_CONTACT_ID)
                    val email = cursor.getStringValue(CommonDataKinds.Email.DATA) ?: continue
                    val type = cursor.getIntValue(CommonDataKinds.Email.TYPE)
                    val label = cursor.getStringValue(CommonDataKinds.Email.LABEL) ?: ""

                    if (emails[id] == null) {
                        emails.put(id, ArrayList())
                    }

                    emails[id]!!.add(Email(email, type, label))
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
                CommonDataKinds.StructuredPostal.TYPE,
                CommonDataKinds.StructuredPostal.LABEL
        )

        val selection = if (contactId == null) getSourcesSelection() else "${ContactsContract.Data.RAW_CONTACT_ID} = ?"
        val selectionArgs = if (contactId == null) getSourcesSelectionArgs() else arrayOf(contactId.toString())

        var cursor: Cursor? = null
        try {
            cursor = activity.contentResolver.query(uri, projection, selection, selectionArgs, null)
            if (cursor?.moveToFirst() == true) {
                do {
                    val id = cursor.getIntValue(ContactsContract.Data.RAW_CONTACT_ID)
                    val address = cursor.getStringValue(CommonDataKinds.StructuredPostal.FORMATTED_ADDRESS) ?: continue
                    val type = cursor.getIntValue(CommonDataKinds.StructuredPostal.TYPE)
                    val label = cursor.getStringValue(CommonDataKinds.StructuredPostal.LABEL) ?: ""

                    if (addresses[id] == null) {
                        addresses.put(id, ArrayList())
                    }

                    addresses[id]!!.add(Address(address, type, label))
                } while (cursor.moveToNext())
            }
        } catch (e: Exception) {
            activity.showErrorToast(e)
        } finally {
            cursor?.close()
        }

        return addresses
    }

    private fun getIMs(contactId: Int? = null): SparseArray<ArrayList<IM>> {
        val IMs = SparseArray<ArrayList<IM>>()
        val uri = ContactsContract.Data.CONTENT_URI
        val projection = arrayOf(
                ContactsContract.Data.RAW_CONTACT_ID,
                CommonDataKinds.Im.DATA,
                CommonDataKinds.Im.PROTOCOL,
                CommonDataKinds.Im.CUSTOM_PROTOCOL
        )

        val selection = getSourcesSelection(true, contactId != null)
        val selectionArgs = getSourcesSelectionArgs(CommonDataKinds.Im.CONTENT_ITEM_TYPE, contactId)

        var cursor: Cursor? = null
        try {
            cursor = activity.contentResolver.query(uri, projection, selection, selectionArgs, null)
            if (cursor?.moveToFirst() == true) {
                do {
                    val id = cursor.getIntValue(ContactsContract.Data.RAW_CONTACT_ID)
                    val IM = cursor.getStringValue(CommonDataKinds.Im.DATA) ?: continue
                    val type = cursor.getIntValue(CommonDataKinds.Im.PROTOCOL)
                    val label = cursor.getStringValue(CommonDataKinds.Im.CUSTOM_PROTOCOL) ?: ""

                    if (IMs[id] == null) {
                        IMs.put(id, ArrayList())
                    }

                    IMs[id]!!.add(IM(IM, type, label))
                } while (cursor.moveToNext())
            }
        } catch (e: Exception) {
            activity.showErrorToast(e)
        } finally {
            cursor?.close()
        }

        return IMs
    }

    private fun getEvents(contactId: Int? = null): SparseArray<ArrayList<Event>> {
        val events = SparseArray<ArrayList<Event>>()
        val uri = ContactsContract.Data.CONTENT_URI
        val projection = arrayOf(
                ContactsContract.Data.RAW_CONTACT_ID,
                CommonDataKinds.Event.START_DATE,
                CommonDataKinds.Event.TYPE
        )

        val selection = getSourcesSelection(true, contactId != null)
        val selectionArgs = getSourcesSelectionArgs(CommonDataKinds.Event.CONTENT_ITEM_TYPE, contactId)

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

        val selection = getSourcesSelection(true, contactId != null)
        val selectionArgs = getSourcesSelectionArgs(Note.CONTENT_ITEM_TYPE, contactId)

        var cursor: Cursor? = null
        try {
            cursor = activity.contentResolver.query(uri, projection, selection, selectionArgs, null)
            if (cursor?.moveToFirst() == true) {
                do {
                    val id = cursor.getIntValue(ContactsContract.Data.RAW_CONTACT_ID)
                    val note = cursor.getStringValue(Note.NOTE) ?: continue
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

    private fun getOrganizations(contactId: Int? = null): SparseArray<Organization> {
        val organizations = SparseArray<Organization>()
        val uri = ContactsContract.Data.CONTENT_URI
        val projection = arrayOf(
                ContactsContract.Data.RAW_CONTACT_ID,
                CommonDataKinds.Organization.COMPANY,
                CommonDataKinds.Organization.TITLE
        )

        val selection = getSourcesSelection(true, contactId != null)
        val selectionArgs = getSourcesSelectionArgs(CommonDataKinds.Organization.CONTENT_ITEM_TYPE, contactId)

        var cursor: Cursor? = null
        try {
            cursor = activity.contentResolver.query(uri, projection, selection, selectionArgs, null)
            if (cursor?.moveToFirst() == true) {
                do {
                    val id = cursor.getIntValue(ContactsContract.Data.RAW_CONTACT_ID)
                    val company = cursor.getStringValue(CommonDataKinds.Organization.COMPANY) ?: ""
                    val title = cursor.getStringValue(CommonDataKinds.Organization.TITLE) ?: ""
                    if (company.isEmpty() && title.isEmpty()) {
                        continue
                    }

                    val organization = Organization(company, title)
                    organizations.put(id, organization)
                } while (cursor.moveToNext())
            }
        } catch (e: Exception) {
            activity.showErrorToast(e)
        } finally {
            cursor?.close()
        }

        return organizations
    }

    private fun getWebsites(contactId: Int? = null): SparseArray<ArrayList<String>> {
        val websites = SparseArray<ArrayList<String>>()
        val uri = ContactsContract.Data.CONTENT_URI
        val projection = arrayOf(
                ContactsContract.Data.RAW_CONTACT_ID,
                CommonDataKinds.Website.URL
        )

        val selection = getSourcesSelection(true, contactId != null)
        val selectionArgs = getSourcesSelectionArgs(CommonDataKinds.Website.CONTENT_ITEM_TYPE, contactId)

        var cursor: Cursor? = null
        try {
            cursor = activity.contentResolver.query(uri, projection, selection, selectionArgs, null)
            if (cursor?.moveToFirst() == true) {
                do {
                    val id = cursor.getIntValue(ContactsContract.Data.RAW_CONTACT_ID)
                    val url = cursor.getStringValue(CommonDataKinds.Website.URL) ?: continue

                    if (websites[id] == null) {
                        websites.put(id, ArrayList())
                    }

                    websites[id]!!.add(url)
                } while (cursor.moveToNext())
            }
        } catch (e: Exception) {
            activity.showErrorToast(e)
        } finally {
            cursor?.close()
        }

        return websites
    }

    private fun getContactGroups(storedGroups: ArrayList<Group>, contactId: Int? = null): SparseArray<ArrayList<Group>> {
        val groups = SparseArray<ArrayList<Group>>()
        if (!activity.hasContactPermissions()) {
            return groups
        }

        val uri = ContactsContract.Data.CONTENT_URI
        val projection = arrayOf(
                ContactsContract.Data.CONTACT_ID,
                ContactsContract.Data.DATA1
        )

        val selection = getSourcesSelection(true, contactId != null, false)
        val selectionArgs = getSourcesSelectionArgs(CommonDataKinds.GroupMembership.CONTENT_ITEM_TYPE, contactId)

        var cursor: Cursor? = null
        try {
            cursor = activity.contentResolver.query(uri, projection, selection, selectionArgs, null)
            if (cursor?.moveToFirst() == true) {
                do {
                    val id = cursor.getIntValue(ContactsContract.Data.CONTACT_ID)
                    val newRowId = cursor.getLongValue(ContactsContract.Data.DATA1)

                    val groupTitle = storedGroups.firstOrNull { it.id == newRowId }?.title ?: continue
                    val group = Group(newRowId, groupTitle)
                    if (groups[id] == null) {
                        groups.put(id, ArrayList())
                    }
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

    private fun getQuestionMarks() = "?,".times(displayContactSources.filter { it.isNotEmpty() }.size).trimEnd(',')

    private fun getSourcesSelection(addMimeType: Boolean = false, addContactId: Boolean = false, useRawContactId: Boolean = true): String {
        val strings = ArrayList<String>()
        if (addMimeType) {
            strings.add("${ContactsContract.Data.MIMETYPE} = ?")
        }

        if (addContactId) {
            strings.add("${if (useRawContactId) ContactsContract.Data.RAW_CONTACT_ID else ContactsContract.Data.CONTACT_ID} = ?")
        } else {
            // sometimes local device storage has null account_name, handle it properly
            val accountnameString = StringBuilder()
            if (displayContactSources.contains("")) {
                accountnameString.append("(")
            }
            accountnameString.append("${ContactsContract.RawContacts.ACCOUNT_NAME} IN (${getQuestionMarks()})")
            if (displayContactSources.contains("")) {
                accountnameString.append(" OR ${ContactsContract.RawContacts.ACCOUNT_NAME} IS NULL)")
            }
            strings.add(accountnameString.toString())
        }

        return TextUtils.join(" AND ", strings)
    }

    private fun getSourcesSelectionArgs(mimetype: String? = null, contactId: Int? = null): Array<String> {
        val args = ArrayList<String>()

        if (mimetype != null) {
            args.add(mimetype)
        }

        if (contactId != null) {
            args.add(contactId.toString())
        } else {
            args.addAll(displayContactSources.filter { it.isNotEmpty() })
        }

        return args.toTypedArray()
    }

    fun getStoredGroups(): ArrayList<Group> {
        val groups = getDeviceStoredGroups()
        groups.addAll(activity.dbHelper.getGroups())
        return groups
    }

    fun getDeviceStoredGroups(): ArrayList<Group> {
        val groups = ArrayList<Group>()
        if (!activity.hasContactPermissions()) {
            return groups
        }

        val uri = ContactsContract.Groups.CONTENT_URI
        val projection = arrayOf(
                ContactsContract.Groups._ID,
                ContactsContract.Groups.TITLE,
                ContactsContract.Groups.SYSTEM_ID
        )

        val selection = "${ContactsContract.Groups.AUTO_ADD} = ? AND ${ContactsContract.Groups.FAVORITES} = ?"
        val selectionArgs = arrayOf("0", "0")

        var cursor: Cursor? = null
        try {
            cursor = activity.contentResolver.query(uri, projection, selection, selectionArgs, null)
            if (cursor?.moveToFirst() == true) {
                do {
                    val id = cursor.getLongValue(ContactsContract.Groups._ID)
                    val title = cursor.getStringValue(ContactsContract.Groups.TITLE) ?: continue

                    val systemId = cursor.getStringValue(ContactsContract.Groups.SYSTEM_ID)
                    if (groups.map { it.title }.contains(title) && systemId != null) {
                        continue
                    }

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

    fun createNewGroup(title: String, accountName: String, accountType: String): Group? {
        if (accountType == SMT_PRIVATE) {
            return activity.dbHelper.insertGroup(Group(0, title))
        }

        val operations = ArrayList<ContentProviderOperation>()
        ContentProviderOperation.newInsert(ContactsContract.Groups.CONTENT_URI).apply {
            withValue(ContactsContract.Groups.TITLE, title)
            withValue(ContactsContract.Groups.GROUP_VISIBLE, 1)
            withValue(ContactsContract.Groups.ACCOUNT_NAME, accountName)
            withValue(ContactsContract.Groups.ACCOUNT_TYPE, accountType)
            operations.add(build())
        }

        try {
            val results = activity.contentResolver.applyBatch(ContactsContract.AUTHORITY, operations)
            val rawId = ContentUris.parseId(results[0].uri)
            return Group(rawId, title)
        } catch (e: Exception) {
            activity.showErrorToast(e)
        }
        return null
    }

    fun renameGroup(group: Group) {
        val operations = ArrayList<ContentProviderOperation>()
        ContentProviderOperation.newUpdate(ContactsContract.Groups.CONTENT_URI).apply {
            val selection = "${ContactsContract.Groups._ID} = ?"
            val selectionArgs = arrayOf(group.id.toString())
            withSelection(selection, selectionArgs)
            withValue(ContactsContract.Groups.TITLE, group.title)
            operations.add(build())
        }

        try {
            activity.contentResolver.applyBatch(ContactsContract.AUTHORITY, operations)
        } catch (e: Exception) {
            activity.showErrorToast(e)
        }
    }

    fun deleteGroup(id: Long) {
        val operations = ArrayList<ContentProviderOperation>()
        val uri = ContentUris.withAppendedId(ContactsContract.Groups.CONTENT_URI, id).buildUpon()
                .appendQueryParameter(ContactsContract.CALLER_IS_SYNCADAPTER, "true")
                .build()

        operations.add(ContentProviderOperation.newDelete(uri).build())

        try {
            activity.contentResolver.applyBatch(ContactsContract.AUTHORITY, operations)
        } catch (e: Exception) {
            activity.showErrorToast(e)
        }
    }

    fun getContactWithId(id: Int, isLocalPrivate: Boolean): Contact? {
        if (id == 0) {
            return null
        } else if (isLocalPrivate) {
            return activity.dbHelper.getContactWithId(activity, id)
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
                val prefix = cursor.getStringValue(CommonDataKinds.StructuredName.PREFIX) ?: ""
                val firstName = cursor.getStringValue(CommonDataKinds.StructuredName.GIVEN_NAME) ?: ""
                val middleName = cursor.getStringValue(CommonDataKinds.StructuredName.MIDDLE_NAME) ?: ""
                val surname = cursor.getStringValue(CommonDataKinds.StructuredName.FAMILY_NAME) ?: ""
                val suffix = cursor.getStringValue(CommonDataKinds.StructuredName.SUFFIX) ?: ""
                val nickname = getNicknames(id)[id] ?: ""
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
                val organization = getOrganizations(id)[id] ?: Organization("", "")
                val websites = getWebsites(id)[id] ?: ArrayList()
                val cleanNumbers = ArrayList<PhoneNumber>()
                val ims = getIMs(id)[id] ?: ArrayList()
                return Contact(id, prefix, firstName, middleName, surname, suffix, nickname, photoUri, number, emails, addresses, events,
                        accountName, starred, contactId, thumbnailUri, null, notes, groups, organization, websites, cleanNumbers, ims)
            }
        } finally {
            cursor?.close()
        }

        return null
    }

    fun getContactSources(callback: (ArrayList<ContactSource>) -> Unit) {
        Thread {
            callback(getContactSourcesSync())
        }.start()
    }

    private fun getContactSourcesSync(): ArrayList<ContactSource> {
        val sources = getDeviceContactSources()
        sources.add(ContactSource(activity.getString(R.string.phone_storage_hidden), SMT_PRIVATE))
        return ArrayList(sources)
    }

    fun getDeviceContactSources(): LinkedHashSet<ContactSource> {
        val sources = LinkedHashSet<ContactSource>()
        if (!activity.hasContactPermissions()) {
            return sources
        }

        val accounts = AccountManager.get(activity).accounts
        accounts.forEach {
            if (ContentResolver.getIsSyncable(it, ContactsContract.AUTHORITY) == 1) {
                val contactSource = ContactSource(it.name, it.type)
                if (it.type == TELEGRAM_PACKAGE) {
                    contactSource.name += " (${activity.getString(R.string.telegram)})"
                }
                sources.add(contactSource)
            }
        }

        val contentResolverAccounts = getContentResolverAccounts().filter {
            it.name.isNotEmpty() && it.type.isNotEmpty() && !accounts.contains(Account(it.name, it.type))
        }
        sources.addAll(contentResolverAccounts)

        if (sources.isEmpty() && activity.config.localAccountName.isEmpty() && activity.config.localAccountType.isEmpty()) {
            sources.add(ContactSource("", ""))
        }

        return sources
    }

    private fun getContactSourceType(accountName: String) = getDeviceContactSources().firstOrNull { it.name == accountName }?.type ?: ""

    private fun getContactProjection() = arrayOf(
            ContactsContract.Data.CONTACT_ID,
            ContactsContract.Data.RAW_CONTACT_ID,
            CommonDataKinds.StructuredName.PREFIX,
            CommonDataKinds.StructuredName.GIVEN_NAME,
            CommonDataKinds.StructuredName.MIDDLE_NAME,
            CommonDataKinds.StructuredName.FAMILY_NAME,
            CommonDataKinds.StructuredName.SUFFIX,
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
        activity.toast(R.string.updating)
        if (contact.source == SMT_PRIVATE) {
            return activity.dbHelper.updateContact(contact)
        }

        try {
            val operations = ArrayList<ContentProviderOperation>()
            ContentProviderOperation.newUpdate(ContactsContract.Data.CONTENT_URI).apply {
                val selection = "${ContactsContract.Data.RAW_CONTACT_ID} = ? AND ${ContactsContract.Data.MIMETYPE} = ?"
                val selectionArgs = arrayOf(contact.id.toString(), CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                withSelection(selection, selectionArgs)
                withValue(CommonDataKinds.StructuredName.PREFIX, contact.prefix)
                withValue(CommonDataKinds.StructuredName.GIVEN_NAME, contact.firstName)
                withValue(CommonDataKinds.StructuredName.MIDDLE_NAME, contact.middleName)
                withValue(CommonDataKinds.StructuredName.FAMILY_NAME, contact.surname)
                withValue(CommonDataKinds.StructuredName.SUFFIX, contact.suffix)
                operations.add(build())
            }

            // delete nickname
            ContentProviderOperation.newDelete(ContactsContract.Data.CONTENT_URI).apply {
                val selection = "${ContactsContract.Data.RAW_CONTACT_ID} = ? AND ${ContactsContract.Data.MIMETYPE} = ? "
                val selectionArgs = arrayOf(contact.id.toString(), Nickname.CONTENT_ITEM_TYPE)
                withSelection(selection, selectionArgs)
                operations.add(build())
            }

            // add nickname
            ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI).apply {
                withValue(ContactsContract.Data.RAW_CONTACT_ID, contact.id)
                withValue(ContactsContract.Data.MIMETYPE, Nickname.CONTENT_ITEM_TYPE)
                withValue(Nickname.NAME, contact.nickname)
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
                    withValue(CommonDataKinds.Phone.LABEL, it.label)
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
                    withValue(CommonDataKinds.Email.LABEL, it.label)
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
                    withValue(CommonDataKinds.StructuredPostal.LABEL, it.label)
                    operations.add(build())
                }
            }

            // delete IMs
            ContentProviderOperation.newDelete(ContactsContract.Data.CONTENT_URI).apply {
                val selection = "${ContactsContract.Data.RAW_CONTACT_ID} = ? AND ${ContactsContract.Data.MIMETYPE} = ? "
                val selectionArgs = arrayOf(contact.id.toString(), CommonDataKinds.Im.CONTENT_ITEM_TYPE)
                withSelection(selection, selectionArgs)
                operations.add(build())
            }

            // add IMs
            contact.IMs.forEach {
                ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI).apply {
                    withValue(ContactsContract.Data.RAW_CONTACT_ID, contact.id)
                    withValue(ContactsContract.Data.MIMETYPE, CommonDataKinds.Im.CONTENT_ITEM_TYPE)
                    withValue(CommonDataKinds.Im.DATA, it.value)
                    withValue(CommonDataKinds.Im.PROTOCOL, it.type)
                    withValue(CommonDataKinds.Im.CUSTOM_PROTOCOL, it.label)
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

            // delete notes
            ContentProviderOperation.newDelete(ContactsContract.Data.CONTENT_URI).apply {
                val selection = "${ContactsContract.Data.RAW_CONTACT_ID} = ? AND ${ContactsContract.Data.MIMETYPE} = ? "
                val selectionArgs = arrayOf(contact.id.toString(), Note.CONTENT_ITEM_TYPE)
                withSelection(selection, selectionArgs)
                operations.add(build())
            }

            // add notes
            ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI).apply {
                withValue(ContactsContract.Data.RAW_CONTACT_ID, contact.id)
                withValue(ContactsContract.Data.MIMETYPE, Note.CONTENT_ITEM_TYPE)
                withValue(Note.NOTE, contact.notes)
                operations.add(build())
            }

            // delete organization
            ContentProviderOperation.newDelete(ContactsContract.Data.CONTENT_URI).apply {
                val selection = "${ContactsContract.Data.RAW_CONTACT_ID} = ? AND ${ContactsContract.Data.MIMETYPE} = ? "
                val selectionArgs = arrayOf(contact.id.toString(), CommonDataKinds.Organization.CONTENT_ITEM_TYPE)
                withSelection(selection, selectionArgs)
                operations.add(build())
            }

            // add organization
            ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI).apply {
                withValue(ContactsContract.Data.RAW_CONTACT_ID, contact.id)
                withValue(ContactsContract.Data.MIMETYPE, CommonDataKinds.Organization.CONTENT_ITEM_TYPE)
                withValue(CommonDataKinds.Organization.COMPANY, contact.organization.company)
                withValue(CommonDataKinds.Organization.TYPE, DEFAULT_ORGANIZATION_TYPE)
                withValue(CommonDataKinds.Organization.TITLE, contact.organization.jobPosition)
                withValue(CommonDataKinds.Organization.TYPE, DEFAULT_ORGANIZATION_TYPE)
                operations.add(build())
            }

            // delete websites
            ContentProviderOperation.newDelete(ContactsContract.Data.CONTENT_URI).apply {
                val selection = "${ContactsContract.Data.RAW_CONTACT_ID} = ? AND ${ContactsContract.Data.MIMETYPE} = ? "
                val selectionArgs = arrayOf(contact.id.toString(), CommonDataKinds.Website.CONTENT_ITEM_TYPE)
                withSelection(selection, selectionArgs)
                operations.add(build())
            }

            // add websites
            contact.websites.forEach {
                ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI).apply {
                    withValue(ContactsContract.Data.RAW_CONTACT_ID, contact.id)
                    withValue(ContactsContract.Data.MIMETYPE, CommonDataKinds.Website.CONTENT_ITEM_TYPE)
                    withValue(CommonDataKinds.Website.URL, it)
                    withValue(CommonDataKinds.Website.TYPE, DEFAULT_WEBSITE_TYPE)
                    operations.add(build())
                }
            }

            // delete groups
            val relevantGroupIDs = getStoredGroups().map { it.id }
            if (relevantGroupIDs.isNotEmpty()) {
                val IDsString = TextUtils.join(",", relevantGroupIDs)
                ContentProviderOperation.newDelete(ContactsContract.Data.CONTENT_URI).apply {
                    val selection = "${ContactsContract.Data.CONTACT_ID} = ? AND ${ContactsContract.Data.MIMETYPE} = ? AND ${ContactsContract.Data.DATA1} IN ($IDsString)"
                    val selectionArgs = arrayOf(contact.contactId.toString(), CommonDataKinds.GroupMembership.CONTENT_ITEM_TYPE)
                    withSelection(selection, selectionArgs)
                    operations.add(build())
                }
            }

            // add groups
            contact.groups.forEach {
                ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI).apply {
                    withValue(ContactsContract.Data.RAW_CONTACT_ID, contact.id)
                    withValue(ContactsContract.Data.MIMETYPE, CommonDataKinds.GroupMembership.CONTENT_ITEM_TYPE)
                    withValue(CommonDataKinds.GroupMembership.GROUP_ROW_ID, it.id)
                    operations.add(build())
                }
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
            return true
        } catch (e: Exception) {
            activity.showErrorToast(e)
            return false
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

    fun addContactsToGroup(contacts: ArrayList<Contact>, groupId: Long) {
        val operations = ArrayList<ContentProviderOperation>()
        contacts.forEach {
            ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI).apply {
                withValue(ContactsContract.Data.RAW_CONTACT_ID, it.id)
                withValue(ContactsContract.Data.MIMETYPE, CommonDataKinds.GroupMembership.CONTENT_ITEM_TYPE)
                withValue(CommonDataKinds.GroupMembership.GROUP_ROW_ID, groupId)
                operations.add(build())
            }

            if (operations.size % BATCH_SIZE == 0) {
                activity.contentResolver.applyBatch(ContactsContract.AUTHORITY, operations)
                operations.clear()
            }
        }

        try {
            activity.contentResolver.applyBatch(ContactsContract.AUTHORITY, operations)
        } catch (e: Exception) {
            activity.showErrorToast(e)
        }
    }

    fun removeContactsFromGroup(contacts: ArrayList<Contact>, groupId: Long) {
        val operations = ArrayList<ContentProviderOperation>()
        contacts.forEach {
            ContentProviderOperation.newDelete(ContactsContract.Data.CONTENT_URI).apply {
                val selection = "${ContactsContract.Data.CONTACT_ID} = ? AND ${ContactsContract.Data.MIMETYPE} = ? AND ${ContactsContract.Data.DATA1} = ?"
                val selectionArgs = arrayOf(it.contactId.toString(), CommonDataKinds.GroupMembership.CONTENT_ITEM_TYPE, groupId.toString())
                withSelection(selection, selectionArgs)
                operations.add(build())
            }

            if (operations.size % BATCH_SIZE == 0) {
                activity.contentResolver.applyBatch(ContactsContract.AUTHORITY, operations)
                operations.clear()
            }
        }
        activity.contentResolver.applyBatch(ContactsContract.AUTHORITY, operations)
    }

    fun insertContact(contact: Contact): Boolean {
        if (contact.source == SMT_PRIVATE) {
            return insertLocalContact(contact)
        }

        try {
            val operations = ArrayList<ContentProviderOperation>()
            ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI).apply {
                withValue(ContactsContract.RawContacts.ACCOUNT_NAME, contact.source)
                withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, getContactSourceType(contact.source))
                withValue(ContactsContract.RawContacts.DIRTY, false)
                operations.add(build())
            }

            // names
            ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI).apply {
                withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                withValue(ContactsContract.Data.MIMETYPE, CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                withValue(CommonDataKinds.StructuredName.PREFIX, contact.prefix)
                withValue(CommonDataKinds.StructuredName.GIVEN_NAME, contact.firstName)
                withValue(CommonDataKinds.StructuredName.MIDDLE_NAME, contact.middleName)
                withValue(CommonDataKinds.StructuredName.FAMILY_NAME, contact.surname)
                withValue(CommonDataKinds.StructuredName.SUFFIX, contact.suffix)
                operations.add(build())
            }

            // nickname
            ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI).apply {
                withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                withValue(ContactsContract.Data.MIMETYPE, Nickname.CONTENT_ITEM_TYPE)
                withValue(Nickname.NAME, contact.nickname)
                operations.add(build())
            }

            // phone numbers
            contact.phoneNumbers.forEach {
                ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI).apply {
                    withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                    withValue(ContactsContract.Data.MIMETYPE, CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                    withValue(CommonDataKinds.Phone.NUMBER, it.value)
                    withValue(CommonDataKinds.Phone.TYPE, it.type)
                    withValue(CommonDataKinds.Phone.LABEL, it.label)
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
                    withValue(CommonDataKinds.Email.LABEL, it.label)
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
                    withValue(CommonDataKinds.StructuredPostal.LABEL, it.label)
                    operations.add(build())
                }
            }

            // IMs
            contact.IMs.forEach {
                ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI).apply {
                    withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                    withValue(ContactsContract.Data.MIMETYPE, CommonDataKinds.Im.CONTENT_ITEM_TYPE)
                    withValue(CommonDataKinds.Im.DATA, it.value)
                    withValue(CommonDataKinds.Im.PROTOCOL, it.type)
                    withValue(CommonDataKinds.Im.CUSTOM_PROTOCOL, it.label)
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

            // organization
            if (!contact.organization.isEmpty()) {
                ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI).apply {
                    withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                    withValue(ContactsContract.Data.MIMETYPE, CommonDataKinds.Organization.CONTENT_ITEM_TYPE)
                    withValue(CommonDataKinds.Organization.COMPANY, contact.organization.company)
                    withValue(CommonDataKinds.Organization.TYPE, DEFAULT_ORGANIZATION_TYPE)
                    withValue(CommonDataKinds.Organization.TITLE, contact.organization.jobPosition)
                    withValue(CommonDataKinds.Organization.TYPE, DEFAULT_ORGANIZATION_TYPE)
                    operations.add(build())
                }
            }

            // websites
            contact.websites.forEach {
                ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI).apply {
                    withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                    withValue(ContactsContract.Data.MIMETYPE, CommonDataKinds.Website.CONTENT_ITEM_TYPE)
                    withValue(CommonDataKinds.Website.URL, it)
                    withValue(CommonDataKinds.Website.TYPE, DEFAULT_WEBSITE_TYPE)
                    operations.add(build())
                }
            }

            // groups
            contact.groups.forEach {
                ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI).apply {
                    withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                    withValue(ContactsContract.Data.MIMETYPE, CommonDataKinds.GroupMembership.CONTENT_ITEM_TYPE)
                    withValue(CommonDataKinds.GroupMembership.GROUP_ROW_ID, it.id)
                    operations.add(build())
                }
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

            return true
        } catch (e: Exception) {
            activity.showErrorToast(e)
            return false
        }
    }

    private fun insertLocalContact(contact: Contact) = activity.dbHelper.insertContact(contact)

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

    fun getContactMimeTypeId(contactId: String, mimeType: String): String {
        val uri = ContactsContract.Data.CONTENT_URI
        val projection = arrayOf(ContactsContract.Data._ID, ContactsContract.Data.RAW_CONTACT_ID, ContactsContract.Data.MIMETYPE)
        val selection = "${ContactsContract.Data.MIMETYPE} = ? AND ${ContactsContract.Data.RAW_CONTACT_ID} = ?"
        val selectionArgs = arrayOf(mimeType, contactId)

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
        if (activity.hasContactPermissions()) {
            toggleFavorites(contacts, true)
        }
    }

    fun removeFavorites(contacts: ArrayList<Contact>) {
        toggleLocalFavorites(contacts, false)
        if (activity.hasContactPermissions()) {
            toggleFavorites(contacts, false)
        }
    }

    private fun toggleFavorites(contacts: ArrayList<Contact>, addToFavorites: Boolean) {
        try {
            val operations = ArrayList<ContentProviderOperation>()
            contacts.filter { it.source != SMT_PRIVATE }.map { it.contactId.toString() }.forEach {
                val uri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_URI, it)
                ContentProviderOperation.newUpdate(uri).apply {
                    withValue(ContactsContract.Contacts.STARRED, if (addToFavorites) 1 else 0)
                    operations.add(build())
                }

                if (operations.size % BATCH_SIZE == 0) {
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
        Thread {
            val localContacts = contacts.filter { it.source == SMT_PRIVATE }.map { it.id.toString() }.toTypedArray()
            activity.dbHelper.deleteContacts(localContacts)

            try {
                val operations = ArrayList<ContentProviderOperation>()
                val selection = "${ContactsContract.RawContacts._ID} = ?"
                contacts.filter { it.source != SMT_PRIVATE }.forEach {
                    ContentProviderOperation.newDelete(ContactsContract.RawContacts.CONTENT_URI).apply {
                        val selectionArgs = arrayOf(it.id.toString())
                        withSelection(selection, selectionArgs)
                        operations.add(build())
                    }

                    if (operations.size % BATCH_SIZE == 0) {
                        activity.contentResolver.applyBatch(ContactsContract.AUTHORITY, operations)
                        operations.clear()
                    }
                }

                if (activity.hasPermission(PERMISSION_WRITE_CONTACTS)) {
                    activity.contentResolver.applyBatch(ContactsContract.AUTHORITY, operations)
                }
            } catch (e: Exception) {
                activity.showErrorToast(e)
            }
        }.start()
    }

    @SuppressLint("MissingPermission")
    fun getRecents(callback: (ArrayList<RecentCall>) -> Unit) {
        Thread {
            val calls = ArrayList<RecentCall>()
            if (!activity.hasPermission(PERMISSION_WRITE_CALL_LOG) || !activity.hasPermission(PERMISSION_READ_CALL_LOG)) {
                callback(calls)
                return@Thread
            }

            val uri = CallLog.Calls.CONTENT_URI
            val projection = arrayOf(
                    CallLog.Calls._ID,
                    CallLog.Calls.NUMBER,
                    CallLog.Calls.DATE,
                    CallLog.Calls.CACHED_NAME
            )

            val sorting = "${CallLog.Calls._ID} DESC LIMIT 100"
            val currentDate = Date(System.currentTimeMillis())
            val currentYear = SimpleDateFormat("yyyy", Locale.getDefault()).format(currentDate)
            val todayDate = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(currentDate)
            val yesterdayDate = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(System.currentTimeMillis() - DAY_SECONDS * 1000))
            val yesterday = activity.getString(R.string.yesterday)
            val timeFormat = if (activity.config.use24HourFormat) "HH:mm" else "h:mm a"
            var prevNumber = ""

            var cursor: Cursor? = null
            try {
                cursor = activity.contentResolver.query(uri, projection, null, null, sorting)
                if (cursor?.moveToFirst() == true) {
                    do {
                        val id = cursor.getIntValue(CallLog.Calls._ID)
                        val number = cursor.getStringValue(CallLog.Calls.NUMBER)
                        val date = cursor.getLongValue(CallLog.Calls.DATE)
                        val name = cursor.getStringValue(CallLog.Calls.CACHED_NAME)
                        if (number == prevNumber) {
                            continue
                        }

                        var formattedDate = SimpleDateFormat("dd MMM yyyy, $timeFormat", Locale.getDefault()).format(Date(date))
                        val datePart = formattedDate.substring(0, 11)
                        when {
                            datePart == todayDate -> formattedDate = formattedDate.substring(12)
                            datePart == yesterdayDate -> formattedDate = yesterday + formattedDate.substring(11)
                            formattedDate.substring(7, 11) == currentYear -> formattedDate = formattedDate.substring(0, 6) + formattedDate.substring(11)
                        }

                        prevNumber = number
                        val recentCall = RecentCall(id, number, formattedDate, name)
                        calls.add(recentCall)
                    } while (cursor.moveToNext())
                }
            } finally {
                cursor?.close()
            }
            callback(calls)
        }.start()
    }

    fun removeRecentCalls(ids: ArrayList<Int>) {
        Thread {
            try {
                val operations = ArrayList<ContentProviderOperation>()
                val selection = "${CallLog.Calls._ID} = ?"
                ids.forEach {
                    ContentProviderOperation.newDelete(CallLog.Calls.CONTENT_URI).apply {
                        val selectionArgs = arrayOf(it.toString())
                        withSelection(selection, selectionArgs)
                        operations.add(build())
                    }

                    if (operations.size % BATCH_SIZE == 0) {
                        activity.contentResolver.applyBatch(CallLog.AUTHORITY, operations)
                        operations.clear()
                    }
                }

                activity.contentResolver.applyBatch(CallLog.AUTHORITY, operations)
            } catch (e: Exception) {
                activity.showErrorToast(e)
            }
        }.start()
    }
}
