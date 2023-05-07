package com.simplemobiletools.contacts.pro.helpers

import com.simplemobiletools.commons.helpers.TAB_CONTACTS
import com.simplemobiletools.commons.helpers.TAB_FAVORITES
import com.simplemobiletools.commons.helpers.TAB_GROUPS

const val PREF_USE_FAMILY_NAME_FOR_PLACEHOLDER_ICON = "use_family_name_placeholder"
const val PREF_MAIN_ACTIVITY_SHOW_STRUCTURED_NAME = "listview_show_structured_name"
const val PREF_MAIN_ACTIVITY_LIST_NAME_FORMAT = "listview_name_format"
const val PREF_MAIN_ACTIVITY_LIST_SORT_BY = "listview_sort_by"
const val PREF_MAIN_ACTIVITY_LIST_INVERSE_SORT = "listview_inverse_sort"
const val PREF_CONTACT_ACTIVITY_SHOW_STRUCTURED_NAME = "contact_show_structured_name"
const val PREF_CONTACT_ACTIVITY_NAME_FORMAT = "contact_name_format"
const val PREF_AUTO_FORMATTED_NAME_FORMAT = "auto_formatted_name_format"
const val PREF_CONTACT_ACTIVITY_EXPORT_SELECTED_FIELDS_ONLY = "export_selected_fields_only"
const val PREF_EDIT_ACTIVITY_PERMIT_CUSTOM_EVENT_TYPES = "permit_custom_event_types"
const val PREF_EDIT_ACTIVITY_SHOW_REMOVE_BUTTONS = "show_remove_buttons"
const val PREF_EDIT_ACTIVITY_SHOW_UPDATE_FORMATTED_ADDRESS_BUTTON = "show_update_address_button"
const val PREF_EDIT_ACTIVITY_ALWAYS_SHOW_NONEMPTY_FIELDS = "always_show_non_empty_fields"

const val GROUP = "group"
const val IS_FROM_SIMPLE_CONTACTS = "is_from_simple_contacts"
const val ADD_NEW_CONTACT_NUMBER = "add_new_contact_number"
const val FIRST_CONTACT_ID = 1000000
const val FIRST_GROUP_ID = 10000L
const val DEFAULT_FILE_NAME = "contacts.vcf"
const val AVOID_CHANGING_TEXT_TAG = "avoid_changing_text_tag"
const val AVOID_CHANGING_VISIBILITY_TAG = "avoid_changing_visibility_tag"

// extras used at third party intents
const val KEY_NAME = "name"
const val KEY_EMAIL = "email"
const val KEY_MAILTO = "mailto"

const val LOCATION_CONTACTS_TAB = 0
const val LOCATION_FAVORITES_TAB = 1
const val LOCATION_GROUP_CONTACTS = 2
const val LOCATION_INSERT_OR_EDIT = 3

val tabsList = arrayListOf(
    TAB_CONTACTS,
    TAB_FAVORITES,
    TAB_GROUPS
)
const val ALL_TABS_MASK = TAB_CONTACTS or TAB_FAVORITES or TAB_GROUPS

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

const val WHATSAPP = "whatsapp"
const val SIGNAL = "signal"
const val VIBER = "viber"
const val TELEGRAM = "telegram"
const val THREEMA = "threema"
