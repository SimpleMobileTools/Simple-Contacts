package com.simplemobiletools.contacts.pro.helpers

import android.provider.ContactsContract.CommonDataKinds
import android.telephony.PhoneNumberUtils
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import com.simplemobiletools.commons.extensions.normalizeString
import com.simplemobiletools.contacts.pro.models.LocalContact

// shared prefs
const val SHOW_CONTACT_THUMBNAILS = "show_contact_thumbnails"
const val SHOW_PHONE_NUMBERS = "show_phone_numbers"
const val SHOW_ONLY_CONTACTS_WITH_NUMBERS = "show_only_contacts_with_numbers"
const val IGNORED_CONTACT_SOURCES = "ignored_contact_sources_2"
const val START_NAME_WITH_SURNAME = "start_name_with_surname"
const val LAST_USED_CONTACT_SOURCE = "last_used_contact_source"
const val ON_CONTACT_CLICK = "on_contact_click"
const val SHOW_CONTACT_FIELDS = "show_contact_fields"
const val SHOW_TABS = "show_tabs"
const val SHOW_CALL_CONFIRMATION = "show_call_confirmation"
const val SHOW_DIALPAD_BUTTON = "show_dialpad_button"
const val SHOW_DIALPAD_LETTERS = "show_dialpad_letters"

const val CONTACT_ID = "contact_id"
const val SMT_PRIVATE = "smt_private"   // used at the contact source of local contacts hidden from other apps
const val IS_PRIVATE = "is_private"
const val GROUP = "group"
const val IS_FROM_SIMPLE_CONTACTS = "is_from_simple_contacts"
const val ADD_NEW_CONTACT_NUMBER = "add_new_contact_number"
const val FIRST_CONTACT_ID = 1000000
const val FIRST_GROUP_ID = 10000L
const val REQUEST_CODE_SET_DEFAULT_DIALER = 1

// extras used at third party intents
const val KEY_PHONE = "phone"
const val KEY_NAME = "name"
const val KEY_EMAIL = "email"
const val KEY_MAILTO = "mailto"

const val LOCATION_CONTACTS_TAB = 0
const val LOCATION_FAVORITES_TAB = 1
const val LOCATION_GROUP_CONTACTS = 2
const val LOCATION_DIALPAD = 3
const val LOCATION_INSERT_OR_EDIT = 4

const val CONTACTS_TAB_MASK = 1
const val FAVORITES_TAB_MASK = 2
const val GROUPS_TAB_MASK = 8
const val ALL_TABS_MASK = CONTACTS_TAB_MASK or FAVORITES_TAB_MASK or GROUPS_TAB_MASK

val tabsList = arrayListOf(CONTACTS_TAB_MASK,
        FAVORITES_TAB_MASK,
        GROUPS_TAB_MASK
)

// contact photo changes
const val PHOTO_ADDED = 1
const val PHOTO_REMOVED = 2
const val PHOTO_CHANGED = 3
const val PHOTO_UNCHANGED = 4

// phone number/email types
const val CELL = "CELL"
const val WORK = "WORK"
const val HOME = "HOME"
const val OTHER = "OTHER"
const val PREF = "PREF"
const val MAIN = "MAIN"
const val FAX = "FAX"
const val WORK_FAX = "WORK;FAX"
const val HOME_FAX = "HOME;FAX"
const val PAGER = "PAGER"
const val MOBILE = "MOBILE"

// IMs not supported by Ez-vcard
const val HANGOUTS = "Hangouts"
const val QQ = "QQ"
const val JABBER = "Jabber"

const val ON_CLICK_CALL_CONTACT = 1
const val ON_CLICK_VIEW_CONTACT = 2
const val ON_CLICK_EDIT_CONTACT = 3

// visible fields filtering
const val SHOW_PREFIX_FIELD = 1
const val SHOW_FIRST_NAME_FIELD = 2
const val SHOW_MIDDLE_NAME_FIELD = 4
const val SHOW_SURNAME_FIELD = 8
const val SHOW_SUFFIX_FIELD = 16
const val SHOW_PHONE_NUMBERS_FIELD = 32
const val SHOW_EMAILS_FIELD = 64
const val SHOW_ADDRESSES_FIELD = 128
const val SHOW_EVENTS_FIELD = 256
const val SHOW_NOTES_FIELD = 512
const val SHOW_ORGANIZATION_FIELD = 1024
const val SHOW_GROUPS_FIELD = 2048
const val SHOW_CONTACT_SOURCE_FIELD = 4096
const val SHOW_WEBSITES_FIELD = 8192
const val SHOW_NICKNAME_FIELD = 16384
const val SHOW_IMS_FIELD = 32768

const val DEFAULT_EMAIL_TYPE = CommonDataKinds.Email.TYPE_HOME
const val DEFAULT_PHONE_NUMBER_TYPE = CommonDataKinds.Phone.TYPE_MOBILE
const val DEFAULT_ADDRESS_TYPE = CommonDataKinds.StructuredPostal.TYPE_HOME
const val DEFAULT_EVENT_TYPE = CommonDataKinds.Event.TYPE_BIRTHDAY
const val DEFAULT_ORGANIZATION_TYPE = CommonDataKinds.Organization.TYPE_WORK
const val DEFAULT_WEBSITE_TYPE = CommonDataKinds.Website.TYPE_HOMEPAGE
const val DEFAULT_IM_TYPE = CommonDataKinds.Im.PROTOCOL_SKYPE

// apps with special handling
const val TELEGRAM_PACKAGE = "org.telegram.messenger"
const val SIGNAL_PACKAGE = "org.thoughtcrime.securesms"
const val WHATSAPP_PACKAGE = "com.whatsapp"

fun getEmptyLocalContact() = LocalContact(0, "", "", "", "", "", "", null, ArrayList(), ArrayList(), ArrayList(), 0, ArrayList(), "", ArrayList(), "", "", ArrayList(), ArrayList())

fun getProperText(text: String, shouldNormalize: Boolean) = if (shouldNormalize) text.normalizeString() else text

fun highlightTextFromNumbers(name: String, textToHighlight: String, adjustedPrimaryColor: Int): SpannableString {
    val spannableString = SpannableString(name)
    val digits = PhoneNumberUtils.convertKeypadLettersToDigits(name)
    if (digits.contains(textToHighlight)) {
        val startIndex = digits.indexOf(textToHighlight, 0, true)
        val endIndex = Math.min(startIndex + textToHighlight.length, name.length)
        try {
            spannableString.setSpan(ForegroundColorSpan(adjustedPrimaryColor), startIndex, endIndex, Spannable.SPAN_EXCLUSIVE_INCLUSIVE)
        } catch (ignored: IndexOutOfBoundsException) {
        }
    }

    return spannableString
}
