package com.simplemobiletools.contacts.adapters

import android.view.Menu
import android.view.View
import android.view.ViewGroup
import com.simplemobiletools.commons.adapters.MyRecyclerViewAdapter
import com.simplemobiletools.commons.dialogs.ConfirmationDialog
import com.simplemobiletools.commons.extensions.applyColorFilter
import com.simplemobiletools.commons.extensions.beVisibleIf
import com.simplemobiletools.commons.views.FastScroller
import com.simplemobiletools.commons.views.MyRecyclerView
import com.simplemobiletools.contacts.R
import com.simplemobiletools.contacts.activities.SimpleActivity
import com.simplemobiletools.contacts.dialogs.RenameGroupDialog
import com.simplemobiletools.contacts.extensions.config
import com.simplemobiletools.contacts.extensions.dbHelper
import com.simplemobiletools.contacts.helpers.ContactsHelper
import com.simplemobiletools.contacts.helpers.GROUPS_TAB_MASK
import com.simplemobiletools.contacts.interfaces.RefreshContactsListener
import com.simplemobiletools.contacts.models.Group
import kotlinx.android.synthetic.main.item_group.view.*
import java.util.*

class GroupsAdapter(activity: SimpleActivity, var groups: ArrayList<Group>, val refreshListener: RefreshContactsListener?, recyclerView: MyRecyclerView,
                    fastScroller: FastScroller, itemClick: (Any) -> Unit) : MyRecyclerViewAdapter(activity, recyclerView, fastScroller, itemClick) {

    private var smallPadding = activity.resources.getDimension(R.dimen.small_margin).toInt()
    private var bigPadding = activity.resources.getDimension(R.dimen.normal_margin).toInt()

    var showContactThumbnails = activity.config.showContactThumbnails

    init {
        setupDragListener(true)
    }

    override fun getActionMenuId() = R.menu.cab_groups

    override fun prepareActionMode(menu: Menu) {
        menu.apply {
            findItem(R.id.cab_rename).isVisible = isOneItemSelected()
        }
    }

    override fun prepareItemSelection(viewHolder: ViewHolder) {}

    override fun markViewHolderSelection(select: Boolean, viewHolder: ViewHolder?) {
        viewHolder?.itemView?.group_frame?.isSelected = select
    }

    override fun actionItemPressed(id: Int) {
        if (selectedPositions.isEmpty()) {
            return
        }

        when (id) {
            R.id.cab_rename -> renameGroup()
            R.id.cab_select_all -> selectAll()
            R.id.cab_delete -> askConfirmDelete()
        }
    }

    override fun getSelectableItemCount() = groups.size

    override fun getIsItemSelectable(position: Int) = true

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = createViewHolder(R.layout.item_group, parent)

    override fun onBindViewHolder(holder: MyRecyclerViewAdapter.ViewHolder, position: Int) {
        val group = groups[position]
        val view = holder.bindView(group, true, true) { itemView, layoutPosition ->
            setupView(itemView, group)
        }
        bindViewHolder(holder, position, view)
    }

    override fun getItemCount() = groups.size

    fun updateItems(newItems: ArrayList<Group>) {
        groups = newItems
        notifyDataSetChanged()
        finishActMode()
        fastScroller?.measureRecyclerView()
    }

    private fun renameGroup() {
        RenameGroupDialog(activity, groups[selectedPositions.first()]) {
            finishActMode()
            refreshListener?.refreshContacts(GROUPS_TAB_MASK)
        }
    }

    private fun askConfirmDelete() {
        ConfirmationDialog(activity) {
            deleteGroups()
        }
    }

    private fun deleteGroups() {
        if (selectedPositions.isEmpty()) {
            return
        }

        val groupsToRemove = ArrayList<Group>()
        selectedPositions.sortedDescending().forEach {
            val group = groups[it]
            groupsToRemove.add(group)
            if (group.isPrivateSecretGroup()) {
                activity.dbHelper.deleteGroup(group.id)
            } else {
                ContactsHelper(activity).deleteGroup(group.id)
            }
        }
        groups.removeAll(groupsToRemove)

        if (groups.isEmpty()) {
            refreshListener?.refreshContacts(GROUPS_TAB_MASK)
            finishActMode()
        } else {
            removeSelectedItems()
        }
    }

    private fun setupView(view: View, group: Group) {
        view.apply {
            group_name.apply {
                setTextColor(textColor)
                text = String.format(activity.getString(R.string.groups_placeholder), group.title, group.contactsCount.toString())
                setPadding(if (showContactThumbnails) smallPadding else bigPadding, smallPadding, smallPadding, 0)
            }

            group_tmb.beVisibleIf(showContactThumbnails)
            if (showContactThumbnails) {
                group_tmb.applyColorFilter(textColor)
            }
        }
    }
}
