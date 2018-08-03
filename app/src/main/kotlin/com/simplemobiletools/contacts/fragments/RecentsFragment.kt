package com.simplemobiletools.contacts.fragments

import android.content.Context
import android.util.AttributeSet
import com.simplemobiletools.commons.extensions.beVisibleIf
import com.simplemobiletools.commons.extensions.isActivityDestroyed
import com.simplemobiletools.contacts.adapters.RecentCallsAdapter
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
            RecentCallsAdapter(activity!!, recentCalls, fragment_list, fragment_fastscroller) {

            }.apply {
                addVerticalDividers(true)
                fragment_list.adapter = this
            }

            fragment_fastscroller.setViews(fragment_list) {}
        } else {
            (currAdapter as RecentCallsAdapter).updateItems(recentCalls)
        }
    }
}
