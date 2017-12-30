package com.simplemobiletools.contacts.dialogs

import android.support.v7.app.AlertDialog
import com.simplemobiletools.commons.extensions.setupDialogStuff
import com.simplemobiletools.contacts.R
import com.simplemobiletools.contacts.activities.SimpleActivity
import com.simplemobiletools.contacts.adapters.AddFavoritesAdapter
import com.simplemobiletools.contacts.extensions.config
import com.simplemobiletools.contacts.helpers.ContactsHelper
import com.simplemobiletools.contacts.models.Contact
import kotlinx.android.synthetic.main.dialog_add_favorites.view.*

class AddFavoritesDialog(val activity: SimpleActivity, val callback: () -> Unit) {
    private var dialog: AlertDialog? = null
    private var view = activity.layoutInflater.inflate(R.layout.dialog_add_favorites, null)
    private val config = activity.config

    init {
        ContactsHelper(activity).getContacts {
            var contacts = it
            Contact.sorting = config.sorting
            contacts.sort()

            val contactSources = config.displayContactSources
            contacts = contacts.filter { contactSources.contains(it.source) } as ArrayList<Contact>
            view.add_favorites_list.adapter = AddFavoritesAdapter(activity, contacts, config.favorites)

            activity.runOnUiThread {
                dialog = AlertDialog.Builder(activity)
                        .setPositiveButton(R.string.ok, { dialog, which -> dialogConfirmed() })
                        .setNegativeButton(R.string.cancel, null)
                        .create().apply {
                    activity.setupDialogStuff(view, this)
                }
            }
        }
    }

    private fun dialogConfirmed() {
        val selectedItems = (view.add_favorites_list.adapter as AddFavoritesAdapter).getSelectedItemsSet()
        if (config.favorites != selectedItems) {
            config.favorites = selectedItems
            callback()
        }
        dialog?.dismiss()
    }
}
