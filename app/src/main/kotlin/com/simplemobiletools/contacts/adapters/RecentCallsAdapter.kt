package com.simplemobiletools.contacts.adapters

import android.view.Menu
import android.view.View
import android.view.ViewGroup
import com.simplemobiletools.commons.adapters.MyRecyclerViewAdapter
import com.simplemobiletools.commons.dialogs.ConfirmationDialog
import com.simplemobiletools.commons.extensions.beVisibleIf
import com.simplemobiletools.commons.views.FastScroller
import com.simplemobiletools.commons.views.MyRecyclerView
import com.simplemobiletools.contacts.R
import com.simplemobiletools.contacts.activities.SimpleActivity
import com.simplemobiletools.contacts.extensions.config
import com.simplemobiletools.contacts.helpers.ContactsHelper
import com.simplemobiletools.contacts.helpers.RECENTS_TAB_MASK
import com.simplemobiletools.contacts.interfaces.RefreshContactsListener
import com.simplemobiletools.contacts.models.RecentCall
import kotlinx.android.synthetic.main.item_recent_call.view.*
import java.util.*

class RecentCallsAdapter(activity: SimpleActivity, var recentCalls: ArrayList<RecentCall>, val refreshListener: RefreshContactsListener?, recyclerView: MyRecyclerView,
                         fastScroller: FastScroller, itemClick: (Any) -> Unit) : MyRecyclerViewAdapter(activity, recyclerView, fastScroller, itemClick) {
    private val showPhoneNumbers = activity.config.showPhoneNumbers

    init {
        setupDragListener(true)
    }

    override fun getActionMenuId() = R.menu.cab_recent_calls

    override fun prepareActionMode(menu: Menu) {}

    override fun actionItemPressed(id: Int) {
        if (selectedKeys.isEmpty()) {
            return
        }

        when (id) {
            R.id.cab_select_all -> selectAll()
            R.id.cab_delete -> askConfirmDelete()
        }
    }

    override fun getSelectableItemCount() = recentCalls.size

    override fun getIsItemSelectable(position: Int) = true

    override fun getItemSelectionKey(position: Int) = recentCalls.getOrNull(position)?.id

    override fun getItemKeyPosition(key: Int) = recentCalls.indexOfFirst { it.id == key }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = createViewHolder(R.layout.item_recent_call, parent)

    override fun onBindViewHolder(holder: MyRecyclerViewAdapter.ViewHolder, position: Int) {
        val recentCall = recentCalls[position]
        holder.bindView(recentCall, true, true) { itemView, layoutPosition ->
            setupView(itemView, recentCall)
        }
        bindViewHolder(holder)
    }

    override fun getItemCount() = recentCalls.size

    fun updateItems(newItems: ArrayList<RecentCall>) {
        recentCalls = newItems
        notifyDataSetChanged()
        finishActMode()
        fastScroller?.measureRecyclerView()
    }

    private fun askConfirmDelete() {
        ConfirmationDialog(activity) {
            deleteRecentCalls()
        }
    }

    private fun deleteRecentCalls() {
        if (selectedKeys.isEmpty()) {
            return
        }

        val callsToRemove = getSelectedItems()
        val positions = getSelectedItemPositions(callsToRemove.map { it.id })
        ContactsHelper(activity).removeRecentCalls(callsToRemove.map { it.id } as ArrayList<Int>)
        recentCalls.removeAll(callsToRemove)

        if (recentCalls.isEmpty()) {
            refreshListener?.refreshContacts(RECENTS_TAB_MASK)
            finishActMode()
        } else {
            removeSelectedItems(positions)
        }
    }

    private fun getSelectedItems() = recentCalls.filter { selectedKeys.contains(it.id) } as ArrayList<RecentCall>

    private fun setupView(view: View, recentCall: RecentCall) {
        view.apply {
            recent_call_frame?.isSelected = selectedKeys.contains(recentCall.id)
            recent_call_name.apply {
                text = recentCall.name ?: recentCall.number
                setTextColor(textColor)
            }

            recent_call_number.apply {
                beVisibleIf(showPhoneNumbers && recentCall.name != null)
                text = recentCall.number
                setTextColor(textColor)
            }

            recent_call_date_time.apply {
                text = recentCall.dateTime
                setTextColor(textColor)
            }
        }
    }
}
