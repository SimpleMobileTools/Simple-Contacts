package com.simplemobiletools.contacts.helpers

import android.provider.ContactsContract.CommonDataKinds
import android.widget.Toast
import com.simplemobiletools.commons.extensions.showErrorToast
import com.simplemobiletools.contacts.activities.SimpleActivity
import com.simplemobiletools.contacts.helpers.VcfImporter.ImportResult.*
import com.simplemobiletools.contacts.models.Contact
import com.simplemobiletools.contacts.models.Email
import com.simplemobiletools.contacts.models.Event
import com.simplemobiletools.contacts.models.PhoneNumber
import java.io.File

class VcfImporter(val activity: SimpleActivity) {
    enum class ImportResult {
        IMPORT_FAIL, IMPORT_OK, IMPORT_PARTIAL
    }

    private var curFirstName = ""
    private var curMiddleName = ""
    private var curSurname = ""
    private var curPhoneNumbers = ArrayList<PhoneNumber>()
    private var curEmails = ArrayList<Email>()
    private var curEvents = ArrayList<Event>()

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
                        continue
                    }

                    when {
                        line == BEGIN_VCARD -> resetValues()
                        line.startsWith(N) -> parseNames(line.substring(N.length))
                        line.startsWith(TEL) -> addPhoneNumber(line.substring(TEL.length))
                        line.startsWith(EMAIL) -> addEmail(line.substring(EMAIL.length))
                        line.startsWith(BDAY) -> addBirthday(line.substring(BDAY.length))
                        line.startsWith(ANNIVERSARY) -> addAnniversary(line.substring(ANNIVERSARY.length))
                        line == END_VCARD -> saveContact(targetContactSource)
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
        val type = getPhoneNumberTypeId(phoneParts[0])
        val value = phoneParts[1]
        curPhoneNumbers.add(PhoneNumber(value, type))
    }

    private fun getPhoneNumberTypeId(type: String) = when (type) {
        CELL -> CommonDataKinds.Phone.TYPE_MOBILE
        WORK -> CommonDataKinds.Phone.TYPE_WORK
        HOME -> CommonDataKinds.Phone.TYPE_HOME
        PREF -> CommonDataKinds.Phone.TYPE_MAIN
        WORK_FAX -> CommonDataKinds.Phone.TYPE_FAX_WORK
        HOME_FAX -> CommonDataKinds.Phone.TYPE_FAX_HOME
        PAGER -> CommonDataKinds.Phone.TYPE_PAGER
        else -> CommonDataKinds.Phone.TYPE_OTHER
    }

    private fun addEmail(email: String) {
        val emailParts = email.trimStart(';').split(":")
        val type = getEmailTypeId(emailParts[0])
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

    private fun saveContact(source: String) {
        val contact = Contact(0, curFirstName, curMiddleName, curSurname, "", curPhoneNumbers, curEmails, curEvents, source, 0, 0)
        if (ContactsHelper(activity).insertContact(contact)) {
            contactsImported++
        }
    }

    private fun resetValues() {
        curFirstName = ""
        curMiddleName = ""
        curSurname = ""
        curPhoneNumbers = ArrayList()
        curEmails = ArrayList()
        curEvents = ArrayList()
    }
}
