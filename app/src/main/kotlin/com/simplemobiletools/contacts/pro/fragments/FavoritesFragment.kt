package com.simplemobiletools.contacts.pro.fragments

import android.content.Context
import android.util.AttributeSet
import com.google.gson.Gson
import com.simplemobiletools.commons.extensions.areSystemAnimationsEnabled
import com.simplemobiletools.commons.extensions.beGone
import com.simplemobiletools.commons.extensions.beVisible
import com.simplemobiletools.commons.helpers.ContactsHelper
import com.simplemobiletools.commons.helpers.TAB_FAVORITES
import com.simplemobiletools.commons.helpers.VIEW_TYPE_GRID
import com.simplemobiletools.commons.models.contacts.Contact
import com.simplemobiletools.commons.views.MyGridLayoutManager
import com.simplemobiletools.commons.views.MyLinearLayoutManager
import com.simplemobiletools.contacts.pro.activities.MainActivity
import com.simplemobiletools.contacts.pro.activities.SimpleActivity
import com.simplemobiletools.contacts.pro.adapters.ContactsAdapter
import com.simplemobiletools.contacts.pro.dialogs.SelectContactsDialog
import com.simplemobiletools.contacts.pro.extensions.config
import com.simplemobiletools.contacts.pro.helpers.LOCATION_FAVORITES_TAB
import com.simplemobiletools.contacts.pro.interfaces.RefreshContactsListener
import kotlinx.android.synthetic.main.dialog_select_contact.view.letter_fastscroller
import kotlinx.android.synthetic.main.fragment_favorites.view.favorites_fragment
import kotlinx.android.synthetic.main.fragment_layout.view.fragment_list

class FavoritesFragment(context: Context, attributeSet: AttributeSet) : MyViewPagerFragment(context, attributeSet) {
    private var favouriteContacts = listOf<Contact>()

    override fun fabClicked() {
        finishActMode()
        showAddFavoritesDialog()
    }

    override fun placeholderClicked() {
        showAddFavoritesDialog()
    }

    private fun showAddFavoritesDialog() {
        SelectContactsDialog(activity!!, allContacts, true, false) { addedContacts, removedContacts ->
            ContactsHelper(activity as SimpleActivity).apply {
                addFavorites(addedContacts)
                removeFavorites(removedContacts)
            }

            (activity as? MainActivity)?.refreshContacts(TAB_FAVORITES)
        }
    }

    fun setupContactsFavoritesAdapter(contacts: List<Contact>) {
        favouriteContacts = contacts
        setupViewVisibility(favouriteContacts.isNotEmpty())
        val currAdapter = fragment_list.adapter

        val viewType = context.config.viewType
        setFavoritesViewType(viewType)

        if (currAdapter == null || forceListRedraw) {
            forceListRedraw = false
            val location = LOCATION_FAVORITES_TAB

            ContactsAdapter(
                activity = activity as SimpleActivity,
                contactItems = favouriteContacts.toMutableList(),
                refreshListener = activity as RefreshContactsListener,
                location = location,
                viewType = viewType,
                removeListener = null,
                recyclerView = fragment_list,
                enableDrag = true,
            ) {
                (activity as RefreshContactsListener).contactClicked(it as Contact)
            }.apply {
                fragment_list.adapter = this
                onDragEndListener = {
                    val adapter = fragment_list?.adapter
                    if (adapter is ContactsAdapter) {
                        val items = adapter.contactItems
                        saveCustomOrderToPrefs(items)
                        setupLetterFastscroller(items)
                    }
                }

                onColumnCountListener = { newColumnCount ->
                    context.config.contactsGridColumnCount = newColumnCount
                }
            }

            if (context.areSystemAnimationsEnabled) {
                fragment_list.scheduleLayoutAnimation()
            }
        } else {
            (currAdapter as ContactsAdapter).apply {
                startNameWithSurname = context.config.startNameWithSurname
                showPhoneNumbers = context.config.showPhoneNumbers
                showContactThumbnails = context.config.showContactThumbnails
                this.viewType = viewType
                updateItems(favouriteContacts)
            }
        }
    }

    fun updateFavouritesAdapter() {
        setupContactsFavoritesAdapter(favouriteContacts)
    }

    private fun setFavoritesViewType(viewType: Int) {
        val spanCount = context.config.contactsGridColumnCount

        val layoutManager = if (viewType == VIEW_TYPE_GRID) {
            favorites_fragment.letter_fastscroller.beGone()
            MyGridLayoutManager(context, spanCount)
        } else {
            favorites_fragment.letter_fastscroller.beVisible()
            MyLinearLayoutManager(context)
        }
        fragment_list.layoutManager = layoutManager
    }

    fun columnCountChanged() {
        (fragment_list.layoutManager as MyGridLayoutManager).spanCount = context!!.config.contactsGridColumnCount
        fragment_list?.adapter?.apply {
            notifyItemRangeChanged(0, favouriteContacts.size)
        }
    }

    private fun saveCustomOrderToPrefs(items: List<Contact>) {
        activity?.apply {
            val orderIds = items.map { it.id }
            val orderGsonString = Gson().toJson(orderIds)
            config.favoritesContactsOrder = orderGsonString
        }
    }

}
