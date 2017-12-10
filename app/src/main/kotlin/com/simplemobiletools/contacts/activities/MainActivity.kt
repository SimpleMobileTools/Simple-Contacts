package com.simplemobiletools.contacts.activities

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import com.simplemobiletools.commons.extensions.appLaunched
import com.simplemobiletools.commons.extensions.restartActivity
import com.simplemobiletools.commons.extensions.toast
import com.simplemobiletools.commons.extensions.updateTextColors
import com.simplemobiletools.commons.helpers.LICENSE_KOTLIN
import com.simplemobiletools.commons.helpers.LICENSE_MULTISELECT
import com.simplemobiletools.commons.helpers.PERMISSION_WRITE_CONTACTS
import com.simplemobiletools.commons.interfaces.RefreshRecyclerViewListener
import com.simplemobiletools.contacts.BuildConfig
import com.simplemobiletools.contacts.R
import com.simplemobiletools.contacts.adapters.ContactsAdapter
import com.simplemobiletools.contacts.dialogs.ChangeSortingDialog
import com.simplemobiletools.contacts.extensions.config
import com.simplemobiletools.contacts.helpers.ContactsHelper
import com.simplemobiletools.contacts.models.Contact
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : SimpleActivity(), RefreshRecyclerViewListener {
    private var storedUseEnglish = false
    private var storedTextColor = 0
    private var storedBackgroundColor = 0
    private var storedPrimaryColor = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        appLaunched()
        contacts_fab.setOnClickListener { addNewContact() }

        handlePermission(PERMISSION_WRITE_CONTACTS) {
            if (it) {
                initContacts()
            } else {
                toast(R.string.no_contacts_permission)
                finish()
            }
        }

        storeStateVariables()
    }

    override fun onResume() {
        super.onResume()
        if (storedUseEnglish != config.useEnglish) {
            restartActivity()
            return
        }

        if (storedTextColor != config.textColor) {
            (contacts_list.adapter as ContactsAdapter).apply {
                updateTextColor(config.textColor)
                initDrawables()
            }
        }

        if (storedPrimaryColor != config.primaryColor) {
            contacts_fastscroller.updatePrimaryColor()
        }

        contacts_fastscroller.updateBubbleColors()
        contacts_fastscroller.allowBubbleDisplay = config.showInfoBubble
        updateTextColors(contacts_holder)
    }

    override fun onPause() {
        super.onPause()
        storeStateVariables()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.sort -> showSortingDialog()
            R.id.settings -> startActivity(Intent(applicationContext, SettingsActivity::class.java))
            R.id.about -> launchAbout()
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    private fun showSortingDialog() {
        ChangeSortingDialog(this) {
            initContacts()
        }
    }

    private fun launchAbout() {
        startAboutActivity(R.string.app_name, LICENSE_KOTLIN or LICENSE_MULTISELECT, BuildConfig.VERSION_NAME)
    }

    private fun storeStateVariables() {
        config.apply {
            storedUseEnglish = useEnglish
            storedTextColor = textColor
            storedBackgroundColor = backgroundColor
            storedPrimaryColor = primaryColor
        }
    }

    private fun initContacts() {
        ContactsHelper(this).getContacts {
            Contact.sorting = config.sorting
            it.sort()
            runOnUiThread {
                setupContacts(it)
            }
        }
    }

    private fun setupContacts(contacts: ArrayList<Contact>) {
        val currAdapter = contacts_list.adapter
        if (currAdapter == null) {
            ContactsAdapter(this, contacts, this, contacts_list) {
                itemClicked(it as Contact)
            }.apply {
                setupDragListener(true)
                addVerticalDividers(true)
                contacts_list.adapter = this
            }

            contacts_fastscroller.setViews(contacts_list) {
                val item = contacts.getOrNull(it)
                contacts_fastscroller.updateBubbleText(item?.getBubbleText() ?: "")
            }
        } else {
            (currAdapter as ContactsAdapter).updateItems(contacts)
        }
    }

    private fun itemClicked(contact: Contact) {

    }

    private fun addNewContact() {
        Intent(applicationContext, ContactActivity::class.java).apply {
            startActivity(this)
        }
    }

    override fun refreshItems() {
        initContacts()
    }
}
