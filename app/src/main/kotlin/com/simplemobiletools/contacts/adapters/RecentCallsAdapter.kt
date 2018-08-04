package com.simplemobiletools.contacts.adapters

import android.view.Menu
import android.view.View
import android.view.ViewGroup
import com.simplemobiletools.commons.adapters.MyRecyclerViewAdapter
import com.simplemobiletools.commons.extensions.beVisibleIf
import com.simplemobiletools.commons.views.FastScroller
import com.simplemobiletools.commons.views.MyRecyclerView
import com.simplemobiletools.contacts.R
import com.simplemobiletools.contacts.activities.SimpleActivity
import com.simplemobiletools.contacts.extensions.config
import com.simplemobiletools.contacts.models.RecentCall
import kotlinx.android.synthetic.main.item_recent_call.view.*
import java.util.*

class RecentCallsAdapter(activity: SimpleActivity, var recentCalls: ArrayList<RecentCall>, recyclerView: MyRecyclerView, fastScroller: FastScroller,
                         itemClick: (Any) -> Unit) : MyRecyclerViewAdapter(activity, recyclerView, fastScroller, itemClick) {
    private val showPhoneNumbers = activity.config.showPhoneNumbers

    init {
        setupDragListener(true)
    }

    override fun getActionMenuId() = R.menu.cab_recent_calls

    override fun prepareActionMode(menu: Menu) {}

    override fun prepareItemSelection(viewHolder: ViewHolder) {}

    override fun markViewHolderSelection(select: Boolean, viewHolder: ViewHolder?) {
        viewHolder?.itemView?.recent_call_frame?.isSelected = select
    }

    override fun actionItemPressed(id: Int) {
        if (selectedPositions.isEmpty()) {
            return
        }

        when (id) {
            R.id.cab_select_all -> selectAll()
        }
    }

    override fun getSelectableItemCount() = recentCalls.size

    override fun getIsItemSelectable(position: Int) = true

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = createViewHolder(R.layout.item_recent_call, parent)

    override fun onBindViewHolder(holder: MyRecyclerViewAdapter.ViewHolder, position: Int) {
        val recentCall = recentCalls[position]
        val view = holder.bindView(recentCall, true, true) { itemView, layoutPosition ->
            setupView(itemView, recentCall)
        }
        bindViewHolder(holder, position, view)
    }

    override fun getItemCount() = recentCalls.size

    fun updateItems(newItems: ArrayList<RecentCall>) {
        recentCalls = newItems
        notifyDataSetChanged()
        finishActMode()
        fastScroller?.measureRecyclerView()
    }

    private fun setupView(view: View, recentCall: RecentCall) {
        view.apply {
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
