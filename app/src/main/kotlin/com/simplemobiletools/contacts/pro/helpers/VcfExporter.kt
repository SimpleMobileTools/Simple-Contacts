package com.simplemobiletools.contacts.pro.helpers

import android.net.Uri
import android.provider.ContactsContract.CommonDataKinds
import android.provider.ContactsContract.CommonDataKinds.*
import android.provider.MediaStore
import com.simplemobiletools.commons.activities.BaseSimpleActivity
import com.simplemobiletools.commons.extensions.showErrorToast
import com.simplemobiletools.commons.extensions.toast
import com.simplemobiletools.contacts.pro.R
import com.simplemobiletools.contacts.pro.extensions.getByteArray
import com.simplemobiletools.contacts.pro.extensions.getDateTimeFromDateString
import com.simplemobiletools.contacts.pro.helpers.VcfExporter.ExportResult.EXPORT_FAIL
import com.simplemobiletools.contacts.pro.models.Contact
import ezvcard.Ezvcard
import ezvcard.VCard
import ezvcard.parameter.ImageType
import ezvcard.property.*
import ezvcard.property.Email
import ezvcard.property.Organization
import ezvcard.property.Photo
import ezvcard.property.StructuredName
import ezvcard.util.PartialDate
import java.io.OutputStream
import java.util.*

class VcfExporter {
    enum class ExportResult {
        EXPORT_FAIL, EXPORT_OK, EXPORT_PARTIAL
    }

    private var contactsExported = 0
    private var contactsFailed = 0

    fun exportContacts(activity: BaseSimpleActivity, outputStream: OutputStream?, contacts: ArrayList<Contact>, showExportingToast: Boolean, callback: (result: ExportResult) -> Unit) {
        try {
            if (outputStream == null) {
                callback(EXPORT_FAIL)
                return
            }

            if (showExportingToast) {
                activity.toast(R.string.exporting)
            }

            val cards = ArrayList<VCard>()
            for (contact in contacts) {
                val card = VCard()
                StructuredName().apply {
                    prefixes.add(contact.prefix)
                    given = contact.firstName
                    additionalNames.add(contact.middleName)
                    family = contact.surname
                    suffixes.add(contact.suffix)
                    card.structuredName = this
                }

                if (contact.nickname.isNotEmpty()) {
                    card.setNickname(contact.nickname)
                }

                contact.phoneNumbers.forEach {
                    val phoneNumber = Telephone(it.value)
                    phoneNumber.parameters.addType(getPhoneNumberTypeLabel(it.type, it.label))
                    card.addTelephoneNumber(phoneNumber)
                }

                contact.emails.forEach {
                    val email = Email(it.value)
                    email.parameters.addType(getEmailTypeLabel(it.type, it.label))
                    card.addEmail(email)
                }

                contact.events.forEach {
                    if (it.type == Event.TYPE_ANNIVERSARY || it.type == Event.TYPE_BIRTHDAY) {
                        val dateTime = it.value.getDateTimeFromDateString()
                        if (it.value.startsWith("--")) {
                            val partialDate = PartialDate.builder().year(null).month(dateTime.monthOfYear).date(dateTime.dayOfMonth).build()
                            if (it.type == Event.TYPE_BIRTHDAY) {
                                card.birthdays.add(Birthday(partialDate))
                            } else {
                                card.anniversaries.add(Anniversary(partialDate))
                            }
                        } else {
                            Calendar.getInstance().apply {
                                clear()
                                set(Calendar.YEAR, dateTime.year)
                                set(Calendar.MONTH, dateTime.monthOfYear - 1)
                                set(Calendar.DAY_OF_MONTH, dateTime.dayOfMonth)
                                if (it.type == Event.TYPE_BIRTHDAY) {
                                    card.birthdays.add(Birthday(time))
                                } else {
                                    card.anniversaries.add(Anniversary(time))
                                }
                            }
                        }
                    }
                }

                contact.addresses.forEach {
                    val address = Address()
                    address.streetAddress = it.value
                    address.parameters.addType(getAddressTypeLabel(it.type, it.label))
                    card.addAddress(address)
                }

                contact.IMs.forEach {
                    val impp = when (it.type) {
                        Im.PROTOCOL_AIM -> Impp.aim(it.value)
                        Im.PROTOCOL_YAHOO -> Impp.yahoo(it.value)
                        Im.PROTOCOL_MSN -> Impp.msn(it.value)
                        Im.PROTOCOL_ICQ -> Impp.icq(it.value)
                        Im.PROTOCOL_SKYPE -> Impp.skype(it.value)
                        Im.PROTOCOL_GOOGLE_TALK -> Impp(HANGOUTS, it.value)
                        Im.PROTOCOL_QQ -> Impp(QQ, it.value)
                        Im.PROTOCOL_JABBER -> Impp(JABBER, it.value)
                        else -> Impp(it.label, it.value)
                    }

                    card.addImpp(impp)
                }

                if (contact.notes.isNotEmpty()) {
                    card.addNote(contact.notes)
                }

                if (contact.organization.isNotEmpty()) {
                    val organization = Organization()
                    organization.values.add(contact.organization.company)
                    card.organization = organization
                    card.titles.add(Title(contact.organization.jobPosition))
                }

                contact.websites.forEach {
                    card.addUrl(it)
                }

                if (contact.thumbnailUri.isNotEmpty()) {
                    val photoByteArray = MediaStore.Images.Media.getBitmap(activity.contentResolver, Uri.parse(contact.thumbnailUri)).getByteArray()
                    val photo = Photo(photoByteArray, ImageType.JPEG)
                    card.addPhoto(photo)
                }

                if (contact.groups.isNotEmpty()) {
                    val groupList = Categories()
                    contact.groups.forEach {
                        groupList.values.add(it.title)
                    }

                    card.categories = groupList
                }

                cards.add(card)
                contactsExported++
            }

            Ezvcard.write(cards).go(outputStream)
        } catch (e: Exception) {
            activity.showErrorToast(e)
        }

        callback(when {
            contactsExported == 0 -> EXPORT_FAIL
            contactsFailed > 0 -> ExportResult.EXPORT_PARTIAL
            else -> ExportResult.EXPORT_OK
        })
    }

    fun exportContact(activity: BaseSimpleActivity, contact: Contact, showExportingToast: Boolean): String? {
        try {

            if (showExportingToast) {
                activity.toast(R.string.exporting)
            }

            val strBuffer = StringBuffer("BEGIN:VCARD").append("\n").append("VERSION:4.0").append("\n")

            strBuffer.append("N:")

            if (contact.surname.isNotEmpty()) {
                strBuffer.append(contact.surname)
            }
            if (contact.firstName.isNotEmpty()) {
                strBuffer.append(";").append(contact.firstName)
            }
            if (contact.middleName.isNotEmpty()) {
                strBuffer.append(";").append(contact.middleName)
            }

            strBuffer.append("\n")

            if (contact.nickname.isNotEmpty()) {
                strBuffer.append("NICKNAME:").append(contact.nickname).append("\n")
            }

            contact.phoneNumbers.forEach {
                val phoneNumber = Telephone(it.value)
                phoneNumber.parameters.addType(getPhoneNumberTypeLabel(it.type, it.label))
                strBuffer.append("TEL:").append(phoneNumber.text).append("\n")
            }

            contact.emails.forEach {
                val email = Email(it.value)
                email.parameters.addType(getEmailTypeLabel(it.type, it.label))
                strBuffer.append("EMAIL;TYPE=INTERNET:").append(email.value).append("\n")
            }

            contact.addresses.forEach {
                val address = Address()
                address.streetAddress = it.value
                address.parameters.addType(getAddressTypeLabel(it.type, it.label))
                strBuffer.append("ADR:;;").append(address.streetAddress).append("\n")
            }

            if (contact.notes.isNotEmpty()) {
                strBuffer.append("NOTE:").append(contact.notes).append("\n")
            }

            if (contact.organization.isNotEmpty()) {
                strBuffer.append("ORG:").append(contact.organization.company).append("\n")
            }

            contact.websites.forEach {
                strBuffer.append("URL:").append(it).append("\n")
            }

            strBuffer.append("END:VCARD")

            return strBuffer.toString()
        } catch (e: Exception) {
            return null
            activity.showErrorToast(e)
        }
    }

    private fun getPhoneNumberTypeLabel(type: Int, label: String) = when (type) {
        Phone.TYPE_MOBILE -> CELL
        Phone.TYPE_HOME -> HOME
        Phone.TYPE_WORK -> WORK
        Phone.TYPE_MAIN -> PREF
        Phone.TYPE_FAX_WORK -> WORK_FAX
        Phone.TYPE_FAX_HOME -> HOME_FAX
        Phone.TYPE_PAGER -> PAGER
        Phone.TYPE_OTHER -> OTHER
        else -> label
    }

    private fun getEmailTypeLabel(type: Int, label: String) = when (type) {
        CommonDataKinds.Email.TYPE_HOME -> HOME
        CommonDataKinds.Email.TYPE_WORK -> WORK
        CommonDataKinds.Email.TYPE_MOBILE -> MOBILE
        CommonDataKinds.Email.TYPE_OTHER -> OTHER
        else -> label
    }

    private fun getAddressTypeLabel(type: Int, label: String) = when (type) {
        StructuredPostal.TYPE_HOME -> HOME
        StructuredPostal.TYPE_WORK -> WORK
        StructuredPostal.TYPE_OTHER -> OTHER
        else -> label
    }
}
