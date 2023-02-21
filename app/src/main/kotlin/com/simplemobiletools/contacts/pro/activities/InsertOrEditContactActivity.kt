package com.simplemobiletools.contacts.pro.activities

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.provider.ContactsContract.CommonDataKinds.Email
import android.provider.ContactsContract.CommonDataKinds.Phone
import android.widget.ImageView
import android.widget.TextView
import androidx.viewpager.widget.ViewPager
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.helpers.*
import com.simplemobiletools.contacts.pro.R
import com.simplemobiletools.contacts.pro.adapters.ViewPagerAdapter
import com.simplemobiletools.contacts.pro.dialogs.ChangeSortingDialog
import com.simplemobiletools.contacts.pro.dialogs.FilterContactSourcesDialog
import com.simplemobiletools.commons.extensions.baseConfig
import com.simplemobiletools.contacts.pro.fragments.MyViewPagerFragment
import com.simplemobiletools.contacts.pro.helpers.ADD_NEW_CONTACT_NUMBER
import com.simplemobiletools.contacts.pro.helpers.KEY_EMAIL
import com.simplemobiletools.contacts.pro.helpers.KEY_NAME
import com.simplemobiletools.contacts.pro.interfaces.RefreshContactsListener
import com.simplemobiletools.commons.models.contacts.*
import kotlinx.android.synthetic.main.activity_insert_edit_contact.*
import kotlinx.android.synthetic.main.fragment_contacts.*
import kotlinx.android.synthetic.main.fragment_favorites.*

class InsertOrEditContactActivity : SimpleActivity(), RefreshContactsListener {
    private val START_INSERT_ACTIVITY = 1
    private val START_EDIT_ACTIVITY = 2

    private var isSelectContactIntent = false
    private var specialMimeType: String? = null

    private val contactsFavoritesList = arrayListOf(
        TAB_CONTACTS,
        TAB_FAVORITES
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_insert_edit_contact)
        setupOptionsMenu()
        isSelectContactIntent = intent.action == Intent.ACTION_PICK
        updateMaterialActivityViews(insert_edit_coordinator, insert_edit_contact_holder, useTransparentNavigation = false, useTopSearchMenu = true)

        if (isSelectContactIntent) {
            specialMimeType = when (intent.data) {
                Email.CONTENT_URI -> Email.CONTENT_ITEM_TYPE
                Phone.CONTENT_URI -> Phone.CONTENT_ITEM_TYPE
                else -> null
            }
        }

        new_contact_holder.beGoneIf(isSelectContactIntent)
        select_contact_label.beGoneIf(isSelectContactIntent)

        if (checkAppSideloading()) {
            return
        }

        setupTabs()

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

    override fun onResume() {
        super.onResume()
        updateMenuColors()
        setupTabColors()
    }

    private fun setupOptionsMenu() {
        insert_edit_menu.getToolbar().inflateMenu(R.menu.menu_insert_or_edit)
        insert_edit_menu.toggleHideOnScroll(false)
        insert_edit_menu.setupMenu()

        insert_edit_menu.onSearchClosedListener = {
            getAllFragments().forEach {
                it?.onSearchClosed()
            }
        }

        insert_edit_menu.onSearchTextChangedListener = { text ->
            getCurrentFragment()?.onSearchQueryChanged(text)
        }

        insert_edit_menu.getToolbar().setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.sort -> showSortingDialog()
                R.id.filter -> showFilterDialog()
                else -> return@setOnMenuItemClickListener false
            }
            return@setOnMenuItemClickListener true
        }
    }

    private fun updateMenuColors() {
        updateStatusbarColor(getProperBackgroundColor())
        insert_edit_menu.updateColors()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        super.onActivityResult(requestCode, resultCode, resultData)
        if (resultCode == Activity.RESULT_OK) {
            hideKeyboard()
            finish()
        }
    }

    override fun onBackPressed() {
        if (insert_edit_menu.isSearchOpen) {
            insert_edit_menu.closeSearch()
        } else {
            super.onBackPressed()
        }
    }

    private fun initFragments() {
        view_pager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrollStateChanged(state: Int) {}

            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {}

            override fun onPageSelected(position: Int) {
                insert_edit_tabs_holder.getTabAt(position)?.select()
                getAllFragments().forEach {
                    it?.finishActMode()
                }
            }
        })

        view_pager.onGlobalLayout {
            refreshContacts(getTabsMask())
        }

        select_contact_label?.setTextColor(getProperPrimaryColor())
        new_contact_tmb?.setImageDrawable(resources.getColoredDrawableWithColor(R.drawable.ic_add_person_vector, getProperTextColor()))
        new_contact_name.setTextColor(getProperTextColor())
        new_contact_holder?.setOnClickListener {
            createNewContact()
        }
    }

    private fun setupTabs() {
        insert_edit_tabs_holder.removeAllTabs()
        contactsFavoritesList.forEachIndexed { index, value ->
            if (this.baseConfig.showTabs and value != 0) {
                insert_edit_tabs_holder.newTab().setCustomView(R.layout.bottom_tablayout_item).apply {
                    customView?.findViewById<ImageView>(R.id.tab_item_icon)?.setImageDrawable(getTabIcon(index))
                    customView?.findViewById<TextView>(R.id.tab_item_label)?.text = getTabLabel(index)
                    insert_edit_tabs_holder.addTab(this)
                }
            }
        }

        insert_edit_tabs_holder.onTabSelectionChanged(
            tabUnselectedAction = {
                updateBottomTabItemColors(it.customView, false, getDeselectedTabDrawableIds()[it.position])
            },
            tabSelectedAction = {
                insert_edit_menu.closeSearch()
                view_pager.currentItem = it.position
                updateBottomTabItemColors(it.customView, true, getSelectedTabDrawableIds()[it.position])
            }
        )

        insert_edit_tabs_holder.beGoneIf(insert_edit_tabs_holder.tabCount == 1)
    }

    private fun getCurrentFragment(): MyViewPagerFragment? {
        return if (view_pager.currentItem == 0) {
            contacts_fragment
        } else {
            favorites_fragment
        }
    }

    private fun getAllFragments() = arrayListOf(contacts_fragment, favorites_fragment)

    private fun setupTabColors() {
        val activeView = insert_edit_tabs_holder.getTabAt(view_pager.currentItem)?.customView
        updateBottomTabItemColors(activeView, true, getSelectedTabDrawableIds()[view_pager.currentItem])

        getInactiveTabIndexes(view_pager.currentItem).forEach { index ->
            val inactiveView = insert_edit_tabs_holder.getTabAt(index)?.customView
            updateBottomTabItemColors(inactiveView, false, getDeselectedTabDrawableIds()[index])
        }

        val bottomBarColor = getBottomNavigationBackgroundColor()
        insert_edit_tabs_holder.setBackgroundColor(bottomBarColor)
        updateNavigationBarColor(bottomBarColor)
    }

    private fun getInactiveTabIndexes(activeIndex: Int) = (0 until insert_edit_tabs_holder.tabCount).filter { it != activeIndex }

    private fun getSelectedTabDrawableIds() = arrayOf(
        R.drawable.ic_person_vector,
        R.drawable.ic_star_vector
    )

    private fun getDeselectedTabDrawableIds() = arrayOf(
        R.drawable.ic_person_outline_vector,
        R.drawable.ic_star_outline_vector
    )

    override fun refreshContacts(refreshTabsMask: Int) {
        if (isDestroyed || isFinishing) {
            return
        }

        if (view_pager.adapter == null) {
            view_pager.adapter = ViewPagerAdapter(this, contactsFavoritesList, getTabsMask())
        }

        ContactsHelper(this).getContacts {
            if (isDestroyed || isFinishing) {
                return@getContacts
            }

            val contacts = it.filter {
                if (specialMimeType != null) {
                    val hasRequiredValues = when (specialMimeType) {
                        Email.CONTENT_ITEM_TYPE -> it.emails.isNotEmpty()
                        Phone.CONTENT_ITEM_TYPE -> it.phoneNumbers.isNotEmpty()
                        else -> true
                    }
                    !it.isPrivate() && hasRequiredValues
                } else {
                    true
                }
            } as ArrayList<Contact>

            val placeholderText = when (specialMimeType) {
                Email.CONTENT_ITEM_TYPE -> getString(R.string.no_contacts_with_emails)
                Phone.CONTENT_ITEM_TYPE -> getString(R.string.no_contacts_with_phone_numbers)
                else -> null
            }

            if (refreshTabsMask and TAB_CONTACTS != 0) {
                contacts_fragment?.skipHashComparing = true
                contacts_fragment?.refreshContacts(contacts, placeholderText)
            }

            if (refreshTabsMask and TAB_FAVORITES != 0) {
                favorites_fragment?.skipHashComparing = true
                favorites_fragment?.refreshContacts(contacts, placeholderText)
            }
        }
    }

    override fun contactClicked(contact: Contact) {
        hideKeyboard()
        if (isSelectContactIntent) {
            Intent().apply {
                data = getResultUri(contact)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                setResult(RESULT_OK, this)
            }
            finish()
        } else {
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
            }
        }
    }

    private fun getResultUri(contact: Contact): Uri {
        return when {
            specialMimeType != null -> {
                val contactId = ContactsHelper(this).getContactMimeTypeId(contact.id.toString(), specialMimeType!!)
                Uri.withAppendedPath(ContactsContract.Data.CONTENT_URI, contactId)
            }
            else -> getContactPublicUri(contact)
        }
    }

    private fun createNewContact() {
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

            try {
                startActivityForResult(this, START_INSERT_ACTIVITY)
            } catch (e: ActivityNotFoundException) {
                toast(R.string.no_app_found)
            } catch (e: Exception) {
                showErrorToast(e)
            }
        }
    }

    private fun showSortingDialog() {
        ChangeSortingDialog(this) {
            refreshContacts(getTabsMask())
        }
    }

    fun showFilterDialog() {
        FilterContactSourcesDialog(this) {
            contacts_fragment?.forceListRedraw = true
            refreshContacts(getTabsMask())
        }
    }

    private fun getTabsMask(): Int {
        var mask = TAB_CONTACTS
        if (this.baseConfig.showTabs and TAB_FAVORITES != 0) {
            mask += TAB_FAVORITES
        }
        return mask
    }
}
