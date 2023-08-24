package com.simplemobiletools.contacts.pro.fragments

import android.content.Context
import android.util.AttributeSet
import com.google.gson.Gson
import com.simplemobiletools.commons.extensions.areSystemAnimationsEnabled
import com.simplemobiletools.commons.extensions.beGone
import com.simplemobiletools.commons.extensions.beVisible
import com.simplemobiletools.commons.helpers.CONTACTS_GRID_MAX_COLUMNS_COUNT
import com.simplemobiletools.commons.helpers.ContactsHelper
import com.simplemobiletools.commons.helpers.TAB_FAVORITES
import com.simplemobiletools.commons.helpers.VIEW_TYPE_GRID
import com.simplemobiletools.commons.models.contacts.Contact
import com.simplemobiletools.commons.views.MyGridLayoutManager
import com.simplemobiletools.commons.views.MyLinearLayoutManager
import com.simplemobiletools.commons.views.MyRecyclerView
import com.simplemobiletools.contacts.pro.activities.MainActivity
import com.simplemobiletools.contacts.pro.activities.SimpleActivity
import com.simplemobiletools.contacts.pro.adapters.ContactsAdapter
import com.simplemobiletools.contacts.pro.databinding.FragmentFavoritesBinding
import com.simplemobiletools.contacts.pro.databinding.FragmentLettersLayoutBinding
import com.simplemobiletools.contacts.pro.dialogs.SelectContactsDialog
import com.simplemobiletools.contacts.pro.extensions.config
import com.simplemobiletools.contacts.pro.helpers.LOCATION_FAVORITES_TAB
import com.simplemobiletools.contacts.pro.interfaces.RefreshContactsListener

class FavoritesFragment(context: Context, attributeSet: AttributeSet) : MyViewPagerFragment<MyViewPagerFragment.LetterLayout>(context, attributeSet) {
    private var favouriteContacts = listOf<Contact>()
    private var zoomListener: MyRecyclerView.MyZoomListener? = null
    private lateinit var binding: FragmentFavoritesBinding

    override fun onFinishInflate() {
        super.onFinishInflate()
        binding = FragmentFavoritesBinding.bind(this)
        innerBinding = LetterLayout(FragmentLettersLayoutBinding.bind(binding.root))
    }

    override fun fabClicked() {
        finishActMode()
        showAddFavoritesDialog()
    }

    override fun placeholderClicked() {
        showAddFavoritesDialog()
    }

    private fun getRecyclerAdapter() = innerBinding.fragmentList.adapter as? ContactsAdapter

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
        val currAdapter = getRecyclerAdapter()

        val viewType = context.config.viewType
        setFavoritesViewType(viewType)
        initZoomListener(viewType)

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
                recyclerView = innerBinding.fragmentList,
                enableDrag = true,
            ) {
                (activity as RefreshContactsListener).contactClicked(it as Contact)
            }.apply {
                innerBinding.fragmentList.adapter = this
                setupZoomListener(zoomListener)
                onDragEndListener = {
                    val adapter = innerBinding.fragmentList.adapter
                    if (adapter is ContactsAdapter) {
                        val items = adapter.contactItems
                        saveCustomOrderToPrefs(items)
                        setupLetterFastscroller(items)
                    }
                }
            }

            if (context.areSystemAnimationsEnabled) {
                innerBinding.fragmentList.scheduleLayoutAnimation()
            }
        } else {
            currAdapter.apply {
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

        if (viewType == VIEW_TYPE_GRID) {
            innerBinding.letterFastscroller.beGone()
            innerBinding.fragmentList.layoutManager = MyGridLayoutManager(context, spanCount)
        } else {
            innerBinding.letterFastscroller.beVisible()
            innerBinding.fragmentList.layoutManager = MyLinearLayoutManager(context)
        }
    }

    private fun initZoomListener(viewType: Int) {
        if (viewType == VIEW_TYPE_GRID) {
            val layoutManager = innerBinding.fragmentList.layoutManager as MyGridLayoutManager
            zoomListener = object : MyRecyclerView.MyZoomListener {
                override fun zoomIn() {
                    if (layoutManager.spanCount > 1) {
                        reduceColumnCount()
                        getRecyclerAdapter()?.finishActMode()
                    }
                }

                override fun zoomOut() {
                    if (layoutManager.spanCount < CONTACTS_GRID_MAX_COLUMNS_COUNT) {
                        increaseColumnCount()
                        getRecyclerAdapter()?.finishActMode()
                    }
                }
            }
        } else {
            zoomListener = null
        }
    }

    private fun increaseColumnCount() {
        if (context.config.viewType == VIEW_TYPE_GRID) {
            context!!.config.contactsGridColumnCount += 1
            columnCountChanged()
        }
    }

    private fun reduceColumnCount() {
        if (context.config.viewType == VIEW_TYPE_GRID) {
            context!!.config.contactsGridColumnCount -= 1
            columnCountChanged()
        }
    }

    fun columnCountChanged() {
        (innerBinding.fragmentList.layoutManager as? MyGridLayoutManager)?.spanCount = context!!.config.contactsGridColumnCount
        getRecyclerAdapter()?.apply {
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
