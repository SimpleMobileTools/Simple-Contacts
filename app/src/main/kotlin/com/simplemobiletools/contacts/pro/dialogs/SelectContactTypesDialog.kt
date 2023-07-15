package com.simplemobiletools.contacts.pro.dialogs

import androidx.appcompat.app.AlertDialog
import com.simplemobiletools.commons.extensions.getAlertDialogBuilder
import com.simplemobiletools.commons.extensions.setupDialogStuff
import com.simplemobiletools.commons.helpers.ContactsHelper
import com.simplemobiletools.commons.models.contacts.Contact
import com.simplemobiletools.commons.models.contacts.ContactSource
import com.simplemobiletools.contacts.pro.R
import com.simplemobiletools.contacts.pro.activities.SimpleActivity
import com.simplemobiletools.contacts.pro.adapters.FilterContactSourcesAdapter
import kotlinx.android.synthetic.main.dialog_filter_contact_sources.view.filter_contact_sources_list

class SelectContactTypesDialog(
    val activity: SimpleActivity,
    private val selectedContactTypes: List<String>,
    val callback: (HashSet<ContactSource>) -> Unit
) {
    private var dialog: AlertDialog? = null
    private val view = activity.layoutInflater.inflate(R.layout.dialog_filter_contact_sources, null)

    private var contactSources = mutableListOf<ContactSource>()
    private var contacts = listOf<Contact>()
    private var isContactSourcesReady = false
    private var isContactsReady = false

    init {
        ContactsHelper(activity).getContactSources { sources ->
            contactSources = sources
            isContactSourcesReady = true
            processDataIfReady()
        }

        ContactsHelper(activity).getContacts(getAll = true) { receivedContacts ->
            contacts = receivedContacts
            isContactsReady = true
            processDataIfReady()
        }
    }

    private fun processDataIfReady() {
        if (!isContactSourcesReady) {
            return
        }

        val contactSourcesWithCount = mutableListOf<ContactSource>()
        for (contactSource in contactSources) {
            val count = if (isContactsReady) {
                contacts.count { it.source == contactSource.name }
            } else {
                -1
            }
            contactSourcesWithCount.add(contactSource.copy(count = count))
        }

        contactSources.clear()
        contactSources.addAll(contactSourcesWithCount)

        activity.runOnUiThread {
            view.filter_contact_sources_list.adapter = FilterContactSourcesAdapter(activity, contactSourcesWithCount, selectedContactTypes.toList())
            if (dialog == null) {
                activity.runOnUiThread {
                    activity.getAlertDialogBuilder()
                        .setPositiveButton(R.string.ok) { _, _ -> confirmContactTypes() }
                        .setNegativeButton(R.string.cancel, null)
                        .apply {
                            activity.setupDialogStuff(view, this) { alertDialog ->
                                dialog = alertDialog
                            }
                        }
                }
            }
        }
    }

    private fun confirmContactTypes() {
        val adapter = view.filter_contact_sources_list.adapter as FilterContactSourcesAdapter
        val selectedItems = adapter.getSelectedContactSources()
        callback(selectedItems.toHashSet())
        dialog?.dismiss()
    }
}
