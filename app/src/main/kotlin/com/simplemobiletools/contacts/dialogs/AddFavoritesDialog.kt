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
import java.util.HashSet
import kotlin.collections.ArrayList

class AddFavoritesDialog(val activity: SimpleActivity, val callback: () -> Unit) {
    private var dialog: AlertDialog? = null
    private var view = activity.layoutInflater.inflate(R.layout.dialog_add_favorites, null)
    private val config = activity.config
    private var allContacts = ArrayList<Contact>()

    init {
        ContactsHelper(activity).getContacts {
            allContacts = it
            Contact.sorting = config.sorting
            allContacts.sort()

            val contactSources = config.displayContactSources
            allContacts = allContacts.filter { contactSources.contains(it.source) } as ArrayList<Contact>
            view.add_favorites_list.adapter = AddFavoritesAdapter(activity, allContacts, config.favorites)

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
        val allDisplayedIDs = ArrayList<String>()
        allContacts.mapTo(allDisplayedIDs, { it.id.toString() })
        val selectedItems = (view.add_favorites_list.adapter as AddFavoritesAdapter).getSelectedItemsSet()
        config.addFavorites(selectedItems)
        allDisplayedIDs.removeAll(selectedItems)

        val favoriteIDsToRemove = HashSet<String>()
        allDisplayedIDs.mapTo(favoriteIDsToRemove, { it })
        config.removeFavorites(favoriteIDsToRemove)
        callback()
        dialog?.dismiss()
    }
}
