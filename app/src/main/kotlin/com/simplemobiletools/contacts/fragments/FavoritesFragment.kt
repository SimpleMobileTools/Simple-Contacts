package com.simplemobiletools.contacts.fragments

import android.content.Context
import android.util.AttributeSet
import com.simplemobiletools.contacts.activities.SimpleActivity
import com.simplemobiletools.contacts.dialogs.SelectContactsDialog
import com.simplemobiletools.contacts.helpers.ContactsHelper

class FavoritesFragment(context: Context, attributeSet: AttributeSet) : MyViewPagerFragment(context, attributeSet) {
    override fun fabClicked() {
        finishActMode()
        showAddFavoritesDialog()
    }

    override fun placeholderClicked() {
        showAddFavoritesDialog()
    }

    private fun showAddFavoritesDialog() {
        SelectContactsDialog(activity!!, allContacts) { addedContacts, removedContacts ->
            ContactsHelper(activity as SimpleActivity).apply {
                addFavorites(addedContacts)
                removeFavorites(removedContacts)
            }

            activity!!.refreshContacts(false, true)
        }
    }
}
