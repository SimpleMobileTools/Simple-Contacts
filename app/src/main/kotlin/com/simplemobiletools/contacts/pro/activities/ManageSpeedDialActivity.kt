package com.simplemobiletools.contacts.pro.activities

import android.os.Bundle
import com.google.gson.Gson
import com.simplemobiletools.commons.extensions.updateTextColors
import com.simplemobiletools.contacts.pro.R
import com.simplemobiletools.contacts.pro.adapters.SpeedDialAdapter
import com.simplemobiletools.contacts.pro.dialogs.SelectContactsDialog
import com.simplemobiletools.contacts.pro.extensions.config
import com.simplemobiletools.contacts.pro.helpers.ContactsHelper
import com.simplemobiletools.contacts.pro.interfaces.RemoveSpeedDialListener
import com.simplemobiletools.contacts.pro.models.Contact
import com.simplemobiletools.contacts.pro.models.SpeedDial
import kotlinx.android.synthetic.main.activity_manage_speed_dial.*

class ManageSpeedDialActivity : SimpleActivity(), RemoveSpeedDialListener {
    private var allContacts = ArrayList<Contact>()
    private var speedDialValues = ArrayList<SpeedDial>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manage_speed_dial)

        speedDialValues = config.getSpeedDialValues()
        updateAdapter()
        ContactsHelper(this).getContacts { contacts ->
            allContacts = contacts
        }

        updateTextColors(manage_speed_dial_scrollview)
    }

    override fun onStop() {
        super.onStop()
        config.speedDial = Gson().toJson(speedDialValues)
    }

    private fun updateAdapter() {
        SpeedDialAdapter(this, speedDialValues, this, speed_dial_list) {
            val clickedContact = it as SpeedDial
            if (allContacts.isEmpty()) {
                return@SpeedDialAdapter
            }

            SelectContactsDialog(this, allContacts, false, true) { addedContacts, removedContacts ->
                val selectedContact = addedContacts.first()
                speedDialValues.first { it.id == clickedContact.id }.apply {
                    displayName = selectedContact.getNameToDisplay()
                    number = selectedContact.phoneNumbers.first().value
                }
                updateAdapter()
            }
        }.apply {
            speed_dial_list.adapter = this
        }
    }

    override fun removeSpeedDial(ids: ArrayList<Int>) {
        ids.forEach {
            val dialId = it
            speedDialValues.first { it.id == dialId }.apply {
                displayName = ""
                number = ""
            }
        }
        updateAdapter()
    }
}
