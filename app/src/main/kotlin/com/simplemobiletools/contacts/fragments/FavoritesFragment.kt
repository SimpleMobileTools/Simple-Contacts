package com.simplemobiletools.contacts.fragments

import android.content.Context
import android.util.AttributeSet
import com.simplemobiletools.contacts.activities.MainActivity
import com.simplemobiletools.contacts.dialogs.AddFavoritesDialog
import kotlinx.android.synthetic.main.fragment_favorites.view.*

class FavoritesFragment(context: Context, attributeSet: AttributeSet) : MyViewPagerFragment(context, attributeSet) {
    var activity: MainActivity? = null
    override fun initFragment(activity: MainActivity) {
        if (this.activity == null) {
            this.activity = activity
            favorites_fab.setOnClickListener {
                addNewFavorites()
            }
        }
    }

    override fun textColorChanged(color: Int) {
    }

    override fun primaryColorChanged(color: Int) {
    }

    override fun startNameWithSurnameChanged(startNameWithSurname: Boolean) {
    }

    override fun onActivityResume() {
    }

    private fun addNewFavorites() {
        AddFavoritesDialog(activity!!) {

        }
    }
}
