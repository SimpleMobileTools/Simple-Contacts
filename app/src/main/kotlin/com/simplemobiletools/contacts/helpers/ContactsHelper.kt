package com.simplemobiletools.contacts.helpers

import android.database.Cursor
import android.provider.ContactsContract
import com.simplemobiletools.commons.extensions.getIntValue
import com.simplemobiletools.commons.extensions.getStringValue
import com.simplemobiletools.commons.extensions.showErrorToast
import com.simplemobiletools.commons.helpers.SORT_BY_NAME
import com.simplemobiletools.commons.helpers.SORT_DESCENDING
import com.simplemobiletools.contacts.activities.SimpleActivity
import com.simplemobiletools.contacts.extensions.config
import com.simplemobiletools.contacts.models.Contact
import java.util.*

class ContactsHelper(val activity: SimpleActivity) {
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

            callback(ArrayList<String>(accounts))
        }.start()
    }

    fun getContacts(callback: (ArrayList<Contact>) -> Unit) {
        val contacts = HashMap<Int, Contact>()
        Thread {
            val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
            val projection = getContactProjection()
            val sortOrder = getSortString()
            var cursor: Cursor? = null
            try {
                cursor = activity.contentResolver.query(uri, projection, null, null, sortOrder)
                if (cursor?.moveToFirst() == true) {
                    do {
                        val id = cursor.getIntValue(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
                        val name = cursor.getStringValue(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME) ?: continue
                        val number = cursor.getStringValue(ContactsContract.CommonDataKinds.Phone.NUMBER) ?: ""
                        val photoUri = cursor.getStringValue(ContactsContract.CommonDataKinds.Phone.PHOTO_URI) ?: ""
                        val contact = Contact(id, name, number, photoUri, "")
                        contacts.put(id, contact)
                    } while (cursor.moveToNext())
                }
            } catch (e: Exception) {
                activity.showErrorToast(e)
            } finally {
                cursor?.close()
            }

            getEmails().forEach {
                if (contacts.containsKey(it.first)) {
                    contacts[it.first]!!.email = it.second
                }
            }

            callback(ArrayList(contacts.values))
        }.start()
    }

    private fun getEmails(): ArrayList<Pair<Int, String>> {
        val pairs = ArrayList<Pair<Int, String>>()
        val uri = ContactsContract.CommonDataKinds.Email.CONTENT_URI
        val projection = arrayOf(ContactsContract.CommonDataKinds.Phone.CONTACT_ID, ContactsContract.CommonDataKinds.Email.DATA)
        var cursor: Cursor? = null
        try {
            cursor = activity.contentResolver.query(uri, projection, null, null, null)
            if (cursor?.moveToFirst() == true) {
                do {
                    val id = cursor.getIntValue(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
                    val email = cursor.getStringValue(ContactsContract.CommonDataKinds.Email.DATA)
                    pairs.add(Pair(id, email))
                } while (cursor.moveToNext())
            }
        } finally {
            cursor?.close()
        }
        return pairs
    }

    fun getContactEmail(id: Int): String {
        val uri = ContactsContract.CommonDataKinds.Email.CONTENT_URI
        val projection = arrayOf(ContactsContract.CommonDataKinds.Email.DATA)
        val selection = "${ContactsContract.CommonDataKinds.Email.CONTACT_ID} = ?"
        val selectionArgs = arrayOf(id.toString())
        var cursor: Cursor? = null
        try {
            cursor = activity.contentResolver.query(uri, projection, selection, selectionArgs, null)
            if (cursor?.moveToFirst() == true) {
                return cursor.getStringValue(ContactsContract.CommonDataKinds.Email.DATA)
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

        val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
        val projection = getContactProjection()
        val selection = "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?"
        val selectionArgs = arrayOf(id.toString())
        var cursor: Cursor? = null
        try {
            cursor = activity.contentResolver.query(uri, projection, selection, selectionArgs, null)
            if (cursor?.moveToFirst() == true) {
                val name = cursor.getStringValue(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME) ?: return null
                val number = cursor.getStringValue(ContactsContract.CommonDataKinds.Phone.NUMBER) ?: ""
                val photoUri = cursor.getStringValue(ContactsContract.CommonDataKinds.Phone.PHOTO_URI) ?: ""
                val email = getContactEmail(id)
                return Contact(id, name, number, photoUri, email)
            }
        } finally {
            cursor?.close()
        }

        return null
    }

    private fun getContactProjection() = arrayOf(
            ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER,
            ContactsContract.CommonDataKinds.Phone.PHOTO_URI
    )

    private fun getSortString(): String {
        val sorting = activity.config.sorting
        var sort = when {
            sorting and SORT_BY_NAME != 0 -> "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} COLLATE NOCASE"
            else -> ContactsContract.CommonDataKinds.Phone.NUMBER
        }

        if (sorting and SORT_DESCENDING != 0) {
            sort += " DESC"
        }
        return sort
    }
}
