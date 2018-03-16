package com.simplemobiletools.contacts.activities

import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.support.v4.view.MenuItemCompat
import android.support.v4.view.ViewPager
import android.support.v7.widget.SearchView
import android.view.Menu
import android.view.MenuItem
import com.simplemobiletools.commons.dialogs.FilePickerDialog
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.helpers.*
import com.simplemobiletools.commons.models.FAQItem
import com.simplemobiletools.commons.models.Release
import com.simplemobiletools.contacts.BuildConfig
import com.simplemobiletools.contacts.R
import com.simplemobiletools.contacts.adapters.ViewPagerAdapter
import com.simplemobiletools.contacts.dialogs.ChangeSortingDialog
import com.simplemobiletools.contacts.dialogs.ExportContactsDialog
import com.simplemobiletools.contacts.dialogs.FilterContactSourcesDialog
import com.simplemobiletools.contacts.dialogs.ImportContactsDialog
import com.simplemobiletools.contacts.extensions.config
import com.simplemobiletools.contacts.extensions.dbHelper
import com.simplemobiletools.contacts.extensions.getTempFile
import com.simplemobiletools.contacts.fragments.MyViewPagerFragment
import com.simplemobiletools.contacts.helpers.ContactsHelper
import com.simplemobiletools.contacts.helpers.VcfExporter
import com.simplemobiletools.contacts.interfaces.RefreshContactsListener
import com.simplemobiletools.contacts.models.Contact
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_contacts.*
import kotlinx.android.synthetic.main.fragment_favorites.*
import kotlinx.android.synthetic.main.fragment_groups.*
import java.io.FileOutputStream

class MainActivity : SimpleActivity(), RefreshContactsListener {
    private var isFirstResume = true
    private var isSearchOpen = false
    private var searchMenuItem: MenuItem? = null

    private var storedUseEnglish = false
    private var storedTextColor = 0
    private var storedBackgroundColor = 0
    private var storedPrimaryColor = 0
    private var storedShowContactThumbnails = false
    private var storedShowPhoneNumbers = false
    private var storedStartNameWithSurname = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        appLaunched()
        setupTabColors()

        // just get a reference to the database to make sure it is created properly
        dbHelper

        handlePermission(PERMISSION_READ_CONTACTS) {
            if (it) {
                handlePermission(PERMISSION_WRITE_CONTACTS) {
                    if (it) {
                        storeLocalAccountData()
                        initFragments()
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
        checkWhatsNewDialog()
    }

    override fun onResume() {
        super.onResume()
        if (storedUseEnglish != config.useEnglish) {
            restartActivity()
            return
        }

        if (storedShowPhoneNumbers != config.showPhoneNumbers) {
            restartActivity()
            return
        }

        val configShowContactThumbnails = config.showContactThumbnails
        if (storedShowContactThumbnails != configShowContactThumbnails) {
            contacts_fragment?.showContactThumbnailsChanged(configShowContactThumbnails)
            favorites_fragment?.showContactThumbnailsChanged(configShowContactThumbnails)
        }

        val configTextColor = config.textColor
        if (storedTextColor != configTextColor) {
            getInactiveTabIndexes(viewpager.currentItem).forEach {
                main_tabs_holder.getTabAt(it)?.icon?.applyColorFilter(configTextColor)
            }
            contacts_fragment?.textColorChanged(configTextColor)
            favorites_fragment?.textColorChanged(configTextColor)
        }

        val configBackgroundColor = config.backgroundColor
        if (storedBackgroundColor != configBackgroundColor) {
            main_tabs_holder.background = ColorDrawable(configBackgroundColor)
        }

        val configPrimaryColor = config.primaryColor
        if (storedPrimaryColor != configPrimaryColor) {
            main_tabs_holder.setSelectedTabIndicatorColor(getAdjustedPrimaryColor())
            main_tabs_holder.getTabAt(viewpager.currentItem)?.icon?.applyColorFilter(getAdjustedPrimaryColor())
            contacts_fragment?.primaryColorChanged()
            favorites_fragment?.primaryColorChanged()
        }

        val configStartNameWithSurname = config.startNameWithSurname
        if (storedStartNameWithSurname != configStartNameWithSurname) {
            contacts_fragment?.startNameWithSurnameChanged(configStartNameWithSurname)
            favorites_fragment?.startNameWithSurnameChanged(configStartNameWithSurname)
        }

        if (!isFirstResume) {
            if (viewpager.adapter == null) {
                initFragments()
            }

            contacts_fragment?.initContacts()
            contacts_fragment?.onActivityResume()
            favorites_fragment?.initContacts()
            favorites_fragment?.onActivityResume()
        }

        if (hasPermission(PERMISSION_WRITE_CONTACTS)) {
            isFirstResume = false
        }
    }

    override fun onPause() {
        super.onPause()
        storeStateVariables()
    }

    override fun onDestroy() {
        super.onDestroy()
        config.lastUsedViewPagerPage = viewpager.currentItem
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu, menu)
        setupSearch(menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.sort -> showSortingDialog()
            R.id.filter -> showFilterDialog()
            R.id.import_contacts -> tryImportContacts()
            R.id.export_contacts -> tryExportContacts()
            R.id.settings -> startActivity(Intent(applicationContext, SettingsActivity::class.java))
            R.id.about -> launchAbout()
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    private fun storeStateVariables() {
        config.apply {
            storedUseEnglish = useEnglish
            storedTextColor = textColor
            storedBackgroundColor = backgroundColor
            storedPrimaryColor = primaryColor
            storedShowContactThumbnails = showContactThumbnails
            storedShowPhoneNumbers = showPhoneNumbers
            storedStartNameWithSurname = startNameWithSurname
        }
    }

    private fun setupSearch(menu: Menu) {
        val searchManager = getSystemService(Context.SEARCH_SERVICE) as SearchManager
        searchMenuItem = menu.findItem(R.id.search)
        (searchMenuItem!!.actionView as SearchView).apply {
            setSearchableInfo(searchManager.getSearchableInfo(componentName))
            isSubmitButtonEnabled = false
            queryHint = getString(if (viewpager.currentItem == 0) R.string.search_contacts else R.string.search_favorites)
            setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String) = false

                override fun onQueryTextChange(newText: String): Boolean {
                    if (isSearchOpen) {
                        (getCurrentFragment() as? MyViewPagerFragment)?.onSearchQueryChanged(newText)
                    }
                    return true
                }
            })
        }

        MenuItemCompat.setOnActionExpandListener(searchMenuItem, object : MenuItemCompat.OnActionExpandListener {
            override fun onMenuItemActionExpand(item: MenuItem?): Boolean {
                (getCurrentFragment() as? MyViewPagerFragment)?.onSearchOpened()
                isSearchOpen = true
                return true
            }

            override fun onMenuItemActionCollapse(item: MenuItem?): Boolean {
                (getCurrentFragment() as? MyViewPagerFragment)?.onSearchClosed()
                isSearchOpen = false
                return true
            }
        })
    }

    private fun getCurrentFragment() = when (viewpager.currentItem) {
        0 -> contacts_fragment
        1 -> favorites_fragment
        else -> groups_fragment
    }

    private fun setupTabColors() {
        val lastUsedPage = config.lastUsedViewPagerPage
        main_tabs_holder.apply {
            background = ColorDrawable(config.backgroundColor)
            setSelectedTabIndicatorColor(getAdjustedPrimaryColor())
            getTabAt(lastUsedPage)?.select()
            getTabAt(lastUsedPage)?.icon?.applyColorFilter(getAdjustedPrimaryColor())

            getInactiveTabIndexes(lastUsedPage).forEach {
                getTabAt(it)?.icon?.applyColorFilter(config.textColor)
            }
        }
    }

    private fun storeLocalAccountData() {
        if (config.localAccountType == "-1") {
            // some manufacturer contact account types from https://stackoverflow.com/a/44802016/1967672
            val localAccountTypes = arrayListOf("vnd.sec.contact.phone",
                    "com.htc.android.pcsc",
                    "com.sonyericsson.localcontacts",
                    "com.lge.sync",
                    "com.lge.phone",
                    "vnd.tmobileus.contact.phone",
                    "com.android.huawei.phone",
                    "Local Phone Account")

            ContactsHelper(this).getContactSources {
                var localAccountType = ""
                var localAccountName = ""
                it.forEach {
                    if (localAccountTypes.contains(it.type)) {
                        localAccountType = it.type
                        localAccountName = it.name
                    }
                }

                config.localAccountType = localAccountType
                config.localAccountName = localAccountName
            }
        }
    }

    private fun getInactiveTabIndexes(activeIndex: Int) = arrayListOf(0, 1, 2).filter { it != activeIndex }

    private fun initFragments() {
        viewpager.adapter = ViewPagerAdapter(this)
        viewpager.offscreenPageLimit = 2
        viewpager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrollStateChanged(state: Int) {
                if (isSearchOpen) {
                    (getCurrentFragment() as? MyViewPagerFragment)?.onSearchQueryChanged("")
                    searchMenuItem?.collapseActionView()
                }
            }

            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
            }

            override fun onPageSelected(position: Int) {
                main_tabs_holder.getTabAt(position)?.select()
                contacts_fragment?.finishActMode()
                favorites_fragment?.finishActMode()
                invalidateOptionsMenu()
            }
        })
        viewpager.currentItem = config.lastUsedViewPagerPage

        main_tabs_holder.onTabSelectionChanged(
                tabUnselectedAction = {
                    it.icon?.applyColorFilter(config.textColor)
                },
                tabSelectedAction = {
                    viewpager.currentItem = it.position
                    it.icon?.applyColorFilter(getAdjustedPrimaryColor())
                }
        )

        if (intent?.action == Intent.ACTION_VIEW && intent.data != null) {
            tryImportContactsFromFile(intent.data)
        }
    }

    private fun showSortingDialog() {
        ChangeSortingDialog(this) {
            contacts_fragment?.initContacts()
            favorites_fragment?.initContacts()
        }
    }

    fun showFilterDialog() {
        FilterContactSourcesDialog(this) {
            contacts_fragment?.forceListRedraw = true
            contacts_fragment?.initContacts()
        }
    }

    private fun tryImportContacts() {
        handlePermission(PERMISSION_READ_STORAGE) {
            if (it) {
                importContacts()
            }
        }
    }

    private fun importContacts() {
        FilePickerDialog(this) {
            showImportContactsDialog(it)
        }
    }

    private fun showImportContactsDialog(path: String) {
        ImportContactsDialog(this, path) {
            if (it) {
                runOnUiThread {
                    refreshContacts()
                }
            }
        }
    }

    private fun tryImportContactsFromFile(uri: Uri) {
        when {
            uri.scheme == "file" -> showImportContactsDialog(uri.path)
            uri.scheme == "content" -> {
                val tempFile = getTempFile()
                if (tempFile == null) {
                    toast(R.string.unknown_error_occurred)
                    return
                }

                val inputStream = contentResolver.openInputStream(uri)
                val out = FileOutputStream(tempFile)
                inputStream.copyTo(out)
                showImportContactsDialog(tempFile.absolutePath)
            }
            else -> toast(R.string.invalid_file_format)
        }
    }

    private fun tryExportContacts() {
        handlePermission(PERMISSION_WRITE_STORAGE) {
            if (it) {
                exportContacts()
            }
        }
    }

    private fun exportContacts() {
        FilePickerDialog(this, pickFile = false, showFAB = true) {
            ExportContactsDialog(this, it) { file, contactSources ->
                Thread {
                    ContactsHelper(this).getContacts {
                        val contacts = it.filter { contactSources.contains(it.source) }
                        if (contacts.isEmpty()) {
                            toast(R.string.no_entries_for_exporting)
                        } else {
                            toast(R.string.exporting)
                            VcfExporter().exportContacts(this, file, contacts as ArrayList<Contact>) {
                                toast(when (it) {
                                    VcfExporter.ExportResult.EXPORT_OK -> R.string.exporting_successful
                                    VcfExporter.ExportResult.EXPORT_PARTIAL -> R.string.exporting_some_entries_failed
                                    else -> R.string.exporting_failed
                                })
                            }
                        }
                    }
                }.start()
            }
        }
    }

    private fun launchAbout() {
        val faqItems = arrayListOf(FAQItem(R.string.faq_2_title_commons, R.string.faq_2_text_commons))
        startAboutActivity(R.string.app_name, LICENSE_MULTISELECT or LICENSE_JODA or LICENSE_GLIDE or LICENSE_GSON or LICENSE_STETHO,
                BuildConfig.VERSION_NAME, faqItems)
    }

    override fun refreshContacts() {
        contacts_fragment.initContacts()
        favorites_fragment.initContacts()
    }

    override fun refreshFavorites() {
        favorites_fragment?.initContacts()
    }

    private fun checkWhatsNewDialog() {
        arrayListOf<Release>().apply {
            add(Release(10, R.string.release_10))
            add(Release(11, R.string.release_11))
            checkWhatsNew(this, BuildConfig.VERSION_CODE)
        }
    }
}
