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
                    val number = it.text
                    val type = getPhoneNumberTypeId(it.types.firstOrNull()?.value ?: MOBILE, it.types.getOrNull(1)?.value)
                    val label = if (type == CommonDataKinds.Phone.TYPE_CUSTOM) {
                        it.types.firstOrNull()?.value ?: ""
                    } else {
                        ""
                    }

                    phoneNumbers.add(PhoneNumber(number, type, label))
                }

                val emails = ArrayList<Email>()
                ezContact.emails.forEach {
                    val email = it.value
                    val type = getEmailTypeId(it.types.firstOrNull()?.value ?: HOME)
                    val label = if (type == CommonDataKinds.Email.TYPE_CUSTOM) {
                        it.types.firstOrNull()?.value ?: ""
                    } else {
                        ""
                    }

                    emails.add(Email(email, type, label))
                }

                val addresses = ArrayList<Address>()
                ezContact.addresses.forEach {
                    val address = it.streetAddress
                    val type = getAddressTypeId(it.types.firstOrNull()?.value ?: HOME)
                    val label = if (type == CommonDataKinds.StructuredPostal.TYPE_CUSTOM) {
                        it.types.firstOrNull()?.value ?: ""
                    } else {
                        ""
                    }

                    if (address?.isNotEmpty() == true) {
                        addresses.add(Address(address, type, label))
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
                val cleanPhoneNumbers = ArrayList<PhoneNumber>()
                val IMs = ArrayList<IM>()

                val contact = Contact(0, prefix, firstName, middleName, surname, suffix, nickname, photoUri, phoneNumbers, emails, addresses, events,
                        targetContactSource, starred, contactId, thumbnailUri, photo, notes, groups, organization, websites, cleanPhoneNumbers, IMs)

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

    private fun getPhoneNumberTypeId(type: String, subtype: String?) = when (type.toUpperCase()) {
        CELL -> CommonDataKinds.Phone.TYPE_MOBILE
        HOME -> {
            if (subtype?.toUpperCase() == FAX) {
                CommonDataKinds.Phone.TYPE_FAX_HOME
            } else {
                CommonDataKinds.Phone.TYPE_HOME
            }
        }
        WORK -> {
            if (subtype?.toUpperCase() == FAX) {
                CommonDataKinds.Phone.TYPE_FAX_WORK
            } else {
                CommonDataKinds.Phone.TYPE_WORK
            }
        }
        PREF, MAIN -> CommonDataKinds.Phone.TYPE_MAIN
        WORK_FAX -> CommonDataKinds.Phone.TYPE_FAX_WORK
        HOME_FAX -> CommonDataKinds.Phone.TYPE_FAX_HOME
        FAX -> CommonDataKinds.Phone.TYPE_FAX_WORK
        PAGER -> CommonDataKinds.Phone.TYPE_PAGER
        OTHER -> CommonDataKinds.Phone.TYPE_OTHER
        else -> CommonDataKinds.Phone.TYPE_CUSTOM
    }

    private fun getEmailTypeId(type: String) = when (type.toUpperCase()) {
        HOME -> CommonDataKinds.Email.TYPE_HOME
        WORK -> CommonDataKinds.Email.TYPE_WORK
        MOBILE -> CommonDataKinds.Email.TYPE_MOBILE
        OTHER -> CommonDataKinds.Email.TYPE_OTHER
        else -> CommonDataKinds.Email.TYPE_CUSTOM
    }

    private fun getAddressTypeId(type: String) = when (type.toUpperCase()) {
        HOME -> CommonDataKinds.StructuredPostal.TYPE_HOME
        WORK -> CommonDataKinds.StructuredPostal.TYPE_WORK
        OTHER -> CommonDataKinds.StructuredPostal.TYPE_OTHER
        else -> CommonDataKinds.StructuredPostal.TYPE_CUSTOM
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
