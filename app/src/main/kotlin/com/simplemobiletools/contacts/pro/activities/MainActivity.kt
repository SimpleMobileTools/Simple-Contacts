package com.simplemobiletools.contacts.pro.activities

import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.widget.SearchView
import androidx.core.view.MenuItemCompat
import androidx.viewpager.widget.ViewPager
import com.simplemobiletools.commons.dialogs.FilePickerDialog
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.helpers.*
import com.simplemobiletools.commons.models.FAQItem
import com.simplemobiletools.commons.models.Release
import com.simplemobiletools.contacts.pro.BuildConfig
import com.simplemobiletools.contacts.pro.R
import com.simplemobiletools.contacts.pro.adapters.ViewPagerAdapter
import com.simplemobiletools.contacts.pro.databases.ContactsDatabase
import com.simplemobiletools.contacts.pro.dialogs.ChangeSortingDialog
import com.simplemobiletools.contacts.pro.dialogs.ExportContactsDialog
import com.simplemobiletools.contacts.pro.dialogs.FilterContactSourcesDialog
import com.simplemobiletools.contacts.pro.dialogs.ImportContactsDialog
import com.simplemobiletools.contacts.pro.extensions.config
import com.simplemobiletools.contacts.pro.extensions.getTempFile
import com.simplemobiletools.contacts.pro.fragments.MyViewPagerFragment
import com.simplemobiletools.contacts.pro.helpers.*
import com.simplemobiletools.contacts.pro.interfaces.RefreshContactsListener
import com.simplemobiletools.contacts.pro.models.Contact
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_contacts.*
import kotlinx.android.synthetic.main.fragment_favorites.*
import kotlinx.android.synthetic.main.fragment_groups.*
import kotlinx.android.synthetic.main.fragment_recents.*
import java.io.FileOutputStream


class MainActivity : SimpleActivity(), RefreshContactsListener {
    private var isSearchOpen = false
    private var searchMenuItem: MenuItem? = null
    private var werePermissionsHandled = false
    private var isFirstResume = true
    private var isGettingContacts = false
    private var handledShowTabs = 0

    private var storedTextColor = 0
    private var storedBackgroundColor = 0
    private var storedPrimaryColor = 0
    private var storedShowContactThumbnails = false
    private var storedShowPhoneNumbers = false
    private var storedStartNameWithSurname = false
    private var storedFilterDuplicates = true
    private var storedShowTabs = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        appLaunched(BuildConfig.APPLICATION_ID)
        setupTabColors()

        handlePermission(PERMISSION_READ_CALL_LOG) {
            if (it) {
                handlePermission(PERMISSION_WRITE_CALL_LOG) {
                    checkContactPermissions()
                }
            } else {
                checkContactPermissions()
            }
        }

        storeStateVariables()
        checkWhatsNewDialog()
    }

    private fun checkContactPermissions() {
        handlePermission(PERMISSION_READ_CONTACTS) {
            werePermissionsHandled = true
            if (it) {
                handlePermission(PERMISSION_WRITE_CONTACTS) {
                    handlePermission(PERMISSION_GET_ACCOUNTS) {
                        storeLocalAccountData()
                        initFragments()
                    }
                }
            } else {
                storeLocalAccountData()
                initFragments()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (storedShowPhoneNumbers != config.showPhoneNumbers) {
            System.exit(0)
            return
        }

        if (storedShowTabs != config.showTabs) {
            config.lastUsedViewPagerPage = 0
            System.exit(0)
            return
        }

        val configShowContactThumbnails = config.showContactThumbnails
        if (storedShowContactThumbnails != configShowContactThumbnails) {
            getAllFragments().forEach {
                it?.showContactThumbnailsChanged(configShowContactThumbnails)
            }
        }

        val configTextColor = config.textColor
        if (storedTextColor != configTextColor) {
            getInactiveTabIndexes(viewpager.currentItem).forEach {
                main_tabs_holder.getTabAt(it)?.icon?.applyColorFilter(configTextColor)
            }
            getAllFragments().forEach {
                it?.textColorChanged(configTextColor)
            }
        }

        val configBackgroundColor = config.backgroundColor
        if (storedBackgroundColor != configBackgroundColor) {
            main_tabs_holder.background = ColorDrawable(configBackgroundColor)
        }

        val configPrimaryColor = config.primaryColor
        if (storedPrimaryColor != configPrimaryColor) {
            main_tabs_holder.setSelectedTabIndicatorColor(getAdjustedPrimaryColor())
            main_tabs_holder.getTabAt(viewpager.currentItem)?.icon?.applyColorFilter(getAdjustedPrimaryColor())
            getAllFragments().forEach {
                it?.primaryColorChanged()
            }
        }

        val configStartNameWithSurname = config.startNameWithSurname
        if (storedStartNameWithSurname != configStartNameWithSurname) {
            contacts_fragment?.startNameWithSurnameChanged(configStartNameWithSurname)
            favorites_fragment?.startNameWithSurnameChanged(configStartNameWithSurname)
        }

        if (storedFilterDuplicates != config.filterDuplicates) {
            refreshContacts(ALL_TABS_MASK)
        }

        if (werePermissionsHandled && !isFirstResume) {
            if (viewpager.adapter == null) {
                initFragments()
            } else {
                refreshContacts(ALL_TABS_MASK)

                getAllFragments().forEach {
                    it?.onActivityResume()
                }
            }
        }

        val dialpadIcon = resources.getColoredDrawableWithColor(R.drawable.ic_dialpad, if (isBlackAndWhiteTheme()) Color.BLACK else Color.WHITE)
        main_dialpad_button.apply {
            setImageDrawable(dialpadIcon)
            background.applyColorFilter(getAdjustedPrimaryColor())
            beVisibleIf(config.showDialpadButton)
        }

        isFirstResume = false
    }

    override fun onPause() {
        super.onPause()
        storeStateVariables()
    }

    override fun onDestroy() {
        super.onDestroy()
        config.lastUsedViewPagerPage = viewpager.currentItem
        if (!isChangingConfigurations) {
            ContactsDatabase.destroyInstance()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu, menu)
        val currentFragment = getCurrentFragment()

        menu.apply {
            findItem(R.id.search).isVisible = currentFragment != groups_fragment && currentFragment != recents_fragment
            findItem(R.id.sort).isVisible = currentFragment != groups_fragment && currentFragment != recents_fragment
            findItem(R.id.filter).isVisible = currentFragment != groups_fragment && currentFragment != recents_fragment
        }
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
            storedTextColor = textColor
            storedBackgroundColor = backgroundColor
            storedPrimaryColor = primaryColor
            storedShowContactThumbnails = showContactThumbnails
            storedShowPhoneNumbers = showPhoneNumbers
            storedStartNameWithSurname = startNameWithSurname
            storedFilterDuplicates = filterDuplicates
            storedShowTabs = showTabs
        }
    }

    private fun setupSearch(menu: Menu) {
        val searchManager = getSystemService(Context.SEARCH_SERVICE) as SearchManager
        searchMenuItem = menu.findItem(R.id.search)
        (searchMenuItem!!.actionView as SearchView).apply {
            setSearchableInfo(searchManager.getSearchableInfo(componentName))
            isSubmitButtonEnabled = false
            queryHint = getString(if (getCurrentFragment() == contacts_fragment) R.string.search_contacts else R.string.search_favorites)
            setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String) = false

                override fun onQueryTextChange(newText: String): Boolean {
                    if (isSearchOpen) {
                        getCurrentFragment()?.onSearchQueryChanged(newText)
                    }
                    return true
                }
            })
        }

        MenuItemCompat.setOnActionExpandListener(searchMenuItem, object : MenuItemCompat.OnActionExpandListener {
            override fun onMenuItemActionExpand(item: MenuItem?): Boolean {
                getCurrentFragment()?.onSearchOpened()
                isSearchOpen = true
                return true
            }

            override fun onMenuItemActionCollapse(item: MenuItem?): Boolean {
                getCurrentFragment()?.onSearchClosed()
                isSearchOpen = false
                return true
            }
        })
    }

    private fun getCurrentFragment(): MyViewPagerFragment? {
        val showTabs = config.showTabs
        val fragments = arrayListOf<MyViewPagerFragment>()
        if (showTabs and CONTACTS_TAB_MASK != 0) {
            fragments.add(contacts_fragment)
        }

        if (showTabs and FAVORITES_TAB_MASK != 0) {
            fragments.add(favorites_fragment)
        }

        if (showTabs and RECENTS_TAB_MASK != 0) {
            fragments.add(recents_fragment)
        }

        if (showTabs and GROUPS_TAB_MASK != 0) {
            fragments.add(groups_fragment)
        }

        return fragments[viewpager.currentItem]
    }

    private fun setupTabColors() {
        handledShowTabs = config.showTabs
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
            ContactsHelper(this).getContactSources { sources ->
                var localAccountType = ""
                var localAccountName = ""
                sources.forEach {
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

    private fun getInactiveTabIndexes(activeIndex: Int) = (0 until tabsList.size).filter { it != activeIndex }

    private fun initFragments() {
        viewpager.offscreenPageLimit = tabsList.size - 1
        viewpager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrollStateChanged(state: Int) {
                if (isSearchOpen) {
                    getCurrentFragment()?.onSearchQueryChanged("")
                    searchMenuItem?.collapseActionView()
                }
            }

            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {}

            override fun onPageSelected(position: Int) {
                main_tabs_holder.getTabAt(position)?.select()
                getAllFragments().forEach {
                    it?.finishActMode()
                }
                invalidateOptionsMenu()
            }
        })

        viewpager.onGlobalLayout {
            refreshContacts(ALL_TABS_MASK)
        }

        main_tabs_holder.onTabSelectionChanged(
                tabUnselectedAction = {
                    it.icon?.applyColorFilter(config.textColor)
                },
                tabSelectedAction = {
                    if (isSearchOpen) {
                        getCurrentFragment()?.onSearchQueryChanged("")
                        searchMenuItem?.collapseActionView()
                    }
                    viewpager.currentItem = it.position
                    it.icon?.applyColorFilter(getAdjustedPrimaryColor())
                }
        )

        if (intent?.action == Intent.ACTION_VIEW && intent.data != null) {
            tryImportContactsFromFile(intent.data)
            intent.data = null
        }

        main_tabs_holder.removeAllTabs()
        var skippedTabs = 0
        tabsList.forEachIndexed { index, value ->
            if (config.showTabs and value == 0) {
                skippedTabs++
            } else {
                main_tabs_holder.addTab(main_tabs_holder.newTab().setIcon(getTabIcon(index)), index - skippedTabs, config.lastUsedViewPagerPage == index - skippedTabs)
            }
        }

        // selecting the proper tab sometimes glitches, add an extra selector to make sure we have it right
        main_tabs_holder.onGlobalLayout {
            Handler().postDelayed({
                if (intent?.action == Intent.ACTION_VIEW && intent.type == "vnd.android.cursor.dir/calls") {
                    main_tabs_holder.getTabAt(getRecentsTabIndex())?.select()
                } else {
                    main_tabs_holder.getTabAt(config.lastUsedViewPagerPage)?.select()
                }
                invalidateOptionsMenu()
            }, 100L)
        }

        main_tabs_holder.beVisibleIf(skippedTabs < tabsList.size - 1)

        main_dialpad_button.setOnClickListener {
            val intent = Intent(applicationContext, DialpadActivity::class.java)
            startActivity(intent)
        }
    }

    private fun getTabIcon(position: Int): Drawable {
        val drawableId = when (position) {
            LOCATION_CONTACTS_TAB -> R.drawable.ic_person
            LOCATION_FAVORITES_TAB -> R.drawable.ic_star_on
            LOCATION_RECENTS_TAB -> R.drawable.ic_clock
            else -> R.drawable.ic_group
        }

        return resources.getColoredDrawableWithColor(drawableId, config.textColor)
    }

    private fun showSortingDialog() {
        ChangeSortingDialog(this) {
            refreshContacts(CONTACTS_TAB_MASK or FAVORITES_TAB_MASK)
        }
    }

    fun showFilterDialog() {
        FilterContactSourcesDialog(this) {
            contacts_fragment?.forceListRedraw = true
            refreshContacts(CONTACTS_TAB_MASK or FAVORITES_TAB_MASK)
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
                    refreshContacts(ALL_TABS_MASK)
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

                try {
                    val inputStream = contentResolver.openInputStream(uri)
                    val out = FileOutputStream(tempFile)
                    inputStream.copyTo(out)
                    showImportContactsDialog(tempFile.absolutePath)
                } catch (e: Exception) {
                    showErrorToast(e)
                }
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
                    ContactsHelper(this).getContacts { allContacts ->
                        val contacts = allContacts.filter { contactSources.contains(it.source) }
                        if (contacts.isEmpty()) {
                            toast(R.string.no_entries_for_exporting)
                        } else {
                            VcfExporter().exportContacts(this, file, contacts as ArrayList<Contact>, true) { result ->
                                toast(when (result) {
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
        val licenses = LICENSE_JODA or LICENSE_GLIDE or LICENSE_GSON

        val faqItems = arrayListOf(
                FAQItem(R.string.faq_1_title, R.string.faq_1_text),
                FAQItem(R.string.faq_2_title_commons, R.string.faq_2_text_commons),
                FAQItem(R.string.faq_6_title_commons, R.string.faq_6_text_commons)
        )

        startAboutActivity(R.string.app_name, licenses, BuildConfig.VERSION_NAME, faqItems, true)
    }

    override fun refreshContacts(refreshTabsMask: Int) {
        if (isDestroyed || isGettingContacts) {
            return
        }

        isGettingContacts = true

        if (viewpager.adapter == null) {
            viewpager.adapter = ViewPagerAdapter(this)
            viewpager.currentItem = config.lastUsedViewPagerPage
        }

        ContactsHelper(this).getContacts {
            isGettingContacts = false
            if (isDestroyed) {
                return@getContacts
            }

            val contacts = it
            if (refreshTabsMask and CONTACTS_TAB_MASK != 0) {
                contacts_fragment?.refreshContacts(contacts)
            }

            if (refreshTabsMask and FAVORITES_TAB_MASK != 0) {
                favorites_fragment?.refreshContacts(contacts)
            }

            if (refreshTabsMask and RECENTS_TAB_MASK != 0) {
                recents_fragment?.refreshContacts(contacts)
            }

            if (refreshTabsMask and GROUPS_TAB_MASK != 0) {
                if (refreshTabsMask == GROUPS_TAB_MASK) {
                    groups_fragment.skipHashComparing = true
                }
                groups_fragment?.refreshContacts(contacts)
            }

            if (refreshTabsMask and RECENTS_TAB_MASK != 0) {
                ContactsHelper(this).getRecents {
                    it.filter { it.name == null }.forEach {
                        val namelessCall = it
                        val contact = contacts.firstOrNull { it.doesContainPhoneNumber(namelessCall.number) }
                        if (contact != null) {
                            it.name = contact.getNameToDisplay()
                        }
                    }

                    runOnUiThread {
                        recents_fragment?.updateRecentCalls(it)
                    }
                }
            }
        }
    }

    private fun getAllFragments() = arrayListOf(contacts_fragment, favorites_fragment, recents_fragment, groups_fragment)

    private fun getRecentsTabIndex(): Int {
        var index = 0
        if (config.showTabs and RECENTS_TAB_MASK == 0) {
            return index
        }

        if (config.showTabs and CONTACTS_TAB_MASK != 0) {
            index++
        }

        if (config.showTabs and FAVORITES_TAB_MASK != 0) {
            index++
        }
        return index
    }

    private fun checkWhatsNewDialog() {
        arrayListOf<Release>().apply {
            add(Release(10, R.string.release_10))
            add(Release(11, R.string.release_11))
            add(Release(16, R.string.release_16))
            add(Release(27, R.string.release_27))
            add(Release(29, R.string.release_29))
            add(Release(31, R.string.release_31))
            add(Release(32, R.string.release_32))
            add(Release(34, R.string.release_34))
            add(Release(39, R.string.release_39))
            checkWhatsNew(this, BuildConfig.VERSION_CODE)
        }
    }
}
