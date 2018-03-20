package com.simplemobiletools.contacts.fragments

import android.content.Context
import android.content.Intent
import android.support.design.widget.CoordinatorLayout
import android.util.AttributeSet
import android.view.ViewGroup
import com.simplemobiletools.commons.extensions.beVisibleIf
import com.simplemobiletools.commons.extensions.getAdjustedPrimaryColor
import com.simplemobiletools.commons.extensions.underlineText
import com.simplemobiletools.commons.extensions.updateTextColors
import com.simplemobiletools.contacts.activities.GroupContactsActivity
import com.simplemobiletools.contacts.activities.MainActivity
import com.simplemobiletools.contacts.activities.SimpleActivity
import com.simplemobiletools.contacts.adapters.GroupsAdapter
import com.simplemobiletools.contacts.dialogs.CreateNewGroupDialog
import com.simplemobiletools.contacts.extensions.config
import com.simplemobiletools.contacts.helpers.ContactsHelper
import com.simplemobiletools.contacts.helpers.GROUP
import com.simplemobiletools.contacts.interfaces.FragmentInterface
import com.simplemobiletools.contacts.models.Contact
import com.simplemobiletools.contacts.models.Group
import kotlinx.android.synthetic.main.fragment_groups.view.*

class GroupsFragment(context: Context, attributeSet: AttributeSet) : CoordinatorLayout(context, attributeSet), FragmentInterface {
    private var activity: MainActivity? = null
    private var lastContacts = ArrayList<Contact>()

    override fun setupFragment(activity: MainActivity) {
        if (this.activity == null) {
            this.activity = activity
            groups_fab.setOnClickListener {
                CreateNewGroupDialog(activity) {
                    refreshContacts(lastContacts)
                }
            }

            groups_placeholder_2.setOnClickListener {
                CreateNewGroupDialog(activity) {
                    refreshContacts(lastContacts)
                }
            }

            groups_placeholder_2.underlineText()
            updateViewStuff()
        }
    }

    override fun textColorChanged(color: Int) {
        (groups_list.adapter as GroupsAdapter).updateTextColor(color)
    }

    override fun primaryColorChanged(color: Int) {
        groups_fastscroller.apply {
            updatePrimaryColor()
            updateBubblePrimaryColor()
        }
    }

    override fun refreshContacts(contacts: ArrayList<Contact>) {
        if (activity == null) {
            return
        }

        lastContacts = contacts
        var storedGroups = ContactsHelper(activity!!).getStoredGroups()
        contacts.forEach {
            it.groups.forEach {
                val group = it
                val storedGroup = storedGroups.firstOrNull { it.id == group.id }
                storedGroup?.addContact()
            }
        }

        groups_placeholder_2.beVisibleIf(storedGroups.isEmpty())
        groups_placeholder.beVisibleIf(storedGroups.isEmpty())
        groups_list.beVisibleIf(storedGroups.isNotEmpty())

        storedGroups = storedGroups.sortedWith(compareBy { it.title }) as? ArrayList<Group> ?: ArrayList()
        val currAdapter = groups_list.adapter
        if (currAdapter == null) {
            GroupsAdapter(activity as SimpleActivity, storedGroups, groups_list, groups_fastscroller) {
                Intent(activity, GroupContactsActivity::class.java).apply {
                    putExtra(GROUP, it as Group)
                    activity!!.startActivity(this)
                }
            }.apply {
                setupDragListener(true)
                addVerticalDividers(true)
                groups_list.adapter = this
            }

            groups_fastscroller.setScrollTo(0)
            groups_fastscroller.setViews(groups_list) {
                val item = (groups_list.adapter as GroupsAdapter).groups.getOrNull(it)
                groups_fastscroller.updateBubbleText(item?.getBubbleText() ?: "")
            }
        } else {
            (currAdapter as GroupsAdapter).apply {
                showContactThumbnails = activity.config.showContactThumbnails
                updateItems(storedGroups)
            }
        }
    }

    override fun showContactThumbnailsChanged(showThumbnails: Boolean) {
        (groups_list.adapter as? GroupsAdapter)?.apply {
            showContactThumbnails = showThumbnails
            notifyDataSetChanged()
        }
    }

    override fun onActivityResume() {
        updateViewStuff()
    }

    override fun finishActMode() {
        (groups_list.adapter as? GroupsAdapter)?.finishActMode()
    }

    override fun onSearchQueryChanged(text: String) {
    }

    override fun onSearchOpened() {
    }

    override fun onSearchClosed() {
    }

    private fun updateViewStuff() {
        context.updateTextColors(groups_wrapper.parent as ViewGroup)
        groups_fastscroller.updateBubbleColors()
        groups_fastscroller.allowBubbleDisplay = context.config.showInfoBubble
        groups_placeholder_2.setTextColor(context.getAdjustedPrimaryColor())
    }
}
