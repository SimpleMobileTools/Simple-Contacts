package com.simplemobiletools.contacts.pro.dialogs

import androidx.appcompat.app.AlertDialog
import com.reddit.indicatorfastscroll.FastScrollItemIndicator
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.helpers.ensureBackgroundThread
import com.simplemobiletools.contacts.pro.R
import com.simplemobiletools.contacts.pro.activities.SimpleActivity
import com.simplemobiletools.contacts.pro.adapters.SelectContactsAdapter
import com.simplemobiletools.contacts.pro.extensions.getVisibleContactSources
import com.simplemobiletools.contacts.pro.models.Contact
import kotlinx.android.synthetic.main.layout_select_contact.view.*
import java.util.*

class SelectContactsDialog(
    val activity: SimpleActivity, initialContacts: ArrayList<Contact>, val allowSelectMultiple: Boolean, val showOnlyContactsWithNumber: Boolean,
    selectContacts: ArrayList<Contact>? = null, val callback: (addedContacts: ArrayList<Contact>, removedContacts: ArrayList<Contact>) -> Unit
) {
    private var dialog: AlertDialog? = null
    private var view = activity.layoutInflater.inflate(R.layout.layout_select_contact, null)
    private var initiallySelectedContacts = ArrayList<Contact>()

    init {
        var allContacts = initialContacts
        if (selectContacts == null) {
            val contactSources = activity.getVisibleContactSources()
            allContacts = allContacts.filter { contactSources.contains(it.source) } as ArrayList<Contact>

            if (showOnlyContactsWithNumber) {
                allContacts = allContacts.filter { it.phoneNumbers.isNotEmpty() }.toMutableList() as ArrayList<Contact>
            }

            initiallySelectedContacts = allContacts.filter { it.starred == 1 } as ArrayList<Contact>
        } else {
            initiallySelectedContacts = selectContacts
        }

        // if selecting multiple contacts is disabled, react on first contact click and dismiss the dialog
        val contactClickCallback: ((Contact) -> Unit)? = if (allowSelectMultiple) null else { contact ->
            callback(arrayListOf(contact), arrayListOf())
            dialog!!.dismiss()
        }

        view.apply {
            select_contact_list.adapter = SelectContactsAdapter(
                activity, allContacts, initiallySelectedContacts, allowSelectMultiple,
                select_contact_list, contactClickCallback
            )

            if (context.areSystemAnimationsEnabled) {
                select_contact_list.scheduleLayoutAnimation()
            }
        }

        setupFastscroller(allContacts)

        val builder = AlertDialog.Builder(activity)
        if (allowSelectMultiple) {
            builder.setPositiveButton(R.string.ok) { dialog, which -> dialogConfirmed() }
        }
        builder.setNegativeButton(R.string.cancel, null)

        dialog = builder.create().apply {
            activity.setupDialogStuff(view, this)
        }
    }

    private fun dialogConfirmed() {
        ensureBackgroundThread {
            val adapter = view?.select_contact_list?.adapter as? SelectContactsAdapter
            val selectedContacts = adapter?.getSelectedItemsSet()?.toList() ?: ArrayList()

            val newlySelectedContacts = selectedContacts.filter { !initiallySelectedContacts.contains(it) } as ArrayList
            val unselectedContacts = initiallySelectedContacts.filter { !selectedContacts.contains(it) } as ArrayList
            callback(newlySelectedContacts, unselectedContacts)
        }
    }

    private fun setupFastscroller(allContacts: ArrayList<Contact>) {
        val adjustedPrimaryColor = activity.getProperPrimaryColor()
        view.apply {
            letter_fastscroller?.textColor = context.getProperTextColor().getColorStateList()
            letter_fastscroller?.pressedTextColor = adjustedPrimaryColor
            letter_fastscroller_thumb?.fontSize = context.getTextSize()
            letter_fastscroller_thumb?.textColor = adjustedPrimaryColor.getContrastColor()
            letter_fastscroller_thumb?.thumbColor = adjustedPrimaryColor.getColorStateList()
            letter_fastscroller_thumb.setupWithFastScroller(letter_fastscroller)
        }

        view.letter_fastscroller.setupWithRecyclerView(view.select_contact_list, { position ->
            try {
                val name = allContacts[position].getNameToDisplay()
                val character = if (name.isNotEmpty()) name.substring(0, 1) else ""
                FastScrollItemIndicator.Text(character.normalizeString().toUpperCase(Locale.getDefault()))
            } catch (e: Exception) {
                FastScrollItemIndicator.Text("")
            }
        })
    }
}
