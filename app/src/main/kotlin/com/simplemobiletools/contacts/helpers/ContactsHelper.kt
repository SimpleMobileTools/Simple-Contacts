package com.simplemobiletools.contacts.helpers

import android.content.ContentProviderOperation
import android.database.Cursor
import android.provider.ContactsContract
import android.util.SparseArray
import com.simplemobiletools.commons.activities.BaseSimpleActivity
import com.simplemobiletools.commons.extensions.getIntValue
import com.simplemobiletools.commons.extensions.getStringValue
import com.simplemobiletools.commons.extensions.showErrorToast
import com.simplemobiletools.commons.helpers.SORT_BY_FIRST_NAME
import com.simplemobiletools.commons.helpers.SORT_BY_MIDDLE_NAME
import com.simplemobiletools.commons.helpers.SORT_BY_SURNAME
import com.simplemobiletools.commons.helpers.SORT_DESCENDING
import com.simplemobiletools.contacts.extensions.config
import com.simplemobiletools.contacts.models.*
import com.simplemobiletools.contacts.overloads.times
import java.util.*

class ContactsHelper(val activity: BaseSimpleActivity) {
    fun getContactSources(callback: (ArrayList<String>) -> Unit) {
        val accounts = HashSet<String>()
        Thread {
            val uri = ContactsContract.RawContacts.CONTENT_URI
            val projection = arrayOf(ContactsContract.RawContacts.ACCOUNT_NAME)
            var cursor: Cursor? = null
            try {
                cursor = activity.contentResolver.query(uri, projection, null, null, null)
                if (cursor?.moveToFirst() == true) {
                    do {
                        accounts.add(cursor.getStringValue(ContactsContract.RawContacts.ACCOUNT_NAME))
                    } while (cursor.moveToNext())
                }
            } finally {
                cursor?.close()
            }

            val sourcesWithContacts = ArrayList(accounts).filter { doesSourceContainContacts(it) } as ArrayList
            callback(sourcesWithContacts)
        }.start()
    }

    fun getContacts(callback: (ArrayList<Contact>) -> Unit) {
        val contacts = SparseArray<Contact>()
        Thread {
            val sources = activity.config.displayContactSources
            val questionMarks = ("?," * sources.size).trimEnd(',')
            val uri = ContactsContract.Data.CONTENT_URI
            val projection = getContactProjection()
            var selection = "${ContactsContract.Data.MIMETYPE} = ?"
            var selectionArgs = arrayOf(ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
            if (!activity.config.showAllContacts()) {
                selection += " AND ${ContactsContract.RawContacts.ACCOUNT_NAME} IN ($questionMarks)"
                selectionArgs += sources.toTypedArray()
            }
            val sortOrder = getSortString()
            var cursor: Cursor? = null
            try {
                cursor = activity.contentResolver.query(uri, projection, selection, selectionArgs, sortOrder)
                if (cursor?.moveToFirst() == true) {
                    do {
                        val id = cursor.getIntValue(ContactsContract.CommonDataKinds.StructuredName.CONTACT_ID)
                        val firstName = cursor.getStringValue(ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME) ?: ""
                        val middleName = cursor.getStringValue(ContactsContract.CommonDataKinds.StructuredName.MIDDLE_NAME) ?: ""
                        val surname = cursor.getStringValue(ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME) ?: ""
                        if (firstName.isEmpty() && middleName.isEmpty() && surname.isEmpty())
                            continue

                        val photoUri = cursor.getStringValue(ContactsContract.CommonDataKinds.StructuredName.PHOTO_URI) ?: ""
                        val number = PhoneNumbers()     // proper value is obtained below
                        val emails = Emails()           // proper value is obtained below
                        val accountName = cursor.getStringValue(ContactsContract.RawContacts.ACCOUNT_NAME)
                        val contact = Contact(id, firstName, middleName, surname, photoUri, number, emails, accountName)
                        contacts.put(id, contact)
                    } while (cursor.moveToNext())
                }
            } catch (e: Exception) {
                activity.showErrorToast(e)
            } finally {
                cursor?.close()
            }

            val emails = getEmails()
            var size = emails.size()
            for (i in 0 until size) {
                val key = emails.keyAt(i)
                contacts[key]?.emails = emails.valueAt(i)
            }

            val phoneNumbers = getPhoneNumbers()
            size = phoneNumbers.size()
            for (i in 0 until size) {
                val key = phoneNumbers.keyAt(i)
                contacts[key]?.phoneNumbers = phoneNumbers.valueAt(i)
            }

            val contactsSize = contacts.size()
            val resultContacts = ArrayList<Contact>(contactsSize)
            (0 until contactsSize).mapTo(resultContacts) { contacts.valueAt(it) }
            callback(resultContacts)
        }.start()
    }

    fun doesSourceContainContacts(source: String): Boolean {
        val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
        val projection = arrayOf(ContactsContract.CommonDataKinds.Email.CONTACT_ID)
        val selection = "${ContactsContract.RawContacts.ACCOUNT_NAME} = ?"
        val selectionArgs = arrayOf(source)
        var cursor: Cursor? = null
        try {
            cursor = activity.contentResolver.query(uri, projection, selection, selectionArgs, null)
            return (cursor?.moveToFirst() == true)
        } finally {
            cursor?.close()
        }
    }

    private fun getEmails(contactId: Int? = null): SparseArray<Emails> {
        val emails = SparseArray<Emails>()
        val uri = ContactsContract.CommonDataKinds.Email.CONTENT_URI
        val projection = arrayOf(
                ContactsContract.CommonDataKinds.Email.CONTACT_ID,
                ContactsContract.CommonDataKinds.Email.DATA,
                ContactsContract.CommonDataKinds.Email.TYPE
        )

        val selection = if (contactId == null) null else "${ContactsContract.CommonDataKinds.Email.CONTACT_ID} = ?"
        val selectionArgs = if (contactId == null) null else arrayOf(contactId.toString())

        var cursor: Cursor? = null
        try {
            cursor = activity.contentResolver.query(uri, projection, selection, selectionArgs, null)
            if (cursor?.moveToFirst() == true) {
                do {
                    val id = cursor.getIntValue(ContactsContract.CommonDataKinds.Email.CONTACT_ID)
                    val email = cursor.getStringValue(ContactsContract.CommonDataKinds.Email.DATA)
                    val type = cursor.getIntValue(ContactsContract.CommonDataKinds.Email.TYPE)

                    if (emails[id] == null) {
                        emails.put(id, Emails())
                    }

                    emails[id]!!.emails.add(Email(email, type))
                } while (cursor.moveToNext())
            }
        } finally {
            cursor?.close()
        }

        return emails
    }

    private fun getPhoneNumbers(contactId: Int? = null): SparseArray<PhoneNumbers> {
        val phoneNumbers = SparseArray<PhoneNumbers>()
        val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
        val projection = arrayOf(
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                ContactsContract.CommonDataKinds.Phone.NUMBER,
                ContactsContract.CommonDataKinds.Phone.TYPE
        )

        val selection = if (contactId == null) null else "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?"
        val selectionArgs = if (contactId == null) null else arrayOf(contactId.toString())

        var cursor: Cursor? = null
        try {
            cursor = activity.contentResolver.query(uri, projection, selection, selectionArgs, null)
            if (cursor?.moveToFirst() == true) {
                do {
                    val id = cursor.getIntValue(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
                    val number = cursor.getStringValue(ContactsContract.CommonDataKinds.Phone.NUMBER)
                    val type = cursor.getIntValue(ContactsContract.CommonDataKinds.Phone.TYPE)

                    if (phoneNumbers[id] == null) {
                        phoneNumbers.put(id, PhoneNumbers())
                    }

                    phoneNumbers[id].phoneNumbers.add(PhoneNumber(number, type))
                } while (cursor.moveToNext())
            }
        } finally {
            cursor?.close()
        }
        return phoneNumbers
    }

    fun getContactNumber(id: Int): String {
        val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
        val projection = arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER)
        val selection = "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?"
        val selectionArgs = arrayOf(id.toString())
        var cursor: Cursor? = null
        try {
            cursor = activity.contentResolver.query(uri, projection, selection, selectionArgs, null)
            if (cursor?.moveToFirst() == true) {
                return cursor.getStringValue(ContactsContract.CommonDataKinds.Phone.NUMBER)
            }
        } finally {
            cursor?.close()
        }

        return ""
    }

    fun getContactWithId(id: Int): Contact? {
        if (id == 0) {
            return null
        }

        val uri = ContactsContract.Data.CONTENT_URI
        val projection = getContactProjection()
        val selection = "${ContactsContract.Data.MIMETYPE} = ? AND ${ContactsContract.CommonDataKinds.StructuredName.CONTACT_ID} = ?"
        val selectionArgs = arrayOf(ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE, id.toString())
        var cursor: Cursor? = null
        try {
            cursor = activity.contentResolver.query(uri, projection, selection, selectionArgs, null)
            if (cursor?.moveToFirst() == true) {
                val firstName = cursor.getStringValue(ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME) ?: ""
                val middleName = cursor.getStringValue(ContactsContract.CommonDataKinds.StructuredName.MIDDLE_NAME) ?: ""
                val surname = cursor.getStringValue(ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME) ?: ""
                val photoUri = cursor.getStringValue(ContactsContract.CommonDataKinds.Phone.PHOTO_URI) ?: ""
                val number = getPhoneNumbers(id)[id] ?: PhoneNumbers()
                val emails = getEmails(id)[id] ?: Emails()
                val accountName = cursor.getStringValue(ContactsContract.RawContacts.ACCOUNT_NAME)
                return Contact(id, firstName, middleName, surname, photoUri, number, emails, accountName)
            }
        } finally {
            cursor?.close()
        }

        return null
    }

    private fun getContactProjection() = arrayOf(
            ContactsContract.CommonDataKinds.StructuredName.CONTACT_ID,
            ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME,
            ContactsContract.CommonDataKinds.StructuredName.MIDDLE_NAME,
            ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME,
            ContactsContract.CommonDataKinds.StructuredName.PHOTO_URI,
            ContactsContract.RawContacts.ACCOUNT_NAME
    )

    private fun getSortString(): String {
        val sorting = activity.config.sorting
        var sort = when {
            sorting and SORT_BY_FIRST_NAME != 0 -> "${ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME} COLLATE NOCASE"
            sorting and SORT_BY_MIDDLE_NAME != 0 -> "${ContactsContract.CommonDataKinds.StructuredName.MIDDLE_NAME} COLLATE NOCASE"
            sorting and SORT_BY_SURNAME != 0 -> "${ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME} COLLATE NOCASE"
            else -> ContactsContract.CommonDataKinds.Phone.NUMBER
        }

        if (sorting and SORT_DESCENDING != 0) {
            sort += " DESC"
        }
        return sort
    }

    fun updateContact(contact: Contact): Boolean {
        return try {
            val operations = ArrayList<ContentProviderOperation>()
            ContentProviderOperation.newUpdate(ContactsContract.Data.CONTENT_URI).apply {
                val selection = "${ContactsContract.Data.CONTACT_ID} = ? AND ${ContactsContract.Data.MIMETYPE} = ?"
                val selectionArgs = arrayOf(contact.id.toString(), ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                withSelection(selection, selectionArgs)
                withValue(ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME, contact.firstName)
                withValue(ContactsContract.CommonDataKinds.StructuredName.MIDDLE_NAME, contact.middleName)
                withValue(ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME, contact.surname)
                operations.add(this.build())
            }

            activity.contentResolver.applyBatch(ContactsContract.AUTHORITY, operations)
            true
        } catch (e: Exception) {
            activity.showErrorToast(e)
            false
        }
    }

    fun deleteContact(contact: Contact) = deleteContacts(arrayListOf(contact))

    fun deleteContacts(contacts: ArrayList<Contact>) {
        try {
            val operations = ArrayList<ContentProviderOperation>()
            val selection = "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?"
            contacts.forEach {
                val selectionArgs = arrayOf(it.id.toString())
                operations.add(ContentProviderOperation.newDelete(ContactsContract.RawContacts.CONTENT_URI).withSelection(selection, selectionArgs).build())
            }
            activity.contentResolver.applyBatch(ContactsContract.AUTHORITY, operations)
        } catch (e: Exception) {
            activity.showErrorToast(e)
        }
    }
}
