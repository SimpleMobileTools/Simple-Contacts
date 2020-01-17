package com.simplemobiletools.contacts.pro.fragments

import android.content.Context
import android.content.Intent
import android.util.AttributeSet
import android.view.ViewGroup
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.simplemobiletools.commons.adapters.MyRecyclerViewAdapter
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.helpers.SORT_BY_FIRST_NAME
import com.simplemobiletools.commons.helpers.SORT_BY_SURNAME
import com.simplemobiletools.contacts.pro.R
import com.simplemobiletools.contacts.pro.activities.GroupContactsActivity
import com.simplemobiletools.contacts.pro.activities.InsertOrEditContactActivity
import com.simplemobiletools.contacts.pro.activities.MainActivity
import com.simplemobiletools.contacts.pro.activities.SimpleActivity
import com.simplemobiletools.contacts.pro.adapters.ContactsAdapter
import com.simplemobiletools.contacts.pro.adapters.GroupsAdapter
import com.simplemobiletools.contacts.pro.extensions.config
import com.simplemobiletools.contacts.pro.extensions.getVisibleContactSources
import com.simplemobiletools.contacts.pro.helpers.*
import com.simplemobiletools.contacts.pro.interfaces.RefreshContactsListener
import com.simplemobiletools.contacts.pro.models.Contact
import com.simplemobiletools.contacts.pro.models.Group
import kotlinx.android.synthetic.main.fragment_layout.view.*

abstract class MyViewPagerFragment(context: Context, attributeSet: AttributeSet) : CoordinatorLayout(context, attributeSet) {
    protected var activity: SimpleActivity? = null
    protected var allContacts = ArrayList<Contact>()

    private var lastHashCode = 0
    private var contactsIgnoringSearch = ArrayList<Contact>()
    private var groupsIgnoringSearch = ArrayList<Group>()
    private lateinit var config: Config

    var skipHashComparing = false
    var forceListRedraw = false

    fun setupFragment(activity: SimpleActivity) {
        config = activity.config
        if (this.activity == null) {
            this.activity = activity
            fragment_fab?.setOnClickListener {
                if (activity is InsertOrEditContactActivity) {
                    activity.fabClicked()
                } else {
                    fabClicked()
                }
            }

            fragment_placeholder_2?.setOnClickListener {
                placeholderClicked()
            }

            fragment_placeholder_2?.underlineText()
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
            }
        }
    }

    fun textColorChanged(color: Int) {
        when {
            this is GroupsFragment -> (fragment_list.adapter as GroupsAdapter).updateTextColor(color)
            else -> (fragment_list.adapter as ContactsAdapter).apply {
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

        (fragment_list.adapter as? GroupsAdapter)?.apply {
            adjustedPrimaryColor = context.getAdjustedPrimaryColor()
        }
    }

    fun startNameWithSurnameChanged(startNameWithSurname: Boolean) {
        if (this !is GroupsFragment) {
            (fragment_list.adapter as? ContactsAdapter)?.apply {
                config.sorting = if (startNameWithSurname) SORT_BY_SURNAME else SORT_BY_FIRST_NAME
                (this@MyViewPagerFragment.activity!! as MainActivity).refreshContacts(CONTACTS_TAB_MASK or FAVORITES_TAB_MASK)
            }
        }
    }

    fun refreshContacts(contacts: ArrayList<Contact>) {
        if ((config.showTabs and CONTACTS_TAB_MASK == 0 && this is ContactsFragment && activity !is InsertOrEditContactActivity) ||
                (config.showTabs and FAVORITES_TAB_MASK == 0 && this is FavoritesFragment) ||
                (config.showTabs and GROUPS_TAB_MASK == 0 && this is GroupsFragment)) {
            return
        }

        if (config.lastUsedContactSource.isEmpty()) {
            val grouped = contacts.asSequence().groupBy { it.source }.maxWith(compareBy { it.value.size })
            config.lastUsedContactSource = grouped?.key ?: ""
        }

        allContacts = contacts

        val filtered = when {
            this is GroupsFragment -> contacts
            this is FavoritesFragment -> contacts.filter { it.starred == 1 } as ArrayList<Contact>
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
            setupGroupsAdapter(contacts) {
                groupsIgnoringSearch = (fragment_list?.adapter as? GroupsAdapter)?.groups ?: ArrayList()
            }
        } else {
            setupContactsFavoritesAdapter(contacts)
            contactsIgnoringSearch = (fragment_list?.adapter as? ContactsAdapter)?.contactItems ?: ArrayList()
        }
    }

    private fun setupGroupsAdapter(contacts: ArrayList<Contact>, callback: () -> Unit) {
        ContactsHelper(activity!!).getStoredGroups {
            var storedGroups = it
            contacts.forEach {
                it.groups.forEach {
                    val group = it
                    val storedGroup = storedGroups.firstOrNull { it.id == group.id }
                    storedGroup?.addContact()
                }
            }

            storedGroups = storedGroups.asSequence().sortedWith(compareBy { it.title.normalizeString() }).toMutableList() as ArrayList<Group>

            fragment_placeholder_2.beVisibleIf(storedGroups.isEmpty())
            fragment_placeholder.beVisibleIf(storedGroups.isEmpty())
            fragment_list.beVisibleIf(storedGroups.isNotEmpty())

            val currAdapter = fragment_list.adapter
            if (currAdapter == null) {
                GroupsAdapter(activity as SimpleActivity, storedGroups, activity as RefreshContactsListener, fragment_list, fragment_fastscroller) {
                    Intent(activity, GroupContactsActivity::class.java).apply {
                        putExtra(GROUP, it as Group)
                        activity!!.startActivity(this)
                    }
                }.apply {
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

            callback()
        }
    }

    private fun setupContactsFavoritesAdapter(contacts: ArrayList<Contact>) {
        setupViewVisibility(contacts.isNotEmpty())
        val currAdapter = fragment_list.adapter
        if (currAdapter == null || forceListRedraw) {
            forceListRedraw = false
            val location = when {
                activity is InsertOrEditContactActivity -> LOCATION_INSERT_OR_EDIT
                this is FavoritesFragment -> LOCATION_FAVORITES_TAB
                else -> LOCATION_CONTACTS_TAB
            }

            ContactsAdapter(activity as SimpleActivity, contacts, activity as RefreshContactsListener, location, null, fragment_list, fragment_fastscroller) {
                (activity as RefreshContactsListener).contactClicked(it as Contact)
            }.apply {
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
        val adapter = fragment_list.adapter
        if (adapter is ContactsAdapter) {
            val shouldNormalize = text.normalizeString() == text
            val convertLetters = config.showDialpadLetters
            val filtered = contactsIgnoringSearch.filter {
                getProperText(it.getNameToDisplay(), shouldNormalize).contains(text, true) ||
                        getProperText(it.nickname, shouldNormalize).contains(text, true) ||
                        it.doesContainPhoneNumber(text, convertLetters) ||
                        it.emails.any { it.value.contains(text, true) } ||
                        it.addresses.any { getProperText(it.value, shouldNormalize).contains(text, true) } ||
                        it.IMs.any { it.value.contains(text, true) } ||
                        getProperText(it.notes, shouldNormalize).contains(text, true) ||
                        getProperText(it.organization.company, shouldNormalize).contains(text, true) ||
                        getProperText(it.organization.jobPosition, shouldNormalize).contains(text, true) ||
                        it.websites.any { it.contains(text, true) }
            } as ArrayList

            filtered.sortBy {
                val nameToDisplay = it.getNameToDisplay()
                !getProperText(nameToDisplay, shouldNormalize).startsWith(text, true) && !nameToDisplay.contains(text, true)
            }

            if (filtered.isEmpty() && this@MyViewPagerFragment is FavoritesFragment) {
                fragment_placeholder.text = activity?.getString(R.string.no_contacts_found)
            }

            fragment_placeholder.beVisibleIf(filtered.isEmpty())
            (adapter as? ContactsAdapter)?.updateItems(filtered, text.normalizeString())
        } else if (adapter is GroupsAdapter) {
            val filtered = groupsIgnoringSearch.filter {
                it.title.contains(text, true)
            } as ArrayList

            if (filtered.isEmpty()) {
                fragment_placeholder.text = activity?.getString(R.string.no_items_found)
            }

            fragment_placeholder.beVisibleIf(filtered.isEmpty())
            (adapter as? GroupsAdapter)?.updateItems(filtered, text)
        }
    }

    fun onSearchOpened() {
        contactsIgnoringSearch = (fragment_list?.adapter as? ContactsAdapter)?.contactItems ?: ArrayList()
        groupsIgnoringSearch = (fragment_list?.adapter as? GroupsAdapter)?.groups ?: ArrayList()
    }

    fun onSearchClosed() {
        if (fragment_list.adapter is ContactsAdapter) {
            (fragment_list.adapter as? ContactsAdapter)?.updateItems(contactsIgnoringSearch)
            setupViewVisibility(contactsIgnoringSearch.isNotEmpty())
        } else if (fragment_list.adapter is GroupsAdapter) {
            (fragment_list.adapter as? GroupsAdapter)?.updateItems(groupsIgnoringSearch)
            setupViewVisibility(groupsIgnoringSearch.isNotEmpty())
        }

        if (this is FavoritesFragment) {
            fragment_placeholder.text = activity?.getString(R.string.no_favorites)
        }
    }

    private fun updateViewStuff() {
        context.updateTextColors(fragment_wrapper.parent as ViewGroup)
        fragment_fastscroller.updateBubbleColors()
        fragment_fastscroller.allowBubbleDisplay = config.showInfoBubble
        fragment_placeholder_2?.setTextColor(context.getAdjustedPrimaryColor())
    }

    private fun setupViewVisibility(hasItemsToShow: Boolean) {
        fragment_placeholder_2?.beVisibleIf(!hasItemsToShow)
        fragment_placeholder?.beVisibleIf(!hasItemsToShow)
        fragment_list.beVisibleIf(hasItemsToShow)
    }

    abstract fun fabClicked()

    abstract fun placeholderClicked()
}
