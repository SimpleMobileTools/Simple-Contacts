package com.simplemobiletools.contacts.helpers

import android.provider.ContactsContract.CommonDataKinds

// shared prefs
const val SHOW_CONTACT_THUMBNAILS = "show_contact_thumbnails"
const val SHOW_PHONE_NUMBERS = "show_phone_numbers"
const val DISPLAY_CONTACT_SOURCES = "display_contact_sources"
const val START_NAME_WITH_SURNAME = "start_name_with_surname"
const val LAST_USED_CONTACT_SOURCE = "last_used_contact_source"
const val LOCAL_ACCOUNT_NAME = "local_account_name"
const val LOCAL_ACCOUNT_TYPE = "local_account_type"
const val ON_CONTACT_CLICK = "on_contact_click"
const val SHOW_CONTACT_FIELDS = "show_contact_fields"

const val CONTACT_ID = "contact_id"
const val SMT_PRIVATE = "smt_private"   // used at the contact source of local contacts hidden from other apps
const val IS_PRIVATE = "is_private"
const val GROUP = "group"
const val FIRST_GROUP_ID = 10000

const val LOCATION_CONTACTS_TAB = 0
const val LOCATION_FAVORITES_TAB = 1
const val LOCATION_GROUPS_TAB = 2
const val LOCATION_GROUP_CONTACTS = 3

const val CONTACTS_TAB_MASK = 1
const val FAVORITES_TAB_MASK = 2
const val GROUPS_TAB_MASK = 4
const val ALL_TABS_MASK = 7

// contact photo changes
const val PHOTO_ADDED = 1
const val PHOTO_REMOVED = 2
const val PHOTO_CHANGED = 3
const val PHOTO_UNCHANGED = 4

// export/import
const val BEGIN_VCARD = "BEGIN:VCARD"
const val END_VCARD = "END:VCARD"
const val N = "N"
const val TEL = "TEL"
const val BDAY = "BDAY:"
const val ANNIVERSARY = "ANNIVERSARY:"
const val PHOTO = "PHOTO"
const val EMAIL = "EMAIL"
const val ADR = "ADR"
const val NOTE = "NOTE:"
const val ORG = "ORG:"
const val TITLE = "TITLE:"
const val URL = "URL:"
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
const val SHOW_STRUCTURED_ADDRESSES_FIELD = 16384

const val DEFAULT_EMAIL_TYPE = CommonDataKinds.Email.TYPE_HOME
const val DEFAULT_PHONE_NUMBER_TYPE = CommonDataKinds.Phone.TYPE_MOBILE
const val DEFAULT_ADDRESS_TYPE = CommonDataKinds.StructuredPostal.TYPE_HOME
const val DEFAULT_EVENT_TYPE = CommonDataKinds.Event.TYPE_BIRTHDAY
const val DEFAULT_ORGANIZATION_TYPE = CommonDataKinds.Organization.TYPE_WORK
const val DEFAULT_WEBSITE_TYPE = CommonDataKinds.Website.TYPE_HOMEPAGE

// some manufacturer contact account types from https://stackoverflow.com/a/44802016/1967672
val localAccountTypes = arrayListOf("vnd.sec.contact.phone",
        "com.htc.android.pcsc",
        "com.sonyericsson.localcontacts",
        "com.lge.sync",
        "com.lge.phone",
        "vnd.tmobileus.contact.phone",
        "com.android.huawei.phone",
        "Local Phone Account"
)
