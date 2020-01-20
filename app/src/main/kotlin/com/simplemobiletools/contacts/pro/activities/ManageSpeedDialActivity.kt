package com.simplemobiletools.contacts.pro.activities

import android.os.Bundle
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.simplemobiletools.contacts.pro.R
import com.simplemobiletools.contacts.pro.adapters.SpeedDialAdapter
import com.simplemobiletools.contacts.pro.dialogs.SelectContactsDialog
import com.simplemobiletools.contacts.pro.extensions.config
import com.simplemobiletools.contacts.pro.helpers.ContactsHelper
import com.simplemobiletools.contacts.pro.models.Contact
import com.simplemobiletools.contacts.pro.models.SpeedDial
import kotlinx.android.synthetic.main.activity_manage_speed_dial.*

class ManageSpeedDialActivity : SimpleActivity() {
    private var allContacts = ArrayList<Contact>()
    private var speedDialValues = ArrayList<SpeedDial>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manage_speed_dial)

        val speedDialType = object : TypeToken<List<SpeedDial>>() {}.type
        speedDialValues = Gson().fromJson<ArrayList<SpeedDial>>(config.speedDial, speedDialType) ?: ArrayList(1)

        for (i in 1..9) {
            val speedDial = SpeedDial(i, "", "")
            if (speedDialValues.firstOrNull { it.id == i } == null) {
                speedDialValues.add(speedDial)
            }
        }

        updateAdapter()
        ContactsHelper(this).getContacts { contacts ->
            allContacts = contacts
        }
    }

    override fun onStop() {
        super.onStop()
        config.speedDial = Gson().toJson(speedDialValues)
    }

    private fun updateAdapter() {
        SpeedDialAdapter(this, speedDialValues, speed_dial_list) {
            val clickedContact = it as SpeedDial
            if (allContacts.isEmpty()) {
                return@SpeedDialAdapter
            }

            SelectContactsDialog(this, allContacts, false, true) { addedContacts, removedContacts ->
                val selectedContact = addedContacts.first()
                speedDialValues.first { it.id == clickedContact.id }.apply {
                    displayName = selectedContact.getNameToDisplay()
                    number = selectedContact.phoneNumbers.first().toString()
                }
                updateAdapter()
            }
        }.apply {
            speed_dial_list.adapter = this
        }
    }
}
