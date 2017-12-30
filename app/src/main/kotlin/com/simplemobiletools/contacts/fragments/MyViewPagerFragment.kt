package com.simplemobiletools.contacts.fragments

import android.content.Context
import android.graphics.Paint
import android.support.design.widget.CoordinatorLayout
import android.util.AttributeSet
import com.simplemobiletools.commons.extensions.beVisibleIf
import com.simplemobiletools.commons.extensions.isActivityDestroyed
import com.simplemobiletools.commons.extensions.toast
import com.simplemobiletools.commons.extensions.updateTextColors
import com.simplemobiletools.commons.helpers.SORT_BY_FIRST_NAME
import com.simplemobiletools.commons.helpers.SORT_BY_SURNAME
import com.simplemobiletools.contacts.R
import com.simplemobiletools.contacts.activities.MainActivity
import com.simplemobiletools.contacts.activities.SimpleActivity
import com.simplemobiletools.contacts.adapters.ContactsAdapter
import com.simplemobiletools.contacts.extensions.config
import com.simplemobiletools.contacts.extensions.openContact
import com.simplemobiletools.contacts.extensions.tryStartCall
import com.simplemobiletools.contacts.helpers.Config
import com.simplemobiletools.contacts.helpers.ContactsHelper
import com.simplemobiletools.contacts.models.Contact
import kotlinx.android.synthetic.main.fragment_layout.view.*

abstract class MyViewPagerFragment(context: Context, attributeSet: AttributeSet) : CoordinatorLayout(context, attributeSet) {
    protected var activity: MainActivity? = null
    private var lastHashCode = 0
    private var contactsIgnoringSearch = ArrayList<Contact>()
    lateinit private var config: Config

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

            fragment_placeholder_2.paintFlags = fragment_placeholder_2.paintFlags or Paint.UNDERLINE_TEXT_FLAG
            updateViewStuff()

            if (this is FavoritesFragment) {
                fragment_placeholder.text = activity.getString(R.string.no_favorites)
                fragment_placeholder_2.text = activity.getString(R.string.add_favorites)
            }
        }

        initContacts()
    }

    fun textColorChanged(color: Int) {
        (fragment_list.adapter as ContactsAdapter).apply {
            updateTextColor(color)
            initDrawables()
        }
    }

    fun primaryColorChanged(color: Int) {
        fragment_fastscroller.updatePrimaryColor()
        fragment_fab.setColors(config.textColor, color, config.backgroundColor)
        fragment_fastscroller.updateBubblePrimaryColor()
    }

    fun startNameWithSurnameChanged(startNameWithSurname: Boolean) {
        (fragment_list.adapter as ContactsAdapter).apply {
            config.sorting = if (startNameWithSurname) SORT_BY_SURNAME else SORT_BY_FIRST_NAME
            initContacts()
        }
    }

    fun initContacts() {
        if (activity == null || activity!!.isActivityDestroyed()) {
            return
        }

        ContactsHelper(activity!!).getContacts {
            var contacts = it
            if (activity == null || activity!!.isActivityDestroyed()) {
                return@getContacts
            }

            if (config.lastUsedContactSource.isEmpty()) {
                val grouped = contacts.groupBy { it.source }.maxWith(compareBy { it.value.size })
                config.lastUsedContactSource = grouped?.key ?: ""
            }

            Contact.sorting = config.sorting
            contacts.sort()

            contacts = if (this is FavoritesFragment) {
                val favorites = config.favorites
                contacts.filter { favorites.contains(it.id.toString()) } as ArrayList<Contact>
            } else {
                val contactSources = config.displayContactSources
                if (config.showAllContacts()) {
                    contacts
                } else {
                    contacts.filter { contactSources.contains(it.source) } as ArrayList<Contact>
                }
            }

            if (contacts.hashCode() != lastHashCode) {
                lastHashCode = contacts.hashCode()
                activity!!.runOnUiThread {
                    setupContacts(contacts)
                }
            }
        }
    }

    private fun setupContacts(contacts: ArrayList<Contact>) {
        fragment_placeholder_2.beVisibleIf(contacts.isEmpty())
        fragment_placeholder.beVisibleIf(contacts.isEmpty())

        val currAdapter = fragment_list.adapter
        if (currAdapter == null) {
            ContactsAdapter(activity as SimpleActivity, contacts, activity, this is FavoritesFragment, fragment_list) {
                if (config.callContact) {
                    val contact = it as Contact
                    if (contact.phoneNumbers.isNotEmpty()) {
                        (activity as SimpleActivity).tryStartCall(it)
                    } else {
                        activity!!.toast(R.string.no_phone_number_found)
                    }
                } else {
                    context!!.openContact(it as Contact)
                }
            }.apply {
                setupDragListener(true)
                addVerticalDividers(true)
                fragment_list.adapter = this
            }

            fragment_fastscroller.setViews(fragment_list) {
                val item = (fragment_list.adapter as ContactsAdapter).contactItems.getOrNull(it)
                fragment_fastscroller.updateBubbleText(item?.getBubbleText() ?: "")
            }
        } else {
            (currAdapter as ContactsAdapter).apply {
                startNameWithSurname = config.startNameWithSurname
                showPhoneNumbers = config.showPhoneNumbers
                updateItems(contacts)
            }
        }
    }

    fun onActivityResume() {
        updateViewStuff()
    }

    fun finishActMode() {
        (fragment_list.adapter as? ContactsAdapter)?.finishActMode()
    }

    fun onSearchQueryChanged(text: String) {
        (fragment_list.adapter as ContactsAdapter).apply {
            val filtered = contactsIgnoringSearch.filter { it.getFullName(startNameWithSurname).contains(text, true) } as ArrayList
            updateItems(filtered)
        }
    }

    fun onSearchOpened() {
        contactsIgnoringSearch = (fragment_list.adapter as ContactsAdapter).contactItems as ArrayList
    }

    private fun updateViewStuff() {
        context.updateTextColors(fragment_wrapper)
        fragment_fastscroller.updateBubbleColors()
        fragment_fastscroller.allowBubbleDisplay = config.showInfoBubble
        fragment_placeholder_2.setTextColor(config.primaryColor)
    }

    abstract fun fabClicked()

    abstract fun placeholderClicked()
}
