package com.simplemobiletools.contacts.dialogs

import android.support.v7.app.AlertDialog
import com.simplemobiletools.commons.extensions.setupDialogStuff
import com.simplemobiletools.contacts.R
import com.simplemobiletools.contacts.activities.SimpleActivity
import com.simplemobiletools.contacts.adapters.FilterContactSourcesAdapter
import com.simplemobiletools.contacts.extensions.config
import com.simplemobiletools.contacts.extensions.getVisibleContactSources
import com.simplemobiletools.contacts.helpers.ContactsHelper
import com.simplemobiletools.contacts.helpers.SMT_PRIVATE
import com.simplemobiletools.contacts.models.ContactSource
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

            it.mapTo(contactSources, { it.copy() })
            val selectedSources = activity.getVisibleContactSources()
            activity.runOnUiThread {
                view.filter_contact_sources_list.adapter = FilterContactSourcesAdapter(activity, it, selectedSources)

                dialog = AlertDialog.Builder(activity)
                        .setPositiveButton(R.string.ok, { dialogInterface, i -> confirmEventTypes() })
                        .setNegativeButton(R.string.cancel, null)
                        .create().apply {
                            activity.setupDialogStuff(view, this)
                        }
            }
        }
    }

    private fun confirmEventTypes() {
        val selectedIndexes = (view.filter_contact_sources_list.adapter as FilterContactSourcesAdapter).getSelectedItemsSet()
        val ignoredIndexes = ArrayList<Int>()
        for (i in 0 until contactSources.size) {
            if (!selectedIndexes.contains(i)) {
                ignoredIndexes.add(i)
            }
        }

        val ignoredContactSources = HashSet<String>()
        ignoredIndexes.forEach {
            ignoredContactSources.add(if (contactSources[it].type == SMT_PRIVATE) SMT_PRIVATE else contactSources[it].name)
        }

        if (activity.getVisibleContactSources() != ignoredContactSources) {
            activity.config.ignoredContactSources = ignoredContactSources
            callback()
        }
        dialog?.dismiss()
    }
}
