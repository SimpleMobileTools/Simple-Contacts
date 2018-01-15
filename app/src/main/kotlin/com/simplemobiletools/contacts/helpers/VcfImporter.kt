package com.simplemobiletools.contacts.helpers

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.provider.ContactsContract.CommonDataKinds
import android.util.Base64
import android.widget.Toast
import com.simplemobiletools.commons.extensions.showErrorToast
import com.simplemobiletools.contacts.activities.SimpleActivity
import com.simplemobiletools.contacts.extensions.getCachePhoto
import com.simplemobiletools.contacts.extensions.getCachePhotoUri
import com.simplemobiletools.contacts.helpers.VcfImporter.ImportResult.*
import com.simplemobiletools.contacts.models.Contact
import com.simplemobiletools.contacts.models.Email
import com.simplemobiletools.contacts.models.Event
import com.simplemobiletools.contacts.models.PhoneNumber
import java.io.File
import java.io.FileOutputStream

class VcfImporter(val activity: SimpleActivity) {
    enum class ImportResult {
        IMPORT_FAIL, IMPORT_OK, IMPORT_PARTIAL
    }

    private var curFirstName = ""
    private var curMiddleName = ""
    private var curSurname = ""
    private var curPhotoUri = ""
    private var curPhoneNumbers = ArrayList<PhoneNumber>()
    private var curEmails = ArrayList<Email>()
    private var curEvents = ArrayList<Event>()

    private var isGettingPhoto = false
    private var currentPhotoString = StringBuilder()
    private var currentPhotoCompressionFormat = Bitmap.CompressFormat.JPEG

    private var contactsImported = 0
    private var contactsFailed = 0

    fun importContacts(path: String, targetContactSource: String): ImportResult {
        try {
            val inputStream = if (path.contains("/")) {
                File(path).inputStream()
            } else {
                activity.assets.open(path)
            }

            inputStream.bufferedReader().use {
                while (true) {
                    val line = it.readLine() ?: break
                    if (line.trim().isEmpty()) {
                        if (isGettingPhoto) {
                            savePhoto()
                            isGettingPhoto = false
                        }
                        continue
                    }

                    when {
                        line == BEGIN_VCARD -> resetValues()
                        line.startsWith(N) -> parseNames(line.substring(N.length))
                        line.startsWith(TEL) -> addPhoneNumber(line.substring(TEL.length))
                        line.startsWith(EMAIL) -> addEmail(line.substring(EMAIL.length))
                        line.startsWith(BDAY) -> addBirthday(line.substring(BDAY.length))
                        line.startsWith(ANNIVERSARY) -> addAnniversary(line.substring(ANNIVERSARY.length))
                        line.startsWith(PHOTO) -> addPhoto(line.substring(PHOTO.length))
                        line == END_VCARD -> saveContact(targetContactSource)
                        isGettingPhoto -> currentPhotoString.append(line.trim())
                    }
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

    private fun parseNames(names: String) {
        val nameParts = names.split(";")
        curFirstName = nameParts[1]
        curMiddleName = nameParts[2]
        curSurname = nameParts[0]
    }

    private fun addPhoneNumber(phoneNumber: String) {
        val phoneParts = phoneNumber.trimStart(';').split(":")
        var rawType = phoneParts[0]
        var subType = ""
        if (rawType.contains('=')) {
            val types = rawType.split('=')
            if (types.any { it.contains(';') }) {
                subType = types[1].split(';')[0]
            }
            rawType = types.last()
        }

        val type = getPhoneNumberTypeId(rawType, subType)
        val value = phoneParts[1]
        curPhoneNumbers.add(PhoneNumber(value, type))
    }

    private fun getPhoneNumberTypeId(type: String, subType: String) = when (type) {
        CELL -> CommonDataKinds.Phone.TYPE_MOBILE
        HOME -> CommonDataKinds.Phone.TYPE_HOME
        WORK -> CommonDataKinds.Phone.TYPE_WORK
        PREF, MAIN -> CommonDataKinds.Phone.TYPE_MAIN
        WORK_FAX -> CommonDataKinds.Phone.TYPE_FAX_WORK
        HOME_FAX -> CommonDataKinds.Phone.TYPE_FAX_HOME
        FAX -> if (subType == WORK) CommonDataKinds.Phone.TYPE_FAX_WORK else CommonDataKinds.Phone.TYPE_FAX_HOME
        PAGER -> CommonDataKinds.Phone.TYPE_PAGER
        else -> CommonDataKinds.Phone.TYPE_OTHER
    }

    private fun addEmail(email: String) {
        val emailParts = email.trimStart(';').split(":")
        var rawType = emailParts[0]
        if (rawType.contains('=')) {
            rawType = rawType.split('=').last()
        }
        val type = getEmailTypeId(rawType)
        val value = emailParts[1]
        curEmails.add(Email(value, type))
    }

    private fun getEmailTypeId(type: String) = when (type) {
        HOME -> CommonDataKinds.Email.TYPE_HOME
        WORK -> CommonDataKinds.Email.TYPE_WORK
        MOBILE -> CommonDataKinds.Email.TYPE_MOBILE
        else -> CommonDataKinds.Email.TYPE_OTHER
    }

    private fun addBirthday(birthday: String) {
        curEvents.add(Event(birthday, CommonDataKinds.Event.TYPE_BIRTHDAY))
    }

    private fun addAnniversary(anniversary: String) {
        curEvents.add(Event(anniversary, CommonDataKinds.Event.TYPE_ANNIVERSARY))
    }

    private fun addPhoto(photo: String) {
        val photoParts = photo.trimStart(';').split(';')
        if (photoParts.size == 2) {
            val typeParts = photoParts[1].split(':')
            currentPhotoCompressionFormat = getPhotoCompressionFormat(typeParts[0])
            val encoding = photoParts[0].split('=').last()
            if (encoding == BASE64) {
                isGettingPhoto = true
                currentPhotoString.append(typeParts[1].trim())
            }
        }
    }

    private fun getPhotoCompressionFormat(type: String) = when (type.toLowerCase()) {
        "png" -> Bitmap.CompressFormat.PNG
        "webp" -> Bitmap.CompressFormat.WEBP
        else -> Bitmap.CompressFormat.JPEG
    }

    private fun savePhoto() {
        val file = activity.getCachePhoto()
        val imageAsBytes = Base64.decode(currentPhotoString.toString().toByteArray(), Base64.DEFAULT)
        val bitmap = BitmapFactory.decodeByteArray(imageAsBytes, 0, imageAsBytes.size)
        var fileOutputStream: FileOutputStream? = null
        try {
            fileOutputStream = FileOutputStream(file)
            bitmap.compress(currentPhotoCompressionFormat, 100, fileOutputStream)
        } finally {
            fileOutputStream?.close()
        }

        curPhotoUri = activity.getCachePhotoUri(file).toString()
    }

    private fun saveContact(source: String) {
        val contact = Contact(0, curFirstName, curMiddleName, curSurname, curPhotoUri, curPhoneNumbers, curEmails, curEvents, source, 0, 0, "")
        if (ContactsHelper(activity).insertContact(contact)) {
            contactsImported++
        }
    }

    private fun resetValues() {
        curFirstName = ""
        curMiddleName = ""
        curSurname = ""
        curPhotoUri = ""
        curPhoneNumbers = ArrayList()
        curEmails = ArrayList()
        curEvents = ArrayList()

        isGettingPhoto = false
        currentPhotoString = StringBuilder()
        currentPhotoCompressionFormat = Bitmap.CompressFormat.JPEG
    }
}
