package com.simplemobiletools.contacts.pro.fragments

import android.content.Context
import android.content.Intent
import android.util.AttributeSet
import com.simplemobiletools.commons.extensions.areSystemAnimationsEnabled
import com.simplemobiletools.commons.extensions.hideKeyboard
import com.simplemobiletools.commons.models.contacts.Contact
import com.simplemobiletools.contacts.pro.activities.EditContactActivity
import com.simplemobiletools.contacts.pro.activities.InsertOrEditContactActivity
import com.simplemobiletools.contacts.pro.activities.MainActivity
import com.simplemobiletools.contacts.pro.activities.SimpleActivity
import com.simplemobiletools.contacts.pro.adapters.ContactsAdapter
import com.simplemobiletools.contacts.pro.databinding.FragmentContactsBinding
import com.simplemobiletools.contacts.pro.databinding.FragmentLettersLayoutBinding
import com.simplemobiletools.contacts.pro.extensions.config
import com.simplemobiletools.contacts.pro.helpers.LOCATION_CONTACTS_TAB
import com.simplemobiletools.contacts.pro.interfaces.RefreshContactsListener

class ContactsFragment(context: Context, attributeSet: AttributeSet) : MyViewPagerFragment<MyViewPagerFragment.LetterLayout>(context, attributeSet) {

    private lateinit var binding: FragmentContactsBinding

    override fun onFinishInflate() {
        super.onFinishInflate()
        binding = FragmentContactsBinding.bind(this)
        innerBinding = LetterLayout(FragmentLettersLayoutBinding.bind(binding.root))
    }

    override fun fabClicked() {
        activity?.hideKeyboard()
        Intent(context, EditContactActivity::class.java).apply {
            context.startActivity(this)
        }
    }

    override fun placeholderClicked() {
        if (activity is MainActivity) {
            (activity as MainActivity).showFilterDialog()
        } else if (activity is InsertOrEditContactActivity) {
            (activity as InsertOrEditContactActivity).showFilterDialog()
        }
    }

    fun setupContactsAdapter(contacts: List<Contact>) {
        setupViewVisibility(contacts.isNotEmpty())
        val currAdapter = innerBinding.fragmentList.adapter

        if (currAdapter == null || forceListRedraw) {
            forceListRedraw = false
            val location = LOCATION_CONTACTS_TAB

            ContactsAdapter(
                activity = activity as SimpleActivity,
                contactItems = contacts.toMutableList(),
                refreshListener = activity as RefreshContactsListener,
                location = location,
                removeListener = null,
                recyclerView = innerBinding.fragmentList,
                enableDrag = false,
            ) {
                (activity as RefreshContactsListener).contactClicked(it as Contact)
            }.apply {
                innerBinding.fragmentList.adapter = this
            }

            if (context.areSystemAnimationsEnabled) {
                innerBinding.fragmentList.scheduleLayoutAnimation()
            }
        } else {
            (currAdapter as ContactsAdapter).apply {
                startNameWithSurname = context.config.startNameWithSurname
                showPhoneNumbers = context.config.showPhoneNumbers
                showContactThumbnails = context.config.showContactThumbnails
                updateItems(contacts)
            }
        }
    }
}
