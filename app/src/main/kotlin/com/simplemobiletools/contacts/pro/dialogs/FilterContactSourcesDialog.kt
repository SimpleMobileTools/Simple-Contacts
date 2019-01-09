package com.simplemobiletools.contacts.pro.dialogs

import androidx.appcompat.app.AlertDialog
import com.simplemobiletools.commons.extensions.setupDialogStuff
import com.simplemobiletools.contacts.pro.R
import com.simplemobiletools.contacts.pro.activities.SimpleActivity
import com.simplemobiletools.contacts.pro.adapters.FilterContactSourcesAdapter
import com.simplemobiletools.contacts.pro.extensions.config
import com.simplemobiletools.contacts.pro.extensions.getVisibleContactSources
import com.simplemobiletools.contacts.pro.helpers.ContactsHelper
import com.simplemobiletools.contacts.pro.helpers.SMT_PRIVATE
import com.simplemobiletools.contacts.pro.models.ContactSource
import kotlinx.android.synthetic.main.dialog_filter_contact_sources.view.*
import java.util.*

class FilterContactSourcesDialog(val activity: SimpleActivity, private val callback: () -> Unit) {
    private var dialog: AlertDialog? = null
    private val view = activity.layoutInflater.inflate(R.layout.dialog_filter_contact_sources, null)
    private var contactSources = ArrayList<ContactSource>()

    init {
        ContactsHelper(activity).getContactSources {
            if (it.isEmpty()) {
                return@getContactSources
            }

            it.mapTo(contactSources) { it.copy() }
            val selectedSources = activity.getVisibleContactSources()
            activity.runOnUiThread {
                view.filter_contact_sources_list.adapter = FilterContactSourcesAdapter(activity, it, selectedSources)

                dialog = AlertDialog.Builder(activity)
                        .setPositiveButton(R.string.ok) { dialogInterface, i -> confirmContactSources() }
                        .setNegativeButton(R.string.cancel, null)
                        .create().apply {
                            activity.setupDialogStuff(view, this)
                        }
            }
        }
    }

    private fun confirmContactSources() {
        val selectedContactSources = (view.filter_contact_sources_list.adapter as FilterContactSourcesAdapter).getSelectedContactSources()
        val ignoredContactSources = contactSources.filter { !selectedContactSources.contains(it) }.map {
            if (it.type == SMT_PRIVATE) SMT_PRIVATE else it.getFullIdentifier()
        }.toHashSet()

        if (activity.getVisibleContactSources() != ignoredContactSources) {
            activity.config.ignoredContactSources = ignoredContactSources
            callback()
        }
        dialog?.dismiss()
    }
}
