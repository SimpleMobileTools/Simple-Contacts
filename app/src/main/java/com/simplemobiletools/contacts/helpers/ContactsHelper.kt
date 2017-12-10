package com.simplemobiletools.contacts.helpers

import android.database.Cursor
import android.provider.ContactsContract
import com.simplemobiletools.commons.extensions.getIntValue
import com.simplemobiletools.commons.extensions.getStringValue
import com.simplemobiletools.commons.extensions.showErrorToast
import com.simplemobiletools.contacts.activities.SimpleActivity
import com.simplemobiletools.contacts.models.Contact

class ContactsHelper(val activity: SimpleActivity) {
    fun getContacts(callback: (ArrayList<Contact>) -> Unit) {
        val contacts = ArrayList<Contact>()
        Thread {
            val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
            val projection = arrayOf(
                    ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Phone.NUMBER,
                    ContactsContract.CommonDataKinds.Phone.PHOTO_URI
            )

            var cursor: Cursor? = null
            try {
                cursor = activity.contentResolver.query(uri, projection, null, null, null)
                if (cursor?.moveToFirst() == true) {
                    do {
                        val id = cursor.getIntValue(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
                        val name = cursor.getStringValue(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME) ?: continue
                        val number = cursor.getStringValue(ContactsContract.CommonDataKinds.Phone.NUMBER) ?: continue
                        val photoUri = cursor.getStringValue(ContactsContract.CommonDataKinds.Phone.PHOTO_URI) ?: ""
                        val contact = Contact(id, name, number, photoUri)
                        contacts.add(contact)
                    } while (cursor.moveToNext())
                }
            } catch (e: Exception) {
                activity.showErrorToast(e)
            } finally {
                cursor?.close()
            }
            callback(contacts)
        }.start()
    }
}
