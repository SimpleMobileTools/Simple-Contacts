package com.simplemobiletools.contacts.dialogs

import android.support.v7.app.AlertDialog
import com.simplemobiletools.commons.extensions.setupDialogStuff
import com.simplemobiletools.contacts.R
import com.simplemobiletools.contacts.activities.SimpleActivity
import com.simplemobiletools.contacts.adapters.FilterContactSourcesAdapter
import com.simplemobiletools.contacts.extensions.config
import com.simplemobiletools.contacts.helpers.ContactsHelper
import kotlinx.android.synthetic.main.dialog_filter_contact_sources.view.*

class FilterContactSourcesDialog(val activity: SimpleActivity, val callback: () -> Unit) {
    var dialog: AlertDialog
    val view = activity.layoutInflater.inflate(R.layout.dialog_filter_contact_sources, null)

    init {
        ContactsHelper(activity).getContactSources {
            val selectedSources = activity.config.displayContactSources
            activity.runOnUiThread {
                view.filter_contact_sources_list.adapter = FilterContactSourcesAdapter(activity, it, selectedSources)
            }
        }

        dialog = AlertDialog.Builder(activity)
                .setPositiveButton(R.string.ok, { dialogInterface, i -> confirmEventTypes() })
                .setNegativeButton(R.string.cancel, null)
                .create().apply {
            activity.setupDialogStuff(view, this)
        }
    }

    private fun confirmEventTypes() {
        val selectedItems = (view.filter_contact_sources_list.adapter as FilterContactSourcesAdapter).getSelectedItemsSet()
        if (activity.config.displayContactSources != selectedItems) {
            activity.config.displayContactSources = selectedItems
            callback()
        }
        dialog.dismiss()
    }
}
