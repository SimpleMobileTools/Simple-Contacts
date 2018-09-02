package com.simplemobiletools.contacts.helpers

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.provider.ContactsContract.CommonDataKinds
import android.widget.Toast
import com.simplemobiletools.commons.extensions.showErrorToast
import com.simplemobiletools.contacts.activities.SimpleActivity
import com.simplemobiletools.contacts.extensions.getCachePhoto
import com.simplemobiletools.contacts.extensions.getCachePhotoUri
import com.simplemobiletools.contacts.helpers.VcfImporter.ImportResult.*
import com.simplemobiletools.contacts.models.*
import ezvcard.Ezvcard
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import java.io.File
import java.io.FileOutputStream
import java.util.*

class VcfImporter(val activity: SimpleActivity) {
    enum class ImportResult {
        IMPORT_FAIL, IMPORT_OK, IMPORT_PARTIAL
    }

    private val PATTERN = "EEE MMM dd HH:mm:ss 'GMT'ZZ YYYY"

    private var contactsImported = 0
    private var contactsFailed = 0

    fun importContacts(path: String, targetContactSource: String): ImportResult {
        try {
            val inputStream = if (path.contains("/")) {
                File(path).inputStream()
            } else {
                activity.assets.open(path)
            }

            val ezContacts = Ezvcard.parse(inputStream).all()
            for (ezContact in ezContacts) {
                val structuredName = ezContact.structuredName
                val prefix = structuredName?.prefixes?.firstOrNull() ?: ""
                val firstName = structuredName?.given ?: ""
                val middleName = structuredName?.additionalNames?.firstOrNull() ?: ""
                val surname = structuredName?.family ?: ""
                val suffix = structuredName?.suffixes?.firstOrNull() ?: ""
                val nickname = ezContact.nickname?.values?.firstOrNull() ?: ""
                val photoUri = ""

                val phoneNumbers = ArrayList<PhoneNumber>()
                ezContact.telephoneNumbers.forEach {
                    val type = getPhoneNumberTypeId(it.types.firstOrNull()?.value ?: MOBILE)
                    val number = it.text
                    phoneNumbers.add(PhoneNumber(number, type, ""))
                }

                val emails = ArrayList<Email>()
                ezContact.emails.forEach {
                    val type = getEmailTypeId(it.types.firstOrNull()?.value ?: HOME)
                    val email = it.value
                    emails.add(Email(email, type, ""))
                }

                val addresses = ArrayList<Address>()
                ezContact.addresses.forEach {
                    val type = getAddressTypeId(it.types.firstOrNull()?.value ?: HOME)
                    val address = it.streetAddress
                    if (address?.isNotEmpty() == true) {
                        addresses.add(Address(address, type, ""))
                    }
                }

                val events = ArrayList<Event>()
                ezContact.birthdays.forEach {
                    val event = Event(formatDateToDayCode(it.date), CommonDataKinds.Event.TYPE_BIRTHDAY)
                    events.add(event)
                }

                ezContact.anniversaries.forEach {
                    val event = Event(formatDateToDayCode(it.date), CommonDataKinds.Event.TYPE_ANNIVERSARY)
                    events.add(event)
                }

                val starred = 0
                val contactId = 0
                val notes = ezContact.notes.firstOrNull()?.value ?: ""
                val groups = ArrayList<Group>()
                val company = ezContact.organization?.values?.firstOrNull() ?: ""
                val jobPosition = ezContact.titles?.firstOrNull()?.value ?: ""
                val organization = Organization(company, jobPosition)
                val websites = ezContact.urls.map { it.value } as ArrayList<String>

                val photoData = ezContact.photos.firstOrNull()?.data
                val photo = null
                val thumbnailUri = savePhoto(photoData)

                val contact = Contact(0, prefix, firstName, middleName, surname, suffix, nickname, photoUri, phoneNumbers, emails, addresses, events,
                        targetContactSource, starred, contactId, thumbnailUri, photo, notes, groups, organization, websites)

                if (ContactsHelper(activity).insertContact(contact)) {
                    contactsImported++
                }
            }
        } catch (e: Exception) {
            activity.showErrorToast(e, Toast.LENGTH_LONG)
            contactsFailed++
        }

        return when {
            contactsImported == 0 -> IMPORT_FAIL
            contactsFailed > 0 -> IMPORT_PARTIAL
            else -> IMPORT_OK
        }
    }

    private fun formatDateToDayCode(date: Date): String {
        val dateTime = DateTime.parse(date.toString(), DateTimeFormat.forPattern(PATTERN))
        return dateTime.toString("yyyy-MM-dd")
    }

    private fun getPhoneNumberTypeId(type: String) = when (type.toUpperCase()) {
        CELL -> CommonDataKinds.Phone.TYPE_MOBILE
        HOME -> CommonDataKinds.Phone.TYPE_HOME
        WORK -> CommonDataKinds.Phone.TYPE_WORK
        PREF, MAIN -> CommonDataKinds.Phone.TYPE_MAIN
        WORK_FAX -> CommonDataKinds.Phone.TYPE_FAX_WORK
        HOME_FAX -> CommonDataKinds.Phone.TYPE_FAX_HOME
        FAX -> CommonDataKinds.Phone.TYPE_FAX_WORK
        PAGER -> CommonDataKinds.Phone.TYPE_PAGER
        else -> CommonDataKinds.Phone.TYPE_OTHER
    }

    private fun getEmailTypeId(type: String) = when (type.toUpperCase()) {
        HOME -> CommonDataKinds.Email.TYPE_HOME
        WORK -> CommonDataKinds.Email.TYPE_WORK
        MOBILE -> CommonDataKinds.Email.TYPE_MOBILE
        else -> CommonDataKinds.Email.TYPE_OTHER
    }

    private fun getAddressTypeId(type: String) = when (type.toUpperCase()) {
        HOME -> CommonDataKinds.Email.TYPE_HOME
        WORK -> CommonDataKinds.Email.TYPE_WORK
        else -> CommonDataKinds.Email.TYPE_OTHER
    }

    private fun savePhoto(byteArray: ByteArray?): String {
        if (byteArray == null) {
            return ""
        }

        val file = activity.getCachePhoto()
        val bitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
        var fileOutputStream: FileOutputStream? = null
        try {
            fileOutputStream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fileOutputStream)
        } finally {
            fileOutputStream?.close()
        }

        return activity.getCachePhotoUri(file).toString()
    }
}
