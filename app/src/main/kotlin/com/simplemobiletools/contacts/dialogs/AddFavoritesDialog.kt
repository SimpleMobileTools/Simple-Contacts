package com.simplemobiletools.contacts.dialogs

import android.support.v7.app.AlertDialog
import com.simplemobiletools.commons.extensions.baseConfig
import com.simplemobiletools.commons.extensions.setupDialogStuff
import com.simplemobiletools.contacts.R
import com.simplemobiletools.contacts.activities.SimpleActivity
import com.simplemobiletools.contacts.adapters.SelectContactsAdapter
import com.simplemobiletools.contacts.extensions.config
import com.simplemobiletools.contacts.helpers.ContactsHelper
import com.simplemobiletools.contacts.models.Contact
import kotlinx.android.synthetic.main.layout_select_contact.view.*

class AddFavoritesDialog(val activity: SimpleActivity, private val callback: () -> Unit) {
    private var view = activity.layoutInflater.inflate(R.layout.layout_select_contact, null)
    private val config = activity.config
    private var allContacts = ArrayList<Contact>()

    init {
        ContactsHelper(activity).getContacts {
            allContacts = it

            val contactSources = config.displayContactSources
            if (!activity.config.showAllContacts()) {
                allContacts = allContacts.filter { contactSources.contains(it.source) } as ArrayList<Contact>
            }

            val favorites = allContacts.filter { it.starred == 1 }.map { it.id.toString() } as ArrayList<String>

            Contact.sorting = config.sorting
            allContacts.sort()

            activity.runOnUiThread {
                view.apply {
                    select_contact_list.adapter = SelectContactsAdapter(activity, allContacts, favorites, true)
                    select_contact_fastscroller.allowBubbleDisplay = activity.baseConfig.showInfoBubble
                    select_contact_fastscroller.setViews(select_contact_list) {
                        select_contact_fastscroller.updateBubbleText(allContacts[it].getBubbleText())
                    }
                }
            }
        }

        AlertDialog.Builder(activity)
                .setPositiveButton(R.string.ok, { dialog, which -> dialogConfirmed() })
                .setNegativeButton(R.string.cancel, null)
                .create().apply {
            activity.setupDialogStuff(view, this)
        }
    }

    private fun dialogConfirmed() {
        Thread {
            val contactsHelper = ContactsHelper(activity)
            val allDisplayedContacts = ArrayList<Contact>()
            allContacts.mapTo(allDisplayedContacts, { it })
            val selectedContacts = (view?.select_contact_list?.adapter as? SelectContactsAdapter)?.getSelectedItemsSet() ?: LinkedHashSet()
            val contactsToAdd = selectedContacts.map { it } as ArrayList<Contact>
            contactsHelper.addFavorites(contactsToAdd)

            allDisplayedContacts.removeAll(selectedContacts)
            contactsHelper.removeFavorites(allDisplayedContacts)

            callback()
        }.start()
    }
}
