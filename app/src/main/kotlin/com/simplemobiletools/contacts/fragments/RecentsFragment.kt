package com.simplemobiletools.contacts.fragments

import android.content.Context
import android.util.AttributeSet
import com.simplemobiletools.commons.extensions.beVisibleIf
import com.simplemobiletools.commons.extensions.isActivityDestroyed
import com.simplemobiletools.contacts.adapters.RecentCallsAdapter
import com.simplemobiletools.contacts.extensions.contactClicked
import com.simplemobiletools.contacts.helpers.PHONE_NUMBER_PATTERN
import com.simplemobiletools.contacts.models.Contact
import com.simplemobiletools.contacts.models.RecentCall
import kotlinx.android.synthetic.main.fragment_layout.view.*

class RecentsFragment(context: Context, attributeSet: AttributeSet) : MyViewPagerFragment(context, attributeSet) {
    override fun fabClicked() {}

    override fun placeholderClicked() {}

    fun updateRecentCalls(recentCalls: ArrayList<RecentCall>) {
        if (activity == null || activity!!.isActivityDestroyed()) {
            return
        }

        fragment_placeholder.beVisibleIf(recentCalls.isEmpty())
        fragment_list.beVisibleIf(recentCalls.isNotEmpty())

        val currAdapter = fragment_list.adapter
        if (currAdapter == null) {
            RecentCallsAdapter(activity!!, recentCalls, activity, fragment_list, fragment_fastscroller) {
                val recentCall = (it as RecentCall).number.replace(PHONE_NUMBER_PATTERN.toRegex(), "")
                var selectedContact: Contact? = null
                for (contact in allContacts) {
                    if (contact.phoneNumbers.any { it.value.replace(PHONE_NUMBER_PATTERN.toRegex(), "") == recentCall }) {
                        selectedContact = contact
                        break
                    }
                }

                if (selectedContact != null) {
                    activity?.contactClicked(selectedContact)
                }
            }.apply {
                addVerticalDividers(true)
                fragment_list.adapter = this
            }

            fragment_fastscroller.setViews(fragment_list) {
                val item = (fragment_list.adapter as RecentCallsAdapter).recentCalls.getOrNull(it)
                fragment_fastscroller.updateBubbleText(item?.name ?: item?.number ?: "")
            }
        } else {
            (currAdapter as RecentCallsAdapter).updateItems(recentCalls)
        }
    }
}
