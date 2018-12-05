package com.simplemobiletools.contacts.pro.adapters

import android.view.Menu
import android.view.View
import android.view.ViewGroup
import com.simplemobiletools.commons.activities.BaseSimpleActivity
import com.simplemobiletools.commons.adapters.MyRecyclerViewAdapter
import com.simplemobiletools.commons.interfaces.RefreshRecyclerViewListener
import com.simplemobiletools.commons.views.MyRecyclerView
import com.simplemobiletools.contacts.pro.R
import com.simplemobiletools.contacts.pro.extensions.config
import com.simplemobiletools.contacts.pro.extensions.deleteBlockedNumber
import com.simplemobiletools.contacts.pro.models.BlockedNumber
import kotlinx.android.synthetic.main.item_manage_blocked_number.view.*
import java.util.*

class ManageBlockedNumbersAdapter(activity: BaseSimpleActivity, var blockedNumbers: ArrayList<BlockedNumber>, val listener: RefreshRecyclerViewListener?,
                                  recyclerView: MyRecyclerView, itemClick: (Any) -> Unit) : MyRecyclerViewAdapter(activity, recyclerView, null, itemClick) {

    private val config = activity.config

    init {
        setupDragListener(true)
    }

    override fun getActionMenuId() = R.menu.cab_remove_only

    override fun prepareActionMode(menu: Menu) {}

    override fun actionItemPressed(id: Int) {
        when (id) {
            R.id.cab_remove -> removeSelection()
        }
    }

    override fun getSelectableItemCount() = blockedNumbers.size

    override fun getIsItemSelectable(position: Int) = true

    override fun getItemSelectionKey(position: Int) = blockedNumbers.getOrNull(position)?.id?.toInt()

    override fun getItemKeyPosition(key: Int) = blockedNumbers.indexOfFirst { it.id.toInt() == key }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = createViewHolder(R.layout.item_manage_blocked_number, parent)

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val blockedNumber = blockedNumbers[position]
        holder.bindView(blockedNumber, true, true) { itemView, adapterPosition ->
            setupView(itemView, blockedNumber)
        }
        bindViewHolder(holder)
    }

    override fun getItemCount() = blockedNumbers.size

    private fun getSelectedItems() = blockedNumbers.filter { selectedKeys.contains(it.id.toInt()) } as ArrayList<BlockedNumber>

    private fun setupView(view: View, blockedNumber: BlockedNumber) {
        view.apply {
            manage_blocked_number_holder?.isSelected = selectedKeys.contains(blockedNumber.id.toInt())
            manage_blocked_number_title.apply {
                text = blockedNumber.number
                setTextColor(config.textColor)
            }
        }
    }

    private fun removeSelection() {
        val removeBlockedNumbers = ArrayList<BlockedNumber>(selectedKeys.size)
        val positions = getSelectedItemPositions()

        getSelectedItems().forEach {
            removeBlockedNumbers.add(it)
            activity.deleteBlockedNumber(it.number)
        }

        blockedNumbers.removeAll(removeBlockedNumbers)
        removeSelectedItems(positions)
        if (blockedNumbers.isEmpty()) {
            listener?.refreshItems()
        }
    }
}
