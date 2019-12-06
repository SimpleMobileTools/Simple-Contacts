package com.simplemobiletools.contacts.pro.activities

import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.provider.ContactsContract
import android.view.Menu
import androidx.viewpager.widget.ViewPager
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.helpers.PERMISSION_GET_ACCOUNTS
import com.simplemobiletools.commons.helpers.PERMISSION_READ_CONTACTS
import com.simplemobiletools.commons.helpers.PERMISSION_WRITE_CONTACTS
import com.simplemobiletools.contacts.pro.R
import com.simplemobiletools.contacts.pro.adapters.ViewPagerAdapter
import com.simplemobiletools.contacts.pro.extensions.config
import com.simplemobiletools.contacts.pro.extensions.getContactPublicUri
import com.simplemobiletools.contacts.pro.helpers.*
import com.simplemobiletools.contacts.pro.interfaces.RefreshContactsListener
import com.simplemobiletools.contacts.pro.models.Contact
import kotlinx.android.synthetic.main.activity_insert_edit_contact.*
import kotlinx.android.synthetic.main.fragment_favorites.*
import kotlinx.android.synthetic.main.fragment_insert_or_edit_contacts.*

class InsertOrEditContactActivity : SimpleActivity(), RefreshContactsListener {
    private val START_INSERT_ACTIVITY = 1
    private val START_EDIT_ACTIVITY = 2

    private val contactsFavoritesList = arrayListOf(CONTACTS_TAB_MASK,
            FAVORITES_TAB_MASK
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_insert_edit_contact)

        if (checkAppSideloading()) {
            return
        }

        setupTabColors()

        // we do not really care about the permission request result. Even if it was denied, load private contacts
        handlePermission(PERMISSION_READ_CONTACTS) {
            if (it) {
                handlePermission(PERMISSION_WRITE_CONTACTS) {
                    handlePermission(PERMISSION_GET_ACCOUNTS) {
                        initFragments()
                    }
                }
            } else {
                initFragments()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        updateMenuItemColors(menu)
        return super.onCreateOptionsMenu(menu)
    }

    private fun initFragments() {
        viewpager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrollStateChanged(state: Int) {}

            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {}

            override fun onPageSelected(position: Int) {
                insert_or_edit_tabs_holder.getTabAt(position)?.select()
                invalidateOptionsMenu()
            }
        })

        viewpager.onGlobalLayout {
            refreshContacts(CONTACTS_TAB_MASK or FAVORITES_TAB_MASK)
        }

        insert_or_edit_tabs_holder.onTabSelectionChanged(
                tabUnselectedAction = {
                    it.icon?.applyColorFilter(config.textColor)
                },
                tabSelectedAction = {
                    viewpager.currentItem = it.position
                    it.icon?.applyColorFilter(getAdjustedPrimaryColor())
                }
        )

        insert_or_edit_tabs_holder.removeAllTabs()
        var skippedTabs = 0
        contactsFavoritesList.forEachIndexed { index, value ->
            if (config.showTabs and value == 0) {
                skippedTabs++
            } else {
                val tab = insert_or_edit_tabs_holder.newTab().setIcon(getTabIcon(index))
                insert_or_edit_tabs_holder.addTab(tab, index - skippedTabs, index == 0)
            }
        }

        insert_or_edit_tabs_holder.beVisibleIf(skippedTabs == 0)
    }

    private fun setupTabColors() {
        insert_or_edit_tabs_holder.apply {
            background = ColorDrawable(config.backgroundColor)
            setSelectedTabIndicatorColor(getAdjustedPrimaryColor())
        }
    }

    override fun refreshContacts(refreshTabsMask: Int) {
        if (isDestroyed || isFinishing) {
            return
        }

        if (viewpager.adapter == null) {
            val fragments = arrayListOf(R.layout.fragment_insert_or_edit_contacts, R.layout.fragment_favorites)
            viewpager.adapter = ViewPagerAdapter(this, contactsFavoritesList, CONTACTS_TAB_MASK or FAVORITES_TAB_MASK, fragments)
        }

        ContactsHelper(this).getContacts { contacts ->
            if (isDestroyed || isFinishing) {
                return@getContacts
            }

            if (refreshTabsMask and CONTACTS_TAB_MASK != 0) {
                insert_or_edit_contacts_fragment?.refreshContacts(contacts)
            }

            if (refreshTabsMask and FAVORITES_TAB_MASK != 0) {
                favorites_fragment?.refreshContacts(contacts)
            }
        }
    }

    override fun contactClicked(contact: Contact?, isCreateNewContact: Boolean) {
        if (contact != null) {
            val phoneNumber = getPhoneNumberFromIntent(intent) ?: ""
            val email = getEmailFromIntent(intent) ?: ""

            Intent(applicationContext, EditContactActivity::class.java).apply {
                data = getContactPublicUri(contact)
                action = ADD_NEW_CONTACT_NUMBER

                if (phoneNumber.isNotEmpty()) {
                    putExtra(KEY_PHONE, phoneNumber)
                }

                if (email.isNotEmpty()) {
                    putExtra(KEY_EMAIL, email)
                }

                putExtra(IS_PRIVATE, contact.isPrivate())
                startActivityForResult(this, START_EDIT_ACTIVITY)
                finish()
            }
        } else if (isCreateNewContact) {
            val name = intent.getStringExtra(KEY_NAME) ?: ""
            val phoneNumber = getPhoneNumberFromIntent(intent) ?: ""
            val email = getEmailFromIntent(intent) ?: ""

            Intent().apply {
                action = Intent.ACTION_INSERT
                data = ContactsContract.Contacts.CONTENT_URI

                if (phoneNumber.isNotEmpty()) {
                    putExtra(KEY_PHONE, phoneNumber)
                }

                if (name.isNotEmpty()) {
                    putExtra(KEY_NAME, name)
                }

                if (email.isNotEmpty()) {
                    putExtra(KEY_EMAIL, email)
                }

                if (resolveActivity(packageManager) != null) {
                    startActivityForResult(this, START_INSERT_ACTIVITY)
                    finish()
                } else {
                    toast(R.string.no_app_found)
                }
            }
        }
    }
}
