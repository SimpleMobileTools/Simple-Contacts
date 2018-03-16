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
import com.simplemobiletools.contacts.models.*
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
    private var curNotes = ""
    private var curPhoneNumbers = ArrayList<PhoneNumber>()
    private var curEmails = ArrayList<Email>()
    private var curEvents = ArrayList<Event>()
    private var curAddresses = ArrayList<Address>()
    private var curGroups = ArrayList<Group>()

    private var isGettingPhoto = false
    private var currentPhotoString = StringBuilder()
    private var currentPhotoCompressionFormat = Bitmap.CompressFormat.JPEG

    private var isGettingName = false
    private var currentNameIsANSI = false
    private var currentNameString = StringBuilder()

    private var isGettingNotes = false
    private var currentNotesSB = StringBuilder()

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
                    } else if (line.startsWith('\t') && isGettingName) {
                        currentNameString.append(line.trimStart('\t'))
                        isGettingName = false
                        parseNames()
                    } else if (isGettingNotes) {
                        if (line.startsWith(' ')) {
                            currentNotesSB.append(line.substring(1))
                        } else {
                            curNotes = currentNotesSB.toString().replace("\\n", "\n").replace("\\,", ",")
                            isGettingNotes = false
                        }
                    }

                    when {
                        line.toUpperCase() == BEGIN_VCARD -> resetValues()
                        line.toUpperCase().startsWith(NOTE) -> addNotes(line.substring(NOTE.length))
                        line.toUpperCase().startsWith(N) -> addNames(line.substring(N.length))
                        line.toUpperCase().startsWith(TEL) -> addPhoneNumber(line.substring(TEL.length))
                        line.toUpperCase().startsWith(EMAIL) -> addEmail(line.substring(EMAIL.length))
                        line.toUpperCase().startsWith(ADR) -> addAddress(line.substring(ADR.length))
                        line.toUpperCase().startsWith(BDAY) -> addBirthday(line.substring(BDAY.length))
                        line.toUpperCase().startsWith(ANNIVERSARY) -> addAnniversary(line.substring(ANNIVERSARY.length))
                        line.toUpperCase().startsWith(PHOTO) -> addPhoto(line.substring(PHOTO.length))
                        line.toUpperCase() == END_VCARD -> saveContact(targetContactSource)
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

    private fun addNames(names: String) {
        val parts = names.split(":")
        currentNameIsANSI = parts.first().toUpperCase().contains("QUOTED-PRINTABLE")
        currentNameString.append(parts[1].trimEnd('='))
        if (!isGettingName && currentNameIsANSI && names.endsWith('=')) {
            isGettingName = true
        } else {
            parseNames()
        }
    }

    private fun parseNames() {
        val nameParts = currentNameString.split(";")
        curSurname = if (currentNameIsANSI) QuotedPrintable.decode(nameParts[0]) else nameParts[0]
        curFirstName = if (currentNameIsANSI) QuotedPrintable.decode(nameParts[1]) else nameParts[1]
        if (nameParts.size > 2) {
            curMiddleName = if (currentNameIsANSI) QuotedPrintable.decode(nameParts[2]) else nameParts[2]
        }
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

        val type = getPhoneNumberTypeId(rawType.toUpperCase(), subType)
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
        val type = getEmailTypeId(rawType.toUpperCase())
        val value = emailParts[1]
        curEmails.add(Email(value, type))
    }

    private fun getEmailTypeId(type: String) = when (type) {
        HOME -> CommonDataKinds.Email.TYPE_HOME
        WORK -> CommonDataKinds.Email.TYPE_WORK
        MOBILE -> CommonDataKinds.Email.TYPE_MOBILE
        else -> CommonDataKinds.Email.TYPE_OTHER
    }

    private fun addAddress(address: String) {
        val addressParts = address.trimStart(';').split(":")
        var rawType = addressParts[0]
        if (rawType.contains('=')) {
            rawType = rawType.split('=').last()
        }
        val type = getAddressTypeId(rawType.toUpperCase())
        val addresses = addressParts[1].split(";")
        if (addresses.size == 7) {
            curAddresses.add(Address(addresses[2], type))
        }
    }

    private fun getAddressTypeId(type: String) = when (type) {
        HOME -> CommonDataKinds.Email.TYPE_HOME
        WORK -> CommonDataKinds.Email.TYPE_WORK
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

    private fun addNotes(notes: String) {
        currentNotesSB.append(notes)
        isGettingNotes = true
    }

    private fun saveContact(source: String) {
        val contact = Contact(0, curFirstName, curMiddleName, curSurname, curPhotoUri, curPhoneNumbers, curEmails, curAddresses, curEvents,
                source, 0, 0, "", null, curNotes, curGroups)
        if (ContactsHelper(activity).insertContact(contact)) {
            contactsImported++
        }
    }

    private fun resetValues() {
        curFirstName = ""
        curMiddleName = ""
        curSurname = ""
        curPhotoUri = ""
        curNotes = ""
        curPhoneNumbers = ArrayList()
        curEmails = ArrayList()
        curEvents = ArrayList()
        curAddresses = ArrayList()
        curGroups = ArrayList()

        isGettingPhoto = false
        currentPhotoString = StringBuilder()
        currentPhotoCompressionFormat = Bitmap.CompressFormat.JPEG

        isGettingName = false
        currentNameIsANSI = false
        currentNameString = StringBuilder()

        isGettingNotes = false
        currentNotesSB = StringBuilder()
    }
}
