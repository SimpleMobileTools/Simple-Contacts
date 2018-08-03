package com.simplemobiletools.contacts.fragments

import android.content.Context
import android.content.Intent
import android.support.design.widget.CoordinatorLayout
import android.util.AttributeSet
import android.view.ViewGroup
import com.simplemobiletools.commons.adapters.MyRecyclerViewAdapter
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.helpers.SORT_BY_FIRST_NAME
import com.simplemobiletools.commons.helpers.SORT_BY_SURNAME
import com.simplemobiletools.contacts.R
import com.simplemobiletools.contacts.activities.GroupContactsActivity
import com.simplemobiletools.contacts.activities.MainActivity
import com.simplemobiletools.contacts.activities.SimpleActivity
import com.simplemobiletools.contacts.adapters.ContactsAdapter
import com.simplemobiletools.contacts.adapters.GroupsAdapter
import com.simplemobiletools.contacts.extensions.*
import com.simplemobiletools.contacts.helpers.*
import com.simplemobiletools.contacts.models.Contact
import com.simplemobiletools.contacts.models.Group
import kotlinx.android.synthetic.main.fragment_layout.view.*

abstract class MyViewPagerFragment(context: Context, attributeSet: AttributeSet) : CoordinatorLayout(context, attributeSet) {
    protected var activity: MainActivity? = null
    protected var allContacts = ArrayList<Contact>()

    private var lastHashCode = 0
    private var contactsIgnoringSearch = ArrayList<Contact>()
    private lateinit var config: Config

    var skipHashComparing = false
    var forceListRedraw = false

    fun setupFragment(activity: MainActivity) {
        config = activity.config
        if (this.activity == null) {
            this.activity = activity
            fragment_fab.setOnClickListener {
                fabClicked()
            }

            fragment_placeholder_2.setOnClickListener {
                placeholderClicked()
            }

            fragment_placeholder_2.underlineText()
            updateViewStuff()

            when {
                this is FavoritesFragment -> {
                    fragment_placeholder.text = activity.getString(R.string.no_favorites)
                    fragment_placeholder_2.text = activity.getString(R.string.add_favorites)
                }
                this is GroupsFragment -> {
                    fragment_placeholder.text = activity.getString(R.string.no_group_created)
                    fragment_placeholder_2.text = activity.getString(R.string.create_group)
                }
                this is RecentsFragment -> fragment_fab.beGone()
            }
        }
    }

    fun textColorChanged(color: Int) {
        if (this is GroupsFragment) {
            (fragment_list.adapter as GroupsAdapter).updateTextColor(color)
        } else {
            (fragment_list.adapter as ContactsAdapter).apply {
                updateTextColor(color)
                initDrawables()
            }
        }
    }

    fun primaryColorChanged() {
        fragment_fastscroller.updatePrimaryColor()
        fragment_fastscroller.updateBubblePrimaryColor()
        (fragment_list.adapter as? ContactsAdapter)?.apply {
            adjustedPrimaryColor = context.getAdjustedPrimaryColor()
        }
    }

    fun startNameWithSurnameChanged(startNameWithSurname: Boolean) {
        if (this !is GroupsFragment) {
            (fragment_list.adapter as ContactsAdapter).apply {
                config.sorting = if (startNameWithSurname) SORT_BY_SURNAME else SORT_BY_FIRST_NAME
                this@MyViewPagerFragment.activity!!.refreshContacts(CONTACTS_TAB_MASK or FAVORITES_TAB_MASK)
            }
        }
    }

    fun refreshContacts(contacts: ArrayList<Contact>) {
        if ((config.showTabs and CONTACTS_TAB_MASK == 0 && this is ContactsFragment) ||
                (config.showTabs and FAVORITES_TAB_MASK == 0 && this is FavoritesFragment) ||
                (config.showTabs and RECENTS_TAB_MASK == 0 && this is RecentsFragment) ||
                (config.showTabs and GROUPS_TAB_MASK == 0 && this is GroupsFragment)) {
            return
        }

        if (config.lastUsedContactSource.isEmpty()) {
            val grouped = contacts.groupBy { it.source }.maxWith(compareBy { it.value.size })
            config.lastUsedContactSource = grouped?.key ?: ""
        }

        Contact.sorting = config.sorting
        Contact.startWithSurname = config.startNameWithSurname
        contacts.sort()
        allContacts = contacts

        val filtered = when {
            this is GroupsFragment -> contacts
            this is FavoritesFragment -> contacts.filter { it.starred == 1 } as ArrayList<Contact>
            this is RecentsFragment -> ArrayList()
            else -> {
                val contactSources = activity!!.getVisibleContactSources()
                contacts.filter { contactSources.contains(it.source) } as ArrayList<Contact>
            }
        }

        if (filtered.hashCode() != lastHashCode || skipHashComparing) {
            skipHashComparing = false
            lastHashCode = filtered.hashCode()
            activity?.runOnUiThread {
                setupContacts(filtered)
            }
        }
    }

    private fun setupContacts(contacts: ArrayList<Contact>) {
        if (this is GroupsFragment) {
            setupGroupsAdapter(contacts)
        } else {
            setupContactsFavoritesAdapter(contacts)
        }
    }

    private fun setupGroupsAdapter(contacts: ArrayList<Contact>) {
        var storedGroups = ContactsHelper(activity!!).getStoredGroups()
        contacts.forEach {
            it.groups.forEach {
                val group = it
                val storedGroup = storedGroups.firstOrNull { it.id == group.id }
                storedGroup?.addContact()
            }
        }

        storedGroups = storedGroups.sortedWith(compareBy { it.title }).toMutableList() as ArrayList<Group>

        fragment_placeholder_2.beVisibleIf(storedGroups.isEmpty())
        fragment_placeholder.beVisibleIf(storedGroups.isEmpty())
        fragment_list.beVisibleIf(storedGroups.isNotEmpty())

        val currAdapter = fragment_list.adapter
        if (currAdapter == null) {
            GroupsAdapter(activity as SimpleActivity, storedGroups, activity, fragment_list, fragment_fastscroller) {
                Intent(activity, GroupContactsActivity::class.java).apply {
                    putExtra(GROUP, it as Group)
                    activity!!.startActivity(this)
                }
            }.apply {
                addVerticalDividers(true)
                fragment_list.adapter = this
            }

            fragment_fastscroller.setScrollToY(0)
            fragment_fastscroller.setViews(fragment_list) {
                val item = (fragment_list.adapter as GroupsAdapter).groups.getOrNull(it)
                fragment_fastscroller.updateBubbleText(item?.getBubbleText() ?: "")
            }
        } else {
            (currAdapter as GroupsAdapter).apply {
                showContactThumbnails = activity.config.showContactThumbnails
                updateItems(storedGroups)
            }
        }
    }

    private fun setupContactsFavoritesAdapter(contacts: ArrayList<Contact>) {
        fragment_placeholder_2.beVisibleIf(contacts.isEmpty() && this !is RecentsFragment)
        fragment_placeholder.beVisibleIf(contacts.isEmpty())
        fragment_list.beVisibleIf(contacts.isNotEmpty())

        val currAdapter = fragment_list.adapter
        if (currAdapter == null || forceListRedraw) {
            forceListRedraw = false
            val location = if (this is FavoritesFragment) LOCATION_FAVORITES_TAB else LOCATION_CONTACTS_TAB
            ContactsAdapter(activity as SimpleActivity, contacts, activity, location, null, fragment_list, fragment_fastscroller) {
                when (config.onContactClick) {
                    ON_CLICK_CALL_CONTACT -> {
                        val contact = it as Contact
                        if (contact.phoneNumbers.isNotEmpty()) {
                            (activity as SimpleActivity).tryStartCall(it)
                        } else {
                            activity!!.toast(R.string.no_phone_number_found)
                        }
                    }
                    ON_CLICK_VIEW_CONTACT -> context!!.viewContact(it as Contact)
                    ON_CLICK_EDIT_CONTACT -> context!!.editContact(it as Contact)
                }
            }.apply {
                addVerticalDividers(true)
                fragment_list.adapter = this
            }

            fragment_fastscroller.setScrollToY(0)
            fragment_fastscroller.setViews(fragment_list) {
                val item = (fragment_list.adapter as ContactsAdapter).contactItems.getOrNull(it)
                fragment_fastscroller.updateBubbleText(item?.getBubbleText() ?: "")
            }
        } else {
            (currAdapter as ContactsAdapter).apply {
                startNameWithSurname = config.startNameWithSurname
                showPhoneNumbers = config.showPhoneNumbers
                showContactThumbnails = config.showContactThumbnails
                updateItems(contacts)
            }
        }
    }

    fun showContactThumbnailsChanged(showThumbnails: Boolean) {
        if (this is GroupsFragment) {
            (fragment_list.adapter as? GroupsAdapter)?.apply {
                showContactThumbnails = showThumbnails
                notifyDataSetChanged()
            }
        } else {
            (fragment_list.adapter as? ContactsAdapter)?.apply {
                showContactThumbnails = showThumbnails
                notifyDataSetChanged()
            }
        }
    }

    fun onActivityResume() {
        updateViewStuff()
    }

    fun finishActMode() {
        (fragment_list.adapter as? MyRecyclerViewAdapter)?.finishActMode()
    }

    fun onSearchQueryChanged(text: String) {
        (fragment_list.adapter as? ContactsAdapter)?.apply {
            val filtered = contactsIgnoringSearch.filter {
                it.getFullName().contains(text, true) ||
                        it.phoneNumbers.any { it.value.contains(text, true) } ||
                        it.emails.any { it.value.contains(text, true) } ||
                        it.addresses.any { it.value.contains(text, true) } ||
                        it.notes.contains(text, true) ||
                        it.organization.company.contains(text, true) ||
                        it.organization.jobPosition.contains(text, true) ||
                        it.websites.any { it.contains(text, true) }
            } as ArrayList

            Contact.sorting = config.sorting
            Contact.startWithSurname = config.startNameWithSurname
            filtered.sort()
            filtered.sortBy { !it.getFullName().startsWith(text, true) }

            if (filtered.isEmpty() && this@MyViewPagerFragment is FavoritesFragment) {
                fragment_placeholder.text = activity.getString(R.string.no_items_found)
            }

            fragment_placeholder.beVisibleIf(filtered.isEmpty())
            updateItems(filtered, text)
        }
    }

    fun onSearchOpened() {
        contactsIgnoringSearch = (fragment_list?.adapter as? ContactsAdapter)?.contactItems ?: ArrayList()
    }

    fun onSearchClosed() {
        (fragment_list.adapter as? ContactsAdapter)?.updateItems(contactsIgnoringSearch)
        if (this is FavoritesFragment) {
            fragment_placeholder.text = activity?.getString(R.string.no_favorites)
        }
    }

    private fun updateViewStuff() {
        context.updateTextColors(fragment_wrapper.parent as ViewGroup)
        fragment_fastscroller.updateBubbleColors()
        fragment_fastscroller.allowBubbleDisplay = config.showInfoBubble
        fragment_placeholder_2.setTextColor(context.getAdjustedPrimaryColor())
    }

    abstract fun fabClicked()

    abstract fun placeholderClicked()
}
