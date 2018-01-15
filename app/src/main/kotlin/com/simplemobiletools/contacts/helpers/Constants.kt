package com.simplemobiletools.contacts.helpers

import android.provider.ContactsContract.CommonDataKinds

// shared prefs
val CALL_CONTACT_ON_CLICK = "call_contact_on_click"
val SHOW_PHONE_NUMBERS = "show_phone_numbers"
val DISPLAY_CONTACT_SOURCES = "display_contact_sources"
val START_NAME_WITH_SURNAME = "start_name_with_surname"
val LAST_USED_CONTACT_SOURCE = "last_used_contact_source"
val LAST_USED_VIEW_PAGER_PAGE = "last_used_view_pager_page"

val CONTACT_ID = "contact_id"

// contact photo changes
val PHOTO_ADDED = 1
val PHOTO_REMOVED = 2
val PHOTO_CHANGED = 3
val PHOTO_UNCHANGED = 4

// default contact values
val DEFAULT_EMAIL_TYPE = CommonDataKinds.Email.TYPE_HOME
val DEFAULT_PHONE_NUMBER_TYPE = CommonDataKinds.Phone.TYPE_MOBILE
val DEFAULT_EVENT_TYPE = CommonDataKinds.Event.TYPE_BIRTHDAY

// export/import
val BEGIN_VCARD = "BEGIN:VCARD"
val END_VCARD = "END:VCARD"
val N = "N:"
val TEL = "TEL"
val BDAY = "BDAY:"
val ANNIVERSARY = "ANNIVERSARY:"
val PHOTO = "PHOTO"
val EMAIL = "EMAIL"
val BASE64 = "BASE64"

// phone number/email types
val CELL = "CELL"
val WORK = "WORK"
val HOME = "HOME"
val PREF = "PREF"
val MAIN = "MAIN"
val FAX = "FAX"
val WORK_FAX = "WORK;FAX"
val HOME_FAX = "HOME;FAX"
val PAGER = "PAGER"
val MOBILE = "MOBILE"
