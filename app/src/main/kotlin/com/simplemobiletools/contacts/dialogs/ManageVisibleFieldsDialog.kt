package com.simplemobiletools.contacts.dialogs

import android.support.v7.app.AlertDialog
import com.simplemobiletools.commons.activities.BaseSimpleActivity
import com.simplemobiletools.commons.extensions.setupDialogStuff
import com.simplemobiletools.commons.views.MyAppCompatCheckbox
import com.simplemobiletools.contacts.R
import com.simplemobiletools.contacts.extensions.config
import com.simplemobiletools.contacts.helpers.*

class ManageVisibleFieldsDialog(val activity: BaseSimpleActivity) {
    private var view = activity.layoutInflater.inflate(R.layout.dialog_manage_visible_fields, null)
    private val fields = LinkedHashMap<Int, Int>()

    init {
        fields.apply {
            put(SHOW_PREFIX_FIELD, R.id.manage_visible_fields_prefix)
            put(SHOW_FIRST_NAME_FIELD, R.id.manage_visible_fields_first_name)
            put(SHOW_MIDDLE_NAME_FIELD, R.id.manage_visible_fields_middle_name)
            put(SHOW_SURNAME_FIELD, R.id.manage_visible_fields_surname)
            put(SHOW_SUFFIX_FIELD, R.id.manage_visible_fields_suffix)
            put(SHOW_PHONE_NUMBERS_FIELD, R.id.manage_visible_fields_phone_numbers)
            put(SHOW_EMAILS_FIELD, R.id.manage_visible_fields_emails)
            put(SHOW_ADDRESSES_FIELD, R.id.manage_visible_fields_addresses)
            put(SHOW_STRUCTURED_ADDRESSES_FIELD, R.id.manage_visible_fields_structured_addresses)
            put(SHOW_EVENTS_FIELD, R.id.manage_visible_fields_events)
            put(SHOW_NOTES_FIELD, R.id.manage_visible_fields_notes)
            put(SHOW_ORGANIZATION_FIELD, R.id.manage_visible_fields_organization)
            put(SHOW_WEBSITES_FIELD, R.id.manage_visible_fields_websites)
            put(SHOW_GROUPS_FIELD, R.id.manage_visible_fields_groups)
            put(SHOW_CONTACT_SOURCE_FIELD, R.id.manage_visible_fields_contact_source)
        }

        val showContactFields = activity.config.showContactFields
        for ((key, value) in fields) {
            view.findViewById<MyAppCompatCheckbox>(value).isChecked = showContactFields and key != 0
        }

        AlertDialog.Builder(activity)
                .setPositiveButton(R.string.ok, { dialog, which -> dialogConfirmed() })
                .setNegativeButton(R.string.cancel, null)
                .create().apply {
                    activity.setupDialogStuff(view, this)
                }
    }

    private fun dialogConfirmed() {
        var result = 0
        for ((key, value) in fields) {
            if (view.findViewById<MyAppCompatCheckbox>(value).isChecked) {
                result += key
            }
        }

        activity.config.showContactFields = result
    }
}
