package com.simplemobiletools.contacts.activities

import android.content.Intent
import android.graphics.Paint
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.helpers.*
import com.simplemobiletools.commons.interfaces.RefreshRecyclerViewListener
import com.simplemobiletools.contacts.BuildConfig
import com.simplemobiletools.contacts.R
import com.simplemobiletools.contacts.adapters.ContactsAdapter
import com.simplemobiletools.contacts.dialogs.ChangeSortingDialog
import com.simplemobiletools.contacts.dialogs.FilterContactSourcesDialog
import com.simplemobiletools.contacts.extensions.config
import com.simplemobiletools.contacts.extensions.openContact
import com.simplemobiletools.contacts.extensions.tryStartCall
import com.simplemobiletools.contacts.helpers.ContactsHelper
import com.simplemobiletools.contacts.models.Contact
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : SimpleActivity(), RefreshRecyclerViewListener {
    private var storedUseEnglish = false
    private var storedTextColor = 0
    private var storedBackgroundColor = 0
    private var storedPrimaryColor = 0
    private var storedStartNameWithSurname = false

    private var isFirstResume = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        appLaunched()
        contacts_fab.setOnClickListener { addNewContact() }

        handlePermission(PERMISSION_READ_CONTACTS) {
            if (it) {
                handlePermission(PERMISSION_WRITE_CONTACTS) {
                    if (it) {
                        initContacts()
                    } else {
                        toast(R.string.no_contacts_permission)
                        finish()
                    }
                }
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

        if (storedStartNameWithSurname != config.startNameWithSurname) {
            (contacts_list.adapter as ContactsAdapter).apply {
                startNameWithSurname = config.startNameWithSurname
                config.sorting = if (config.startNameWithSurname) SORT_BY_SURNAME else SORT_BY_FIRST_NAME
                initContacts()
            }
        }

        contacts_fastscroller.updateBubbleColors()
        contacts_fastscroller.allowBubbleDisplay = config.showInfoBubble
        updateTextColors(contacts_holder)

        contacts_placeholder_2.paintFlags = contacts_placeholder_2.paintFlags or Paint.UNDERLINE_TEXT_FLAG
        contacts_placeholder_2.setTextColor(config.primaryColor)
        contacts_placeholder_2.setOnClickListener {
            showFilterDialog()
        }

        if (!isFirstResume) {
            initContacts()
        }
        isFirstResume = false
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
            R.id.filter -> showFilterDialog()
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

    private fun showFilterDialog() {
        FilterContactSourcesDialog(this) {
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
            storedStartNameWithSurname = startNameWithSurname
        }
    }

    private fun initContacts() {
        ContactsHelper(this).getContacts {
            if (config.lastUsedContactSource.isEmpty()) {
                val grouped = it.groupBy { it.source }.maxWith(compareBy { it.value.size })
                config.lastUsedContactSource = grouped?.key ?: ""
            }

            Contact.sorting = config.sorting
            it.sort()

            if (it.hashCode() != (contacts_list.adapter as? ContactsAdapter)?.contactItems?.hashCode()) {
                runOnUiThread {
                    setupContacts(it)
                }
            }
        }
    }

    private fun setupContacts(contacts: ArrayList<Contact>) {
        contacts_placeholder_2.beVisibleIf(contacts.isEmpty())
        contacts_placeholder.beVisibleIf(contacts.isEmpty())

        val currAdapter = contacts_list.adapter
        if (currAdapter == null) {
            ContactsAdapter(this, contacts, this, contacts_list) {
                if (config.callContact) {
                    val contact = it as Contact
                    if (contact.phoneNumbers.isNotEmpty()) {
                        tryStartCall(it)
                    } else {
                        toast(R.string.no_phone_number_found)
                    }
                } else {
                    openContact(it as Contact)
                }
            }.apply {
                setupDragListener(true)
                addVerticalDividers(true)
                contacts_list.adapter = this
            }

            contacts_fastscroller.setViews(contacts_list) {
                val item = (contacts_list.adapter as ContactsAdapter).contactItems.getOrNull(it)
                contacts_fastscroller.updateBubbleText(item?.getBubbleText() ?: "")
            }
        } else {
            (currAdapter as ContactsAdapter).updateItems(contacts)
        }
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
