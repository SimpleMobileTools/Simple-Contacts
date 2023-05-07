package com.simplemobiletools.contacts.pro.helpers

import android.content.Context
import com.simplemobiletools.commons.helpers.BaseConfig
import com.simplemobiletools.commons.helpers.SHOW_DISPLAYNAME_FIELD
import com.simplemobiletools.commons.helpers.SHOW_PREFIX_FIELD
import com.simplemobiletools.commons.helpers.SHOW_FIRST_NAME_FIELD
import com.simplemobiletools.commons.helpers.SHOW_MIDDLE_NAME_FIELD
import com.simplemobiletools.commons.helpers.SHOW_SURNAME_FIELD
import com.simplemobiletools.commons.helpers.SHOW_SUFFIX_FIELD
import com.simplemobiletools.commons.helpers.SHOW_NICKNAME_FIELD
import com.simplemobiletools.commons.helpers.SHOW_PHONETIC_NAME_FIELDS
import com.simplemobiletools.commons.helpers.SHOW_TABS
import com.simplemobiletools.commons.models.contacts.ContactNameSortBy
import com.simplemobiletools.commons.models.contacts.ContactNameFormat

class Config(context: Context) : BaseConfig(context) {
    companion object {
        fun newInstance(context: Context) = Config(context)
    }

    var showTabs: Int
        get() = prefs.getInt(SHOW_TABS, ALL_TABS_MASK)
        set(showTabs) = prefs.edit().putInt(SHOW_TABS, showTabs).apply()

    var useFamilyNameForPlaceholderIcon: Boolean  // Use the family name (rather than the given name) to create a placeholder icon if no photo is provided
        get() = prefs.getBoolean(PREF_USE_FAMILY_NAME_FOR_PLACEHOLDER_ICON, true)
        set(selected) = prefs.edit().putBoolean(PREF_USE_FAMILY_NAME_FOR_PLACEHOLDER_ICON, selected).apply()

    // contactListShowStructuredName and contactListNameFormat are used
    // together to define the way names are displayed in the SelectContactDialog.
    // Note: It is NOT possible to simply merge these two variables into a
    // single parameter that can be set to NAMEFORMAT_FORMATTED_NAME when
    // the formatted name should be shown and a suitable different format
    // when the displayed name should be created from the structured name.
    // While this approach looks tempting, it will fail if the formatted name
    // is requested but the formatted_name field of the name is empty. Just
    // displaying this empty string would not be a good choice and thus we
    // need a fallback solution based on the structured name. However if we
    // build a display from the structured name, we need to know the preferred
    // name format.
    var contactListShowFormattedName: Boolean
        get() = prefs.getBoolean(PREF_MAIN_ACTIVITY_SHOW_STRUCTURED_NAME, true)
        set(selected) = prefs.edit().putBoolean(PREF_MAIN_ACTIVITY_SHOW_STRUCTURED_NAME, selected).apply()

    var contactListNameFormat: ContactNameFormat  // Format for names in the SelectContactDialog list view
        get() {
            val formatOrdinal: Int = prefs.getInt(PREF_MAIN_ACTIVITY_LIST_NAME_FORMAT, ContactNameFormat.NAMEFORMAT_FAMILY_GIVEN.ordinal)
            return ContactNameFormat.values().getOrElse(formatOrdinal) { ContactNameFormat.NAMEFORMAT_FAMILY_GIVEN }
        }
        set(selected) = prefs.edit().putInt(PREF_MAIN_ACTIVITY_LIST_NAME_FORMAT, selected.ordinal).apply()

    var contactListSortBy: ContactNameSortBy // Sorting order for names in the SelectContactDialog list view
        get() {
            val formatOrdinal: Int = prefs.getInt(PREF_MAIN_ACTIVITY_LIST_SORT_BY, ContactNameSortBy.NAMESORTBY_FAMILY_NAME.ordinal)
            return ContactNameSortBy.values().getOrElse(formatOrdinal) { ContactNameSortBy.NAMESORTBY_FAMILY_NAME }
        }
        set(selected) = prefs.edit().putInt(PREF_MAIN_ACTIVITY_LIST_SORT_BY, selected.ordinal).apply()

    var contactListInverseSortOrder: Boolean   // Invert sorting order for names in the SelectContactDialog list view
        get() = prefs.getBoolean(PREF_MAIN_ACTIVITY_LIST_INVERSE_SORT, false)
        set(selected) = prefs.edit().putBoolean(PREF_MAIN_ACTIVITY_LIST_INVERSE_SORT, selected).apply()

    // See the comments at contactListShowFormattedName for an explanation
    // why there are two distinct variables for contactNameShowFormattedName
    // and contactNameFormat
    var contactNameShowFormattedName: Boolean
        get() = prefs.getBoolean(PREF_CONTACT_ACTIVITY_SHOW_STRUCTURED_NAME, true)
        set(selected) = prefs.edit().putBoolean(PREF_CONTACT_ACTIVITY_SHOW_STRUCTURED_NAME, selected).apply()

    var contactNameFormat: ContactNameFormat // Format for names in the ContactActivity
        get() {
            val formatOrdinal: Int = prefs.getInt(PREF_CONTACT_ACTIVITY_NAME_FORMAT, ContactNameFormat.NAMEFORMAT_FAMILY_GIVEN.ordinal)
            return ContactNameFormat.values().getOrElse(formatOrdinal) { ContactNameFormat.NAMEFORMAT_FAMILY_GIVEN }
        }
        set(selected) = prefs.edit().putInt(PREF_CONTACT_ACTIVITY_NAME_FORMAT, selected.ordinal).apply()

    var autoFormattedNameFormat: ContactNameFormat // Format used for automatically generating formatted names in the EditContactActivity
        get() {
            val formatOrdinal: Int = prefs.getInt(PREF_AUTO_FORMATTED_NAME_FORMAT, ContactNameFormat.NAMEFORMAT_FAMILY_GIVEN.ordinal)
            return ContactNameFormat.values().getOrElse(formatOrdinal) { ContactNameFormat.NAMEFORMAT_FAMILY_GIVEN }
        }
        set(selected) = prefs.edit().putInt(PREF_AUTO_FORMATTED_NAME_FORMAT, selected.ordinal).apply()

    var exportSelectedFieldsOnly: Boolean
        get() = prefs.getBoolean(PREF_CONTACT_ACTIVITY_EXPORT_SELECTED_FIELDS_ONLY, true)
        set(selected) = prefs.edit().putBoolean(PREF_CONTACT_ACTIVITY_EXPORT_SELECTED_FIELDS_ONLY, selected).apply()

    // Android's ContactContract.Event type knows about birthdays and
    // anniversaries, but also supports custom event types supported by
    // the user.
    // vCard (and CardDAV) only supports _one_ birthday and _one_ anniversary
    // per contact and no other event types.
    // This causes problems when synchronizing an Android contact with an
    // external CardDAV server (via DAVx5 or similar). This problem can be
    // mitigated by limiting the Android contact to one birthday and one
    // anniversary only, but this just enforces a limitation to users that
    // only use Android for their contact management.
    // Should this choice be given to the end user (in the setting dialog)
    // or should this decision be made by the App-developer?
    var permitCustomEventTypes: Boolean
        get() = prefs.getBoolean(PREF_EDIT_ACTIVITY_PERMIT_CUSTOM_EVENT_TYPES, true)
        set(selected) = prefs.edit().putBoolean(PREF_EDIT_ACTIVITY_PERMIT_CUSTOM_EVENT_TYPES, selected).apply()

    // How should we handle deleting items within a contact (e.g. an email
    // address no longer needed)
    // Approach A (traditional SimpleContact): The user removes the text of
    // the email address, and when the contact is save, the application detects
    // that the line is empty and does not store an empty entry (effectively
    // deleting it)
    // Approach B: Add a 'Delete' button to each entry. When the user clicks
    // on it the corresponding entry is deleted at once. This approach is
    // consistent with the 'Add' buttons that are present for each type of
    // items where multiples are permitted and is faster that deleting all
    // characters of text. However this also is more error-prone (one click
    // and the entire entry is gone (currently without 'Undo') and requires
    // screen space on each line.
    var showRemoveButtons: Boolean
        get() = prefs.getBoolean(PREF_EDIT_ACTIVITY_SHOW_REMOVE_BUTTONS, true)
        set(selected) = prefs.edit().putBoolean(PREF_EDIT_ACTIVITY_SHOW_REMOVE_BUTTONS, selected).apply()

    // If the formatted address of a contact matches the text that would be
    // created if a formatted addresses would be created from the structured
    // address (or when the formatted address is empty), then we shall
    // automatically update the formatted address every time that the
    // structured address is modified (and ONLY in this direction!
    // Manually editing the formatted address does NOT update the structured
    // address!).
    // If we want to recreate this link, we could either delete the complete
    // formatted address and then edit the structured address, or we could
    // press the 'Update-Formatted-Address' button, if such a button is supplied.
    // However such a button requires screen space and might be confusing to
    // the user. Should we really display it?
    // Currently (2023-05) this is just an internal setting. There is no
    // corresponding checkbox in the settings activity. Should we add one?
    var showUpdateFormattedAddressButton: Boolean
        get() = prefs.getBoolean(PREF_EDIT_ACTIVITY_SHOW_UPDATE_FORMATTED_ADDRESS_BUTTON, false)
        set(selected) = prefs.edit().putBoolean(PREF_EDIT_ACTIVITY_SHOW_UPDATE_FORMATTED_ADDRESS_BUTTON, selected).apply()

    // We have showContactFields to select which fields shall be displayed
    // in a ViewActivity or EditActivity. If fields that contain data are
    // not displayed in the edit activity, there might be stale and/or
    // confidential data that the user is unaware of when editing. Thus we
    // might to choose to force the display of data fields that actually
    // contain (non-null) data, even if corresponding field was disabled in
    // showContactFields. Obvioulsly this is a violation of the users intentions
    // (since he disable the field in showContactFields, however it might
    // still be desirable to show these fields anyhow.
    // We currently just force the editable display of filled name parts,
    // but not of other fields. Is this a clever choice?
    // Currently (2023-05) this is just an internal setting. There is no
    // corresponding checkbox in the settings activity. Should we add one?
    var alwaysShowNonEmptyContactFields: Int
        get() = prefs.getInt(PREF_EDIT_ACTIVITY_ALWAYS_SHOW_NONEMPTY_FIELDS,
            SHOW_DISPLAYNAME_FIELD or SHOW_PREFIX_FIELD or SHOW_FIRST_NAME_FIELD or
            SHOW_MIDDLE_NAME_FIELD or SHOW_SURNAME_FIELD or SHOW_SUFFIX_FIELD or
            SHOW_NICKNAME_FIELD or SHOW_PHONETIC_NAME_FIELDS)
        set(selected) = prefs.edit().putInt(PREF_EDIT_ACTIVITY_ALWAYS_SHOW_NONEMPTY_FIELDS, selected).apply()
}

