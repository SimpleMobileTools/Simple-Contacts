package com.simplemobiletools.contacts.fragments

import android.content.Context
import android.util.AttributeSet
import com.simplemobiletools.contacts.dialogs.AddFavoritesDialog

class FavoritesFragment(context: Context, attributeSet: AttributeSet) : MyViewPagerFragment(context, attributeSet) {
    override fun fabClicked() {
        AddFavoritesDialog(activity!!) {
            initContacts()
        }
    }

    override fun refreshItems() {
        initContacts()
    }
}
