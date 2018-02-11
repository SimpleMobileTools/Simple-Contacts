package com.simplemobiletools.contacts.helpers

import android.provider.ContactsContract.CommonDataKinds

// shared prefs
const val SHOW_CONTACT_THUMBNAILS = "show_contact_thumbnails"
const val SHOW_PHONE_NUMBERS = "show_phone_numbers"
const val DISPLAY_CONTACT_SOURCES = "display_contact_sources"
const val START_NAME_WITH_SURNAME = "start_name_with_surname"
const val LAST_USED_CONTACT_SOURCE = "last_used_contact_source"
const val LAST_USED_VIEW_PAGER_PAGE = "last_used_view_pager_page"
const val LOCAL_ACCOUNT_NAME = "local_account_name"
const val LOCAL_ACCOUNT_TYPE = "local_account_type"
const val ON_CONTACT_CLICK = "on_contact_click"

const val CONTACT_ID = "contact_id"
const val SMT_PRIVATE = "smt_private"   // used at the contact source of local contacts hidden from other apps
const val IS_PRIVATE = "is_private"

// contact photo changes
const val PHOTO_ADDED = 1
const val PHOTO_REMOVED = 2
const val PHOTO_CHANGED = 3
const val PHOTO_UNCHANGED = 4

// default contact values
const val DEFAULT_EMAIL_TYPE = CommonDataKinds.Email.TYPE_HOME
const val DEFAULT_PHONE_NUMBER_TYPE = CommonDataKinds.Phone.TYPE_MOBILE
const val DEFAULT_EVENT_TYPE = CommonDataKinds.Event.TYPE_BIRTHDAY

// export/import
const val BEGIN_VCARD = "BEGIN:VCARD"
const val END_VCARD = "END:VCARD"
const val N = "N"
const val TEL = "TEL"
const val BDAY = "BDAY:"
const val ANNIVERSARY = "ANNIVERSARY:"
const val PHOTO = "PHOTO"
const val EMAIL = "EMAIL"
const val ENCODING = "ENCODING"
const val BASE64 = "BASE64"
const val JPEG = "JPEG"
const val VERSION_2_1 = "VERSION:2.1"

// phone number/email types
const val CELL = "CELL"
const val WORK = "WORK"
const val HOME = "HOME"
const val PREF = "PREF"
const val MAIN = "MAIN"
const val FAX = "FAX"
const val WORK_FAX = "WORK;FAX"
const val HOME_FAX = "HOME;FAX"
const val PAGER = "PAGER"
const val MOBILE = "MOBILE"
const val VOICE = "VOICE"

const val ON_CLICK_CALL_CONTACT = 1
const val ON_CLICK_VIEW_CONTACT = 2
const val ON_CLICK_EDIT_CONTACT = 3
