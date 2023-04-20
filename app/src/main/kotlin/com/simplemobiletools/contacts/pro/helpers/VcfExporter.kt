package com.simplemobiletools.commons.helpers
// WARNING - DO NOT USE THIS CODE - SEE Commons!!
import android.net.Uri
import android.provider.ContactsContract.CommonDataKinds
import android.provider.ContactsContract.CommonDataKinds.Event
import android.provider.ContactsContract.CommonDataKinds.Im
import android.provider.ContactsContract.CommonDataKinds.Phone
import android.provider.ContactsContract.CommonDataKinds.StructuredPostal
import android.provider.MediaStore
import com.simplemobiletools.commons.R
import com.simplemobiletools.commons.activities.BaseSimpleActivity
import com.simplemobiletools.commons.extensions.getByteArray
import com.simplemobiletools.commons.extensions.getDateTimeFromDateString
import com.simplemobiletools.commons.extensions.showErrorToast
import com.simplemobiletools.commons.extensions.toast
import com.simplemobiletools.commons.models.contacts.Contact
import com.simplemobiletools.commons.models.contacts.ContactRelation
import com.simplemobiletools.commons.models.contacts.IM
import ezvcard.Ezvcard
import ezvcard.VCard
import ezvcard.VCardVersion
import ezvcard.parameter.ImageType
import ezvcard.parameter.RelatedType
import ezvcard.property.*
import java.io.OutputStream
import java.util.*

class VcfExporter {
    enum class ExportResult {
        EXPORT_FAIL, EXPORT_OK, EXPORT_PARTIAL
    }

    private var contactsExported = 0
    private var contactsFailed = 0

    fun exportContacts(
        activity: BaseSimpleActivity,
        outputStream: OutputStream?,
        contacts: ArrayList<Contact>,
        showExportingToast: Boolean,
        callback: (result: ExportResult) -> Unit
    ) {
        try {
            if (outputStream == null) {
                callback(ExportResult.EXPORT_FAIL)
                return
            }

            if (showExportingToast) {
                activity.toast(R.string.exporting)
            }

            val cards = ArrayList<VCard>()
            for (contact in contacts) {
                val card = VCard()

                val formattedName = arrayOf(contact.name.prefix, contact.name.givenName, contact.name.middleName, contact.name.familyName, contact.name.suffix)
                    .filter { it.isNotEmpty() }
                    .joinToString(separator = " ")
                card.formattedName = FormattedName(formattedName)

                StructuredName().apply {
                    prefixes.add(contact.name.prefix)
                    given = contact.name.givenName
                    additionalNames.add(contact.name.middleName)
                    family = contact.name.familyName
                    suffixes.add(contact.name.suffix)
                    card.structuredName = this
                }

                val nicknames: Array<String> = contact.nicknames.map { it.name }.toTypedArray()
                card.setNickname(*nicknames)

                contact.phoneNumbers.forEach {
                    val phoneNumber = Telephone(it.value)
                    phoneNumber.parameters.addType(getPhoneNumberTypeLabel(it.type, it.label))
                    card.addTelephoneNumber(phoneNumber)
                }

                contact.emails.forEach {
                    val email = Email(it.address)
                    email.parameters.addType(getEmailTypeLabel(it.type, it.label))
                    card.addEmail(email)
                }

                contact.events.forEach { event ->
                    // Android contact support a general "Events", however SimpleContacts
                    // and vCard 4.0 only support birthdays and anniversaries. Thus we
                    // need to decide what to do with "TYPE_OTHER" and "TYPE_CUSTOM" events.
                    // We can either simply discard them here, or map them to anniversaries.
                    // We shall opt for the latter, since losing the event type is better
                    // that losing the complete event.
                    // if ((event.type == Event.TYPE_ANNIVERSARY) || (event.type == Event.TYPE_BIRTHDAY)) {
                    if (event.type == Event.TYPE_ANNIVERSARY || event.type == Event.TYPE_BIRTHDAY) {
                        val dateTime = event.startDate.getDateTimeFromDateString(false)
                        Calendar.getInstance().apply {
                            clear()
                            if (event.startDate.startsWith("--")) {
                                set(Calendar.YEAR, 1900)
                            } else {
                                set(Calendar.YEAR, dateTime.year)

                            }
                            set(Calendar.MONTH, dateTime.monthOfYear - 1)
                            set(Calendar.DAY_OF_MONTH, dateTime.dayOfMonth)
                            if (event.type == Event.TYPE_BIRTHDAY) {
                                card.birthdays.add(Birthday(time))
                            } else {
                                card.anniversaries.add(Anniversary(time))
                            }
                        }
                    }
                }

                contact.addresses.forEach {
                    val address = Address()
                    // Optionally, set the text to print on the mailing label (i.e. formatted address)
                    // address.label = it.value (or it.formattedAddress)
                    // address.label = null
                    address.streetAddress = it.street
                    address.poBox = it.postOfficeBox
                    address.extendedAddress = it.neighborhood
                    address.locality = it.city
                    address.region = it.region
                    address.postalCode = it.postalCode
                    address.country = it.country
                    address.parameters.addType(getAddressTypeLabel(it.type, it.label))
                    card.addAddress(address)
                }

                contact.IMs.forEach {
                    val impp = when (it.type) {
                        Im.PROTOCOL_AIM -> Impp.aim(it.data)
                        Im.PROTOCOL_YAHOO -> Impp.yahoo(it.data)
                        Im.PROTOCOL_MSN -> Impp.msn(it.data)
                        Im.PROTOCOL_ICQ -> Impp.icq(it.data)
                        Im.PROTOCOL_SKYPE -> Impp.skype(it.data)
                        Im.PROTOCOL_GOOGLE_TALK -> Impp(HANGOUTS, it.data)
                        Im.PROTOCOL_QQ -> Impp(QQ, it.data)
                        Im.PROTOCOL_JABBER -> Impp.xmpp(it.data)
                        IM.PROTOCOL_SIP -> Impp.sip(it.data)
                        IM.PROTOCOL_IRC -> Impp.irc(it.data)
                        else -> Impp(it.custom_protocol, it.data)
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
                    if (contact.organization.jobTitle.isNotEmpty())
                        card.titles.add(Title(contact.organization.jobTitle))
                    if (contact.organization.jobDescription.isNotEmpty())
                        card.roles.add(Role(contact.organization.jobDescription))
                    // card.logos.add(Logo(contact.organization.logo))
                }

                contact.websites.forEach {
                    var WebsiteURL: Url = ezvcard.property.Url(it.URL)
                    WebsiteURL.setType(getWebsiteTypeLabel(it.type, it.label))
                    card.addUrl(WebsiteURL)
                }

                contact.relations.forEach {
                    var name = it.name.trim()
                    if (name.isNotEmpty()) {
                        var related = ezvcard.property.Related()
                        related.text = name
                        related.types.add (
                            when (it.type) {
                                // vCard 4.0 relation types are directly mapped to their related type
                                ContactRelation.TYPE_CONTACT -> RelatedType.CONTACT
                                ContactRelation.TYPE_ACQUAINTANCE ->  RelatedType.ACQUAINTANCE
                                ContactRelation.TYPE_FRIEND ->  RelatedType.FRIEND
                                ContactRelation.TYPE_MET ->  RelatedType.MET
                                ContactRelation.TYPE_CO_WORKER ->  RelatedType.CO_WORKER
                                ContactRelation.TYPE_COLLEAGUE ->  RelatedType.COLLEAGUE
                                ContactRelation.TYPE_CO_RESIDENT ->  RelatedType.CO_RESIDENT
                                ContactRelation.TYPE_NEIGHBOR ->  RelatedType.NEIGHBOR
                                ContactRelation.TYPE_CHILD ->  RelatedType.CHILD
                                ContactRelation.TYPE_PARENT ->  RelatedType.PARENT
                                ContactRelation.TYPE_SIBLING ->  RelatedType.SIBLING
                                ContactRelation.TYPE_SPOUSE ->  RelatedType.SPOUSE
                                ContactRelation.TYPE_KIN ->  RelatedType.KIN
                                ContactRelation.TYPE_MUSE ->  RelatedType.MUSE
                                ContactRelation.TYPE_CRUSH ->  RelatedType.CRUSH
                                ContactRelation.TYPE_DATE ->  RelatedType.DATE
                                ContactRelation.TYPE_SWEETHEART ->  RelatedType.SWEETHEART
                                ContactRelation.TYPE_ME ->  RelatedType.ME
                                ContactRelation.TYPE_AGENT ->  RelatedType.AGENT
                                ContactRelation.TYPE_EMERGENCY ->  RelatedType.EMERGENCY

                                // Android relation types are mapped to a suitable substitute (with loss of precision!)
                                ContactRelation.TYPE_ASSISTANT -> RelatedType.COLLEAGUE
                                ContactRelation.TYPE_BROTHER -> RelatedType.SIBLING
                                ContactRelation.TYPE_DOMESTIC_PARTNER -> RelatedType.FRIEND
                                ContactRelation.TYPE_FATHER ->  RelatedType.PARENT
                                ContactRelation.TYPE_MANAGER -> RelatedType.COLLEAGUE
                                ContactRelation.TYPE_MOTHER ->  RelatedType.PARENT
                                ContactRelation.TYPE_PARTNER -> RelatedType.FRIEND
                                ContactRelation.TYPE_REFERRED_BY -> RelatedType.CONTACT
                                ContactRelation.TYPE_RELATIVE -> RelatedType.KIN
                                ContactRelation.TYPE_SISTER -> RelatedType.SIBLING

                                // Custom relation types are mapped to a suitable substitute (with loss of precision!)
                                ContactRelation.TYPE_SUPERIOR -> RelatedType.COLLEAGUE
                                ContactRelation.TYPE_SUBORDINATE -> RelatedType.COLLEAGUE

                                ContactRelation.TYPE_HUSBAND -> RelatedType.SPOUSE
                                ContactRelation.TYPE_WIFE -> RelatedType.SPOUSE
                                ContactRelation.TYPE_SON -> RelatedType.CHILD
                                ContactRelation.TYPE_DAUGHTER -> RelatedType.CHILD
                                ContactRelation.TYPE_GRANDPARENT -> RelatedType.KIN
                                ContactRelation.TYPE_GRANDFATHER -> RelatedType.KIN
                                ContactRelation.TYPE_GRANDMOTHER -> RelatedType.KIN
                                ContactRelation.TYPE_GRANDCHILD -> RelatedType.KIN
                                ContactRelation.TYPE_GRANDSON -> RelatedType.KIN
                                ContactRelation.TYPE_GRANDDAUGHTER -> RelatedType.KIN
                                ContactRelation.TYPE_UNCLE -> RelatedType.KIN
                                ContactRelation.TYPE_AUNT -> RelatedType.KIN
                                ContactRelation.TYPE_NEPHEW -> RelatedType.KIN
                                ContactRelation.TYPE_NIECE -> RelatedType.KIN
                                ContactRelation.TYPE_FATHER_IN_LAW -> RelatedType.KIN
                                ContactRelation.TYPE_MOTHER_IN_LAW -> RelatedType.KIN
                                ContactRelation.TYPE_SON_IN_LAW -> RelatedType.KIN
                                ContactRelation.TYPE_DAUGHTER_IN_LAW -> RelatedType.KIN
                                ContactRelation.TYPE_BROTHER_IN_LAW -> RelatedType.KIN
                                ContactRelation.TYPE_SISTER_IN_LAW -> RelatedType.KIN
                                else -> RelatedType.CONTACT
                            }
                        )
                        card.addRelated(related)
                    }
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

            Ezvcard.write(cards).version(VCardVersion.V4_0).go(outputStream)
        } catch (e: Exception) {
            activity.showErrorToast(e)
        }

        callback(
            when {
                contactsExported == 0 -> ExportResult.EXPORT_FAIL
                contactsFailed > 0 -> ExportResult.EXPORT_PARTIAL
                else -> ExportResult.EXPORT_OK
            }
        )
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

    private fun getWebsiteTypeLabel(type: Int, label: String): String = when (type) {
        CommonDataKinds.Website.TYPE_HOMEPAGE -> HOME
        CommonDataKinds.Website.TYPE_BLOG     -> HOME
        CommonDataKinds.Website.TYPE_PROFILE  -> HOME
        CommonDataKinds.Website.TYPE_HOME -> HOME
        CommonDataKinds.Website.TYPE_WORK -> WORK
        CommonDataKinds.Website.TYPE_FTP ->  HOME
        CommonDataKinds.Website.TYPE_OTHER ->  OTHER
        else -> label
    }
}
