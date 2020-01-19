package com.simplemobiletools.contacts.pro.activities

import android.os.Bundle
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.simplemobiletools.commons.views.MyTextView
import com.simplemobiletools.contacts.pro.R
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

        val views = HashMap<Int, MyTextView>().apply {
            put(1, speed_dial_1)
            put(2, speed_dial_2)
            put(3, speed_dial_3)
            put(4, speed_dial_4)
            put(5, speed_dial_5)
            put(6, speed_dial_6)
            put(7, speed_dial_7)
            put(8, speed_dial_8)
            put(9, speed_dial_9)
        }

        val speedDialType = object : TypeToken<List<SpeedDial>>() {}.type
        speedDialValues = Gson().fromJson<ArrayList<SpeedDial>>(config.speedDial, speedDialType) ?: ArrayList(1)

        speedDialValues.forEach {
            val view = views.get(it.id)
            view!!.text = "${it.id}. ${it.displayName}"
        }

        ContactsHelper(this).getContacts { contacts ->
            allContacts = contacts

            for ((id, textView) in views) {
                setupView(id, textView)
            }
        }
    }

    override fun onStop() {
        super.onStop()
        config.speedDial = Gson().toJson(speedDialValues)
    }

    private fun setupView(id: Int, textView: MyTextView) {
        textView.setOnClickListener {
            SelectContactsDialog(this, allContacts, false, true) { addedContacts, removedContacts ->
                val selectedContact = addedContacts.first()
                val speedDial = SpeedDial(id, selectedContact.phoneNumbers.first().toString(), selectedContact.getNameToDisplay())
                textView.text = "$id. ${speedDial.displayName}"
                speedDialValues = speedDialValues.filter { it.id != id }.toMutableList() as ArrayList<SpeedDial>
                speedDialValues.add(speedDial)
            }
        }
    }
}
