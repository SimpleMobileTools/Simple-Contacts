package com.simplemobiletools.contacts.dialogs

import androidx.appcompat.app.AlertDialog
import com.simplemobiletools.commons.extensions.baseConfig
import com.simplemobiletools.commons.extensions.setupDialogStuff
import com.simplemobiletools.contacts.R
import com.simplemobiletools.contacts.activities.SimpleActivity
import com.simplemobiletools.contacts.adapters.SelectContactsAdapter
import com.simplemobiletools.contacts.extensions.config
import com.simplemobiletools.contacts.extensions.getVisibleContactSources
import com.simplemobiletools.contacts.models.Contact
import kotlinx.android.synthetic.main.layout_select_contact.view.*

class SelectContactsDialog(val activity: SimpleActivity, initialContacts: ArrayList<Contact>, val selectContacts: ArrayList<Contact>? = null,
                           val callback: (addedContacts: ArrayList<Contact>, removedContacts: ArrayList<Contact>) -> Unit) {
    private var view = activity.layoutInflater.inflate(R.layout.layout_select_contact, null)
    private var initiallySelectedContacts = ArrayList<Contact>()

    init {
        var allContacts = initialContacts
        if (selectContacts == null) {
            val contactSources = activity.getVisibleContactSources()
            allContacts = allContacts.filter { contactSources.contains(it.source) } as ArrayList<Contact>

            initiallySelectedContacts = allContacts.filter { it.starred == 1 } as ArrayList<Contact>
        } else {
            initiallySelectedContacts = selectContacts
        }

        Contact.sorting = activity.config.sorting
        Contact.startWithSurname = activity.config.startNameWithSurname
        allContacts.sort()

        activity.runOnUiThread {
            view.apply {
                select_contact_list.adapter = SelectContactsAdapter(activity, allContacts, initiallySelectedContacts, true, select_contact_list)
                select_contact_fastscroller.allowBubbleDisplay = activity.baseConfig.showInfoBubble
                select_contact_fastscroller.setViews(select_contact_list) {
                    select_contact_fastscroller.updateBubbleText(allContacts[it].getBubbleText())
                }
            }
        }

        AlertDialog.Builder(activity)
                .setPositiveButton(R.string.ok) { dialog, which -> dialogConfirmed() }
                .setNegativeButton(R.string.cancel, null)
                .create().apply {
                    activity.setupDialogStuff(view, this)
                }
    }

    private fun dialogConfirmed() {
        Thread {
            val adapter = view?.select_contact_list?.adapter as? SelectContactsAdapter
            val selectedContacts = adapter?.getSelectedItemsSet()?.toList() ?: ArrayList()

            val newlySelectedContacts = selectedContacts.filter { !initiallySelectedContacts.contains(it) } as ArrayList
            val unselectedContacts = initiallySelectedContacts.filter { !selectedContacts.contains(it) } as ArrayList
            callback(newlySelectedContacts, unselectedContacts)
        }.start()
    }
}
