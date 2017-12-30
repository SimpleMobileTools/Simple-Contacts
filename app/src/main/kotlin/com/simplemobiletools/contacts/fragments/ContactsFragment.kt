package com.simplemobiletools.contacts.fragments

import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.util.AttributeSet
import com.simplemobiletools.commons.extensions.beVisibleIf
import com.simplemobiletools.commons.extensions.isActivityDestroyed
import com.simplemobiletools.commons.extensions.toast
import com.simplemobiletools.commons.extensions.updateTextColors
import com.simplemobiletools.commons.helpers.SORT_BY_FIRST_NAME
import com.simplemobiletools.commons.helpers.SORT_BY_SURNAME
import com.simplemobiletools.commons.interfaces.RefreshRecyclerViewListener
import com.simplemobiletools.contacts.R
import com.simplemobiletools.contacts.activities.ContactActivity
import com.simplemobiletools.contacts.activities.MainActivity
import com.simplemobiletools.contacts.activities.SimpleActivity
import com.simplemobiletools.contacts.adapters.ContactsAdapter
import com.simplemobiletools.contacts.extensions.openContact
import com.simplemobiletools.contacts.extensions.tryStartCall
import com.simplemobiletools.contacts.helpers.ContactsHelper
import com.simplemobiletools.contacts.models.Contact
import kotlinx.android.synthetic.main.fragment_contacts.view.*

class ContactsFragment(context: Context, attributeSet: AttributeSet) : MyViewPagerFragment(context, attributeSet), RefreshRecyclerViewListener {
    var activity: MainActivity? = null

    override fun initFragment(activity: MainActivity) {
        if (this.activity == null) {
            this.activity = activity
            contacts_fab.setOnClickListener {
                addNewContact()
            }

            contacts_placeholder_2.setOnClickListener {
                activity.showFilterDialog()
            }

            contacts_placeholder_2.paintFlags = contacts_placeholder_2.paintFlags or Paint.UNDERLINE_TEXT_FLAG
            updateViewStuff()
        }

        initContacts()
    }

    override fun textColorChanged(color: Int) {
        (contacts_list.adapter as ContactsAdapter).apply {
            updateTextColor(color)
            initDrawables()
        }
    }

    override fun primaryColorChanged(color: Int) {
        contacts_fastscroller.updatePrimaryColor()
        contacts_fab.setColors(config.textColor, color, config.backgroundColor)
        contacts_fastscroller.updateBubblePrimaryColor()
    }

    override fun startNameWithSurnameChanged(startNameWithSurname: Boolean) {
        (contacts_list.adapter as ContactsAdapter).apply {
            config.sorting = if (startNameWithSurname) SORT_BY_SURNAME else SORT_BY_FIRST_NAME
            initContacts()
        }
    }

    override fun onActivityResume() {
        updateViewStuff()
    }

    private fun updateViewStuff() {
        context.updateTextColors(contacts_fragment)
        contacts_fastscroller.updateBubbleColors()
        contacts_fastscroller.allowBubbleDisplay = config.showInfoBubble
        contacts_placeholder_2.setTextColor(config.primaryColor)
    }

    fun initContacts() {
        if (activity == null || activity!!.isActivityDestroyed()) {
            return
        }

        ContactsHelper(activity!!).getContacts {
            if (activity == null || activity!!.isActivityDestroyed()) {
                return@getContacts
            }

            if (config.lastUsedContactSource.isEmpty()) {
                val grouped = it.groupBy { it.source }.maxWith(compareBy { it.value.size })
                config.lastUsedContactSource = grouped?.key ?: ""
            }

            Contact.sorting = config.sorting
            it.sort()

            if (it.hashCode() != (contacts_list.adapter as? ContactsAdapter)?.contactItems?.hashCode()) {
                activity!!.runOnUiThread {
                    setupContacts(it)
                }
            }
        }
    }

    private fun setupContacts(contacts: ArrayList<Contact>) {
        contacts_placeholder_2.beVisibleIf(contacts.isEmpty())
        contacts_placeholder.beVisibleIf(contacts.isEmpty())

        val currAdapter = contacts_list.adapter
        if (currAdapter == null) {
            ContactsAdapter(activity as SimpleActivity, contacts, this, contacts_list) {
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
                contacts_list.adapter = this
            }

            contacts_fastscroller.setViews(contacts_list) {
                val item = (contacts_list.adapter as ContactsAdapter).contactItems.getOrNull(it)
                contacts_fastscroller.updateBubbleText(item?.getBubbleText() ?: "")
            }
        } else {
            (currAdapter as ContactsAdapter).apply {
                startNameWithSurname = config.startNameWithSurname
                showPhoneNumbers = config.showPhoneNumbers
                updateItems(contacts)
            }
        }
    }

    private fun addNewContact() {
        Intent(context, ContactActivity::class.java).apply {
            context.startActivity(this)
        }
    }

    override fun refreshItems() {
        initContacts()
    }
}
