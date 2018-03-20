package com.simplemobiletools.contacts.fragments

import android.content.Context
import android.util.AttributeSet
import com.simplemobiletools.contacts.activities.SimpleActivity
import com.simplemobiletools.contacts.dialogs.SelectContactsDialog
import com.simplemobiletools.contacts.helpers.ContactsHelper
import com.simplemobiletools.contacts.models.Contact

class FavoritesFragment(context: Context, attributeSet: AttributeSet) : MyViewPagerFragment(context, attributeSet) {
    override fun fabClicked() {
        finishActMode()
        showAddFavoritesDialog()
    }

    override fun placeholderClicked() {
        showAddFavoritesDialog()
    }

    private fun showAddFavoritesDialog() {
        SelectContactsDialog(activity!!) { displayedContacts, selectedContacts ->
            val contactsHelper = ContactsHelper(activity as SimpleActivity)
            val contactsToAdd = selectedContacts.map { it } as ArrayList<Contact>
            contactsHelper.addFavorites(contactsToAdd)

            displayedContacts.removeAll(selectedContacts)
            contactsHelper.removeFavorites(displayedContacts)
            activity!!.refreshContacts(false, true)
        }
    }
}
