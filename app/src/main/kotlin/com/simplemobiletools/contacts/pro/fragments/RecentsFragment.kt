package com.simplemobiletools.contacts.pro.fragments

import android.annotation.TargetApi
import android.content.Context
import android.content.Intent
import android.os.Build
import android.telecom.TelecomManager
import android.util.AttributeSet
import com.simplemobiletools.commons.extensions.beVisibleIf
import com.simplemobiletools.commons.extensions.hasPermission
import com.simplemobiletools.commons.helpers.PERMISSION_READ_CALL_LOG
import com.simplemobiletools.commons.helpers.PERMISSION_WRITE_CALL_LOG
import com.simplemobiletools.commons.helpers.isMarshmallowPlus
import com.simplemobiletools.contacts.pro.activities.EditContactActivity
import com.simplemobiletools.contacts.pro.adapters.RecentCallsAdapter
import com.simplemobiletools.contacts.pro.extensions.contactClicked
import com.simplemobiletools.contacts.pro.extensions.normalizeNumber
import com.simplemobiletools.contacts.pro.extensions.telecomManager
import com.simplemobiletools.contacts.pro.helpers.IS_FROM_SIMPLE_CONTACTS
import com.simplemobiletools.contacts.pro.helpers.KEY_PHONE
import com.simplemobiletools.contacts.pro.helpers.RECENTS_TAB_MASK
import com.simplemobiletools.contacts.pro.models.Contact
import com.simplemobiletools.contacts.pro.models.RecentCall
import kotlinx.android.synthetic.main.fragment_layout.view.*

class RecentsFragment(context: Context, attributeSet: AttributeSet) : MyViewPagerFragment(context, attributeSet) {
    override fun fabClicked() {}

    @TargetApi(Build.VERSION_CODES.M)
    override fun placeholderClicked() {
        if (!isMarshmallowPlus() || (isMarshmallowPlus() && context.telecomManager.defaultDialerPackage == context.packageName)) {
            activity!!.handlePermission(PERMISSION_WRITE_CALL_LOG) {
                if (it) {
                    activity!!.handlePermission(PERMISSION_READ_CALL_LOG) {
                        activity?.refreshContacts(RECENTS_TAB_MASK)
                    }
                }
            }
        } else {
            val intent = Intent(TelecomManager.ACTION_CHANGE_DEFAULT_DIALER).putExtra(TelecomManager.EXTRA_CHANGE_DEFAULT_DIALER_PACKAGE_NAME, context.packageName)
            context.startActivity(intent)
        }
    }

    fun updateRecentCalls(recentCalls: ArrayList<RecentCall>) {
        if (activity == null || activity!!.isDestroyed) {
            return
        }

        fragment_placeholder.beVisibleIf(recentCalls.isEmpty())
        fragment_placeholder_2.beVisibleIf(recentCalls.isEmpty() && !activity!!.hasPermission(PERMISSION_WRITE_CALL_LOG))
        fragment_list.beVisibleIf(recentCalls.isNotEmpty())

        val currAdapter = fragment_list.adapter
        if (currAdapter == null) {
            RecentCallsAdapter(activity!!, recentCalls, activity, fragment_list, fragment_fastscroller) {
                val recentCall = (it as RecentCall).number.normalizeNumber()
                var selectedContact: Contact? = null
                for (contact in allContacts) {
                    if (contact.phoneNumbers.any { it.normalizedNumber == recentCall }) {
                        selectedContact = contact
                        break
                    }
                }

                if (selectedContact != null) {
                    activity?.contactClicked(selectedContact)
                } else {
                    Intent(context, EditContactActivity::class.java).apply {
                        action = Intent.ACTION_INSERT
                        putExtra(KEY_PHONE, recentCall)
                        putExtra(IS_FROM_SIMPLE_CONTACTS, true)
                        context.startActivity(this)
                    }
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
