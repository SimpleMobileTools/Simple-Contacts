package com.simplemobiletools.contacts.pro.helpers

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.provider.ContactsContract.CommonDataKinds
import android.provider.ContactsContract.CommonDataKinds.Im
import android.provider.ContactsContract.CommonDataKinds.Phone
import android.provider.ContactsContract.CommonDataKinds.StructuredPostal
import android.widget.Toast
import com.simplemobiletools.commons.extensions.groupsDB
import com.simplemobiletools.commons.extensions.normalizePhoneNumber
import com.simplemobiletools.commons.extensions.showErrorToast
import com.simplemobiletools.commons.extensions.getCachePhoto
import com.simplemobiletools.contacts.pro.extensions.getCachePhotoUri
import com.simplemobiletools.commons.helpers.ContactsHelper
import com.simplemobiletools.commons.helpers.DEFAULT_MIMETYPE
import com.simplemobiletools.commons.models.PhoneNumber
import com.simplemobiletools.commons.models.contacts.*
import com.simplemobiletools.contacts.pro.activities.SimpleActivity
import com.simplemobiletools.contacts.pro.helpers.VcfImporter.ImportResult.IMPORT_FAIL
import com.simplemobiletools.contacts.pro.helpers.VcfImporter.ImportResult.IMPORT_OK
import com.simplemobiletools.contacts.pro.helpers.VcfImporter.ImportResult.IMPORT_PARTIAL
import ezvcard.Ezvcard
import ezvcard.VCard
import ezvcard.parameter.RelatedType
import ezvcard.util.PartialDate
import java.io.File
import java.io.FileOutputStream
import java.net.URLDecoder
import java.util.*

class VcfImporter(val activity: SimpleActivity) {
    enum class ImportResult {
        IMPORT_FAIL, IMPORT_OK, IMPORT_PARTIAL
    }

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
                var photoUri = ""

                val phoneNumbers = ArrayList<PhoneNumber>()
                ezContact.telephoneNumbers.forEach {
                    val number = it.text
                    val type = getPhoneNumberTypeId(it.types.firstOrNull()?.value ?: MOBILE, it.types.getOrNull(1)?.value)
                    val label = if (type == Phone.TYPE_CUSTOM) {
                        it.types.firstOrNull()?.value ?: ""
                    } else {
                        ""
                    }
                    val preferred = getPreferredValue(it.types.lastOrNull()?.value) == 1

                    phoneNumbers.add(PhoneNumber(number, type, label, number.normalizePhoneNumber(), preferred))
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

                    if (email.isNotEmpty()) {
                        emails.add(Email(email, type, label))
                    }
                }

                val addresses = ArrayList<Address>()
                ezContact.addresses.forEach {
                    var address = it.streetAddress ?: ""
                    val type = getAddressTypeId(it.types.firstOrNull()?.value ?: HOME)
                    val label = if (type == StructuredPostal.TYPE_CUSTOM) {
                        it.types.firstOrNull()?.value ?: ""
                    } else {
                        ""
                    }

                    if (it.locality?.isNotEmpty() == true) {
                        address += " ${it.locality} "
                    }

                    if (it.region?.isNotEmpty() == true) {
                        if (address.isNotEmpty()) {
                            address = "${address.trim()}, "
                        }
                        address += "${it.region} "
                    }

                    if (it.postalCode?.isNotEmpty() == true) {
                        address += "${it.postalCode} "
                    }

                    if (it.country?.isNotEmpty() == true) {
                        address += "${it.country} "
                    }

                    address = address.trim()

                    if (address.isNotEmpty()) {
                        addresses.add(Address(address, type, label))
                    }
                }

                val events = ArrayList<Event>()
                ezContact.anniversaries.forEach { anniversary ->
                    val event = if (anniversary.date != null) {
                        Event(formatDateToDayCode(anniversary.date), CommonDataKinds.Event.TYPE_ANNIVERSARY)
                    } else {
                        Event(formatPartialDateToDayCode(anniversary.partialDate), CommonDataKinds.Event.TYPE_ANNIVERSARY)
                    }
                    events.add(event)
                }

                ezContact.birthdays.forEach { birthday ->
                    val event = if (birthday.date != null) {
                        Event(formatDateToDayCode(birthday.date), CommonDataKinds.Event.TYPE_BIRTHDAY)
                    } else {
                        Event(formatPartialDateToDayCode(birthday.partialDate), CommonDataKinds.Event.TYPE_BIRTHDAY)
                    }
                    events.add(event)
                }

                val starred = 0
                val contactId = 0
                val notes = ezContact.notes.firstOrNull()?.value ?: ""
                val groups = getContactGroups(ezContact)
                val company = ezContact.organization?.values?.firstOrNull() ?: ""
                val jobPosition = ezContact.titles?.firstOrNull()?.value ?: ""
                val organization = Organization(company, jobPosition)
                val websites = ezContact.urls.map { it.value } as ArrayList<String>
                val photoData = ezContact.photos.firstOrNull()?.data
                val photo = if (photoData != null) {
                    BitmapFactory.decodeByteArray(photoData, 0, photoData.size)
                } else {
                    null
                }

                val thumbnailUri = savePhoto(photoData)
                if (thumbnailUri.isNotEmpty()) {
                    photoUri = thumbnailUri
                }

                val ringtone = null

                val relations = ArrayList<ContactRelation>()
                ezContact.relations.forEach {
                    val name = it.text
                    val type = getRelationTypeId(it.types.firstOrNull())
                    if (name.isNotEmpty()) {
                        relations.add(ContactRelation(name, type, ""))
                    }
                }

                val IMs = ArrayList<IM>()
                ezContact.impps.forEach {
                    val typeString = it.uri.scheme
                    val value = URLDecoder.decode(it.uri.toString().substring(it.uri.scheme.length + 1), "UTF-8")
                    val type = when {
                        it.isAim -> Im.PROTOCOL_AIM
                        it.isYahoo -> Im.PROTOCOL_YAHOO
                        it.isMsn -> Im.PROTOCOL_MSN
                        it.isIcq -> Im.PROTOCOL_ICQ
                        it.isSkype -> Im.PROTOCOL_SKYPE
                        typeString == HANGOUTS -> Im.PROTOCOL_GOOGLE_TALK
                        typeString == QQ -> Im.PROTOCOL_QQ
                        typeString == JABBER -> Im.PROTOCOL_JABBER
                        else -> Im.PROTOCOL_CUSTOM
                    }

                    val label = if (type == Im.PROTOCOL_CUSTOM) URLDecoder.decode(typeString, "UTF-8") else ""
                    val IM = IM(value, type, label)
                    IMs.add(IM)
                }

                val contact = Contact(
                    0, prefix, firstName, middleName, surname, suffix, nickname, photoUri, phoneNumbers, emails, addresses, events,
                    targetContactSource, starred, contactId, thumbnailUri, photo, notes, groups, organization, websites, relations, IMs, DEFAULT_MIMETYPE, ringtone
                )

                // if there is no N and ORG fields at the given contact, only FN, treat it as an organization
                if (contact.getNameToDisplay().isEmpty() && contact.organization.isEmpty() && ezContact.formattedName?.value?.isNotEmpty() == true) {
                    contact.organization.company = ezContact.formattedName.value
                    contact.mimetype = CommonDataKinds.Organization.CONTENT_ITEM_TYPE
                }

                if (contact.isABusinessContact()) {
                    contact.mimetype = CommonDataKinds.Organization.CONTENT_ITEM_TYPE
                }

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
        val year = if (date.year == 0) "-" else "${1900 + date.year}"
        val month = String.format("%02d", date.month + 1)
        val day = String.format("%02d", date.date)
        return "$year-$month-$day"
    }

    private fun formatPartialDateToDayCode(partialDate: PartialDate): String {
        val month = String.format("%02d", partialDate.month)
        val day = String.format("%02d", partialDate.date)
        return "--$month-$day"
    }

    private fun getContactGroups(ezContact: VCard): ArrayList<Group> {
        val groups = ArrayList<Group>()
        if (ezContact.categories != null) {
            val groupNames = ezContact.categories.values

            if (groupNames != null) {
                val storedGroups = ContactsHelper(activity).getStoredGroupsSync()

                groupNames.forEach {
                    val groupName = it
                    val storedGroup = storedGroups.firstOrNull { it.title == groupName }

                    if (storedGroup != null) {
                        groups.add(storedGroup)
                    } else {
                        val newGroup = Group(null, groupName)
                        val id = activity.groupsDB.insertOrUpdate(newGroup)
                        newGroup.id = id
                        groups.add(newGroup)
                    }
                }
            }
        }
        return groups
    }

    private fun getPhoneNumberTypeId(type: String, subtype: String?) = when (type.toUpperCase()) {
        CELL -> Phone.TYPE_MOBILE
        HOME -> {
            if (subtype?.toUpperCase() == FAX) {
                Phone.TYPE_FAX_HOME
            } else {
                Phone.TYPE_HOME
            }
        }
        WORK -> {
            if (subtype?.toUpperCase() == FAX) {
                Phone.TYPE_FAX_WORK
            } else {
                Phone.TYPE_WORK
            }
        }
        MAIN -> Phone.TYPE_MAIN
        WORK_FAX -> Phone.TYPE_FAX_WORK
        HOME_FAX -> Phone.TYPE_FAX_HOME
        FAX -> Phone.TYPE_FAX_WORK
        PAGER -> Phone.TYPE_PAGER
        OTHER -> Phone.TYPE_OTHER
        else -> Phone.TYPE_CUSTOM
    }

    private fun getEmailTypeId(type: String) = when (type.toUpperCase()) {
        HOME -> CommonDataKinds.Email.TYPE_HOME
        WORK -> CommonDataKinds.Email.TYPE_WORK
        MOBILE -> CommonDataKinds.Email.TYPE_MOBILE
        OTHER -> CommonDataKinds.Email.TYPE_OTHER
        else -> CommonDataKinds.Email.TYPE_CUSTOM
    }

    private fun getAddressTypeId(type: String): Int = when (type.toUpperCase()) {
        HOME -> StructuredPostal.TYPE_HOME
        WORK -> StructuredPostal.TYPE_WORK
        OTHER -> StructuredPostal.TYPE_OTHER
        else -> StructuredPostal.TYPE_CUSTOM
    }

    private fun getRelationTypeId(type: RelatedType?): Int {
        if (type == null) {
            return (ContactRelation.TYPE_CONTACT)
        } else {
            return when (type) {
                RelatedType.ACQUAINTANCE -> ContactRelation.TYPE_ACQUAINTANCE
                RelatedType.AGENT -> ContactRelation.TYPE_AGENT
                RelatedType.CHILD -> ContactRelation.TYPE_CHILD
                RelatedType.CO_RESIDENT -> ContactRelation.TYPE_CO_RESIDENT
                RelatedType.CO_WORKER -> ContactRelation.TYPE_CO_WORKER
                RelatedType.COLLEAGUE -> ContactRelation.TYPE_COLLEAGUE
                RelatedType.CONTACT -> ContactRelation.TYPE_CONTACT
                RelatedType.CRUSH -> ContactRelation.TYPE_CRUSH
                RelatedType.DATE -> ContactRelation.TYPE_DATE
                RelatedType.EMERGENCY -> ContactRelation.TYPE_EMERGENCY
                RelatedType.FRIEND -> ContactRelation.TYPE_FRIEND
                RelatedType.ME -> ContactRelation.TYPE_ME
                RelatedType.MET -> ContactRelation.TYPE_MET
                RelatedType.MUSE -> ContactRelation.TYPE_MUSE
                RelatedType.NEIGHBOR -> ContactRelation.TYPE_NEIGHBOR
                RelatedType.PARENT -> ContactRelation.TYPE_PARENT
                RelatedType.SIBLING -> ContactRelation.TYPE_SIBLING
                RelatedType.SPOUSE -> ContactRelation.TYPE_SPOUSE
                RelatedType.SWEETHEART -> ContactRelation.TYPE_SWEETHEART
                RelatedType.PARENT -> ContactRelation.TYPE_PARENT
                else -> ContactRelation.TYPE_CONTACT
            }
        }
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

    private fun getPreferredValue(type: String?): Int {
        if (type != null) {
            if (type.startsWith("$PREF=".lowercase())) {
                return type.split("=")[1].toInt()
            }
        }

        return -1
    }
}
