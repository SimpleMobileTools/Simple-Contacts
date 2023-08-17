package com.simplemobiletools.contacts.pro.activities

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Icon
import android.graphics.drawable.LayerDrawable
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.viewpager.widget.ViewPager
import com.simplemobiletools.commons.databases.ContactsDatabase
import com.simplemobiletools.commons.databinding.BottomTablayoutItemBinding
import com.simplemobiletools.commons.dialogs.ChangeViewTypeDialog
import com.simplemobiletools.commons.dialogs.ConfirmationDialog
import com.simplemobiletools.commons.dialogs.FilePickerDialog
import com.simplemobiletools.commons.dialogs.RadioGroupDialog
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.helpers.*
import com.simplemobiletools.commons.models.FAQItem
import com.simplemobiletools.commons.models.RadioItem
import com.simplemobiletools.commons.models.Release
import com.simplemobiletools.commons.models.contacts.Contact
import com.simplemobiletools.contacts.pro.BuildConfig
import com.simplemobiletools.contacts.pro.R
import com.simplemobiletools.contacts.pro.adapters.ViewPagerAdapter
import com.simplemobiletools.contacts.pro.databinding.ActivityMainBinding
import com.simplemobiletools.contacts.pro.dialogs.ChangeSortingDialog
import com.simplemobiletools.contacts.pro.dialogs.ExportContactsDialog
import com.simplemobiletools.contacts.pro.dialogs.FilterContactSourcesDialog
import com.simplemobiletools.contacts.pro.dialogs.ImportContactsDialog
import com.simplemobiletools.contacts.pro.extensions.config
import com.simplemobiletools.contacts.pro.extensions.handleGenericContactClick
import com.simplemobiletools.contacts.pro.fragments.FavoritesFragment
import com.simplemobiletools.contacts.pro.fragments.MyViewPagerFragment
import com.simplemobiletools.contacts.pro.helpers.ALL_TABS_MASK
import com.simplemobiletools.contacts.pro.helpers.VcfExporter
import com.simplemobiletools.contacts.pro.helpers.tabsList
import com.simplemobiletools.contacts.pro.interfaces.RefreshContactsListener
import me.grantland.widget.AutofitHelper
import java.io.FileOutputStream
import java.io.OutputStream
import java.util.*

class MainActivity : SimpleActivity(), RefreshContactsListener {
    companion object {
        private const val PICK_IMPORT_SOURCE_INTENT = 1
        private const val PICK_EXPORT_FILE_INTENT = 2
    }

    private var werePermissionsHandled = false
    private var isFirstResume = true
    private var isGettingContacts = false
    private var ignoredExportContactSources = HashSet<String>()

    private var storedShowContactThumbnails = false
    private var storedShowPhoneNumbers = false
    private var storedStartNameWithSurname = false
    private var storedFontSize = 0
    private var storedShowTabs = 0
    private val binding by viewBinding(ActivityMainBinding::inflate)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        appLaunched(BuildConfig.APPLICATION_ID)
        setupOptionsMenu()
        refreshMenuItems()
        updateMaterialActivityViews(binding.mainCoordinator, binding.mainHolder, useTransparentNavigation = false, useTopSearchMenu = true)
        storeStateVariables()
        setupTabs()
        checkContactPermissions()
        checkWhatsNewDialog()

        if (isPackageInstalled("com.simplemobiletools.contacts")) {
            val dialogText = getString(com.simplemobiletools.commons.R.string.upgraded_from_free_contacts, getString(R.string.phone_storage_hidden))
            ConfirmationDialog(this, dialogText, 0, com.simplemobiletools.commons.R.string.ok, 0, false) {}
        }
    }

    private fun checkContactPermissions() {
        handlePermission(PERMISSION_READ_CONTACTS) {
            werePermissionsHandled = true
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
        if (storedShowPhoneNumbers != config.showPhoneNumbers) {
            System.exit(0)
            return
        }

        if (storedShowTabs != config.showTabs) {
            config.lastUsedViewPagerPage = 0
            finish()
            startActivity(intent)
            return
        }

        val configShowContactThumbnails = config.showContactThumbnails
        if (storedShowContactThumbnails != configShowContactThumbnails) {
            getAllFragments().forEach {
                it?.showContactThumbnailsChanged(configShowContactThumbnails)
            }
        }

        val properPrimaryColor = getProperPrimaryColor()
        binding.mainTabsHolder.background = ColorDrawable(getProperBackgroundColor())
        binding.mainTabsHolder.setSelectedTabIndicatorColor(properPrimaryColor)
        getAllFragments().forEach {
            it?.setupColors(getProperTextColor(), properPrimaryColor)
        }

        updateMenuColors()
        setupTabColors()

        val configStartNameWithSurname = config.startNameWithSurname
        if (storedStartNameWithSurname != configStartNameWithSurname) {
            findViewById<MyViewPagerFragment<*>>(R.id.contacts_fragment)?.startNameWithSurnameChanged(configStartNameWithSurname)
            findViewById<MyViewPagerFragment<*>>(R.id.favorites_fragment)?.startNameWithSurnameChanged(configStartNameWithSurname)
        }

        val configFontSize = config.fontSize
        if (storedFontSize != configFontSize) {
            getAllFragments().forEach {
                it?.fontSizeChanged()
            }
        }

        if (werePermissionsHandled && !isFirstResume) {
            if (binding.viewPager.adapter == null) {
                initFragments()
            } else {
                refreshContacts(ALL_TABS_MASK)
            }
        }

        val dialpadIcon =
            resources.getColoredDrawableWithColor(com.simplemobiletools.commons.R.drawable.ic_dialpad_vector, properPrimaryColor.getContrastColor())
        binding.mainDialpadButton.apply {
            setImageDrawable(dialpadIcon)
            background.applyColorFilter(properPrimaryColor)
            beVisibleIf(config.showDialpadButton)
        }

        isFirstResume = false
        checkShortcuts()
    }

    override fun onPause() {
        super.onPause()
        storeStateVariables()
        config.lastUsedViewPagerPage = binding.viewPager.currentItem
    }

    override fun onDestroy() {
        super.onDestroy()
        if (!isChangingConfigurations) {
            ContactsDatabase.destroyInstance()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        super.onActivityResult(requestCode, resultCode, resultData)
        if (requestCode == PICK_IMPORT_SOURCE_INTENT && resultCode == Activity.RESULT_OK && resultData?.data != null) {
            tryImportContactsFromFile(resultData.data!!)
        } else if (requestCode == PICK_EXPORT_FILE_INTENT && resultCode == Activity.RESULT_OK && resultData?.data != null) {
            try {
                val outputStream = contentResolver.openOutputStream(resultData.data!!)
                exportContactsTo(ignoredExportContactSources, outputStream)
            } catch (e: Exception) {
                showErrorToast(e)
            }
        }
    }

    override fun onBackPressed() {
        if (binding.mainMenu.isSearchOpen) {
            binding.mainMenu.closeSearch()
        } else {
            super.onBackPressed()
        }
    }

    private fun refreshMenuItems() {
        val currentFragment = getCurrentFragment()
        binding.mainMenu.getToolbar().menu.apply {
            findItem(R.id.sort).isVisible = currentFragment != findViewById(R.id.groups_fragment)
            findItem(R.id.filter).isVisible = currentFragment != findViewById(R.id.groups_fragment)
            findItem(R.id.dialpad).isVisible = !config.showDialpadButton
            findItem(R.id.change_view_type).isVisible = currentFragment == findViewById(R.id.favorites_fragment)
            findItem(R.id.column_count).isVisible = currentFragment == findViewById(R.id.favorites_fragment) && config.viewType == VIEW_TYPE_GRID
            findItem(R.id.more_apps_from_us).isVisible = !resources.getBoolean(com.simplemobiletools.commons.R.bool.hide_google_relations)
        }
    }

    private fun setupOptionsMenu() {
        binding.mainMenu.getToolbar().inflateMenu(R.menu.menu)
        binding.mainMenu.toggleHideOnScroll(false)
        binding.mainMenu.setupMenu()

        binding.mainMenu.onSearchClosedListener = {
            getAllFragments().forEach {
                it?.onSearchClosed()
            }
        }

        binding.mainMenu.onSearchTextChangedListener = { text ->
            getCurrentFragment()?.onSearchQueryChanged(text)
        }

        binding.mainMenu.getToolbar().setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.sort -> showSortingDialog(showCustomSorting = getCurrentFragment() is FavoritesFragment)
                R.id.filter -> showFilterDialog()
                R.id.dialpad -> launchDialpad()
                R.id.import_contacts -> tryImportContacts()
                R.id.export_contacts -> tryExportContacts()
                R.id.more_apps_from_us -> launchMoreAppsFromUsIntent()
                R.id.change_view_type -> changeViewType()
                R.id.column_count -> changeColumnCount()
                R.id.settings -> launchSettings()
                R.id.about -> launchAbout()
                else -> return@setOnMenuItemClickListener false
            }
            return@setOnMenuItemClickListener true
        }
    }

    private fun changeViewType() {
        ChangeViewTypeDialog(this) {
            refreshMenuItems()
            findViewById<FavoritesFragment>(R.id.favorites_fragment)?.updateFavouritesAdapter()
        }
    }

    private fun changeColumnCount() {
        val items = ArrayList<RadioItem>()
        for (i in 1..CONTACTS_GRID_MAX_COLUMNS_COUNT) {
            items.add(RadioItem(i, resources.getQuantityString(com.simplemobiletools.commons.R.plurals.column_counts, i, i)))
        }

        val currentColumnCount = config.contactsGridColumnCount
        RadioGroupDialog(this, items, currentColumnCount) {
            val newColumnCount = it as Int
            if (currentColumnCount != newColumnCount) {
                config.contactsGridColumnCount = newColumnCount
                findViewById<FavoritesFragment>(R.id.favorites_fragment)?.columnCountChanged()
            }
        }
    }

    private fun updateMenuColors() {
        updateStatusbarColor(getProperBackgroundColor())
        binding.mainMenu.updateColors()
    }

    private fun storeStateVariables() {
        config.apply {
            storedShowContactThumbnails = showContactThumbnails
            storedShowPhoneNumbers = showPhoneNumbers
            storedStartNameWithSurname = startNameWithSurname
            storedShowTabs = showTabs
            storedFontSize = fontSize
        }
    }

    @SuppressLint("NewApi")
    private fun checkShortcuts() {
        val appIconColor = config.appIconColor
        if (isNougatMR1Plus() && config.lastHandledShortcutColor != appIconColor) {
            val createNewContact = getCreateNewContactShortcut(appIconColor)

            try {
                shortcutManager.dynamicShortcuts = Arrays.asList(createNewContact)
                config.lastHandledShortcutColor = appIconColor
            } catch (ignored: Exception) {
            }
        }
    }

    @SuppressLint("NewApi")
    private fun getCreateNewContactShortcut(appIconColor: Int): ShortcutInfo {
        val newEvent = getString(com.simplemobiletools.commons.R.string.create_new_contact)
        val drawable = resources.getDrawable(com.simplemobiletools.commons.R.drawable.shortcut_plus)
        (drawable as LayerDrawable).findDrawableByLayerId(com.simplemobiletools.commons.R.id.shortcut_plus_background).applyColorFilter(appIconColor)
        val bmp = drawable.convertToBitmap()

        val intent = Intent(this, EditContactActivity::class.java)
        intent.action = Intent.ACTION_VIEW
        return ShortcutInfo.Builder(this, "create_new_contact")
            .setShortLabel(newEvent)
            .setLongLabel(newEvent)
            .setIcon(Icon.createWithBitmap(bmp))
            .setIntent(intent)
            .build()
    }

    private fun getCurrentFragment(): MyViewPagerFragment<*>? {
        val showTabs = config.showTabs
        val fragments = arrayListOf<MyViewPagerFragment<*>>()
        if (showTabs and TAB_CONTACTS != 0) {
            fragments.add(findViewById(R.id.contacts_fragment))
        }

        if (showTabs and TAB_FAVORITES != 0) {
            fragments.add(findViewById(R.id.favorites_fragment))
        }

        if (showTabs and TAB_GROUPS != 0) {
            fragments.add(findViewById(R.id.groups_fragment))
        }

        return fragments.getOrNull(binding.viewPager.currentItem)
    }

    private fun setupTabColors() {
        val activeView = binding.mainTabsHolder.getTabAt(binding.viewPager.currentItem)?.customView
        updateBottomTabItemColors(activeView, true, getSelectedTabDrawableIds()[binding.viewPager.currentItem])

        getInactiveTabIndexes(binding.viewPager.currentItem).forEach { index ->
            val inactiveView = binding.mainTabsHolder.getTabAt(index)?.customView
            updateBottomTabItemColors(inactiveView, false, getDeselectedTabDrawableIds()[index])
        }

        val bottomBarColor = getBottomNavigationBackgroundColor()
        binding.mainTabsHolder.setBackgroundColor(bottomBarColor)
        updateNavigationBarColor(bottomBarColor)
    }

    private fun getInactiveTabIndexes(activeIndex: Int) = (0 until binding.mainTabsHolder.tabCount).filter { it != activeIndex }

    private fun getSelectedTabDrawableIds(): ArrayList<Int> {
        val showTabs = config.showTabs
        val icons = ArrayList<Int>()

        if (showTabs and TAB_CONTACTS != 0) {
            icons.add(com.simplemobiletools.commons.R.drawable.ic_person_vector)
        }

        if (showTabs and TAB_FAVORITES != 0) {
            icons.add(com.simplemobiletools.commons.R.drawable.ic_star_vector)
        }

        if (showTabs and TAB_GROUPS != 0) {
            icons.add(com.simplemobiletools.commons.R.drawable.ic_people_vector)
        }

        return icons
    }

    private fun getDeselectedTabDrawableIds(): ArrayList<Int> {
        val showTabs = config.showTabs
        val icons = ArrayList<Int>()

        if (showTabs and TAB_CONTACTS != 0) {
            icons.add(com.simplemobiletools.commons.R.drawable.ic_person_outline_vector)
        }

        if (showTabs and TAB_FAVORITES != 0) {
            icons.add(com.simplemobiletools.commons.R.drawable.ic_star_outline_vector)
        }

        if (showTabs and TAB_GROUPS != 0) {
            icons.add(com.simplemobiletools.commons.R.drawable.ic_people_outline_vector)
        }

        return icons
    }

    private fun initFragments() {
        binding.viewPager.offscreenPageLimit = tabsList.size - 1
        binding.viewPager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrollStateChanged(state: Int) {}

            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {}

            override fun onPageSelected(position: Int) {
                binding.mainTabsHolder.getTabAt(position)?.select()
                getAllFragments().forEach {
                    it?.finishActMode()
                }
                refreshMenuItems()
            }
        })

        binding.viewPager.onGlobalLayout {
            refreshContacts(ALL_TABS_MASK)
            refreshMenuItems()
        }

        if (intent?.action == Intent.ACTION_VIEW && intent.data != null) {
            tryImportContactsFromFile(intent.data!!)
            intent.data = null
        }

        binding.mainDialpadButton.setOnClickListener {
            launchDialpad()
        }
    }

    private fun setupTabs() {
        binding.mainTabsHolder.removeAllTabs()
        tabsList.forEachIndexed { index, value ->
            if (config.showTabs and value != 0) {
                binding.mainTabsHolder.newTab().setCustomView(com.simplemobiletools.commons.R.layout.bottom_tablayout_item).apply tab@{
                    customView
                        ?.let { BottomTablayoutItemBinding.bind(it) }
                        ?.apply {
                            tabItemIcon.setImageDrawable(getTabIcon(index))
                            tabItemLabel.text = getTabLabel(index)
                            AutofitHelper.create(tabItemLabel)
                            binding.mainTabsHolder.addTab(this@tab)
                        }
                }
            }
        }

        binding.mainTabsHolder.onTabSelectionChanged(
            tabUnselectedAction = {
                updateBottomTabItemColors(it.customView, false, getDeselectedTabDrawableIds()[it.position])
            },
            tabSelectedAction = {
                binding.mainMenu.closeSearch()
                binding.viewPager.currentItem = it.position
                updateBottomTabItemColors(it.customView, true, getSelectedTabDrawableIds()[it.position])
            }
        )

        binding.mainTabsHolder.beGoneIf(binding.mainTabsHolder.tabCount == 1)
    }

    private fun showSortingDialog(showCustomSorting: Boolean) {
        ChangeSortingDialog(this, showCustomSorting) {
            refreshContacts(TAB_CONTACTS or TAB_FAVORITES)
        }
    }

    fun showFilterDialog() {
        FilterContactSourcesDialog(this) {
            findViewById<MyViewPagerFragment<*>>(R.id.contacts_fragment)?.forceListRedraw = true
            refreshContacts(TAB_CONTACTS or TAB_FAVORITES)
        }
    }

    private fun launchDialpad() {
        hideKeyboard()
        Intent(Intent.ACTION_DIAL).apply {
            try {
                startActivity(this)
            } catch (e: ActivityNotFoundException) {
                toast(com.simplemobiletools.commons.R.string.no_app_found)
            } catch (e: Exception) {
                showErrorToast(e)
            }
        }
    }

    private fun tryImportContacts() {
        if (isQPlus()) {
            Intent(Intent.ACTION_GET_CONTENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "text/x-vcard"

                try {
                    startActivityForResult(this, PICK_IMPORT_SOURCE_INTENT)
                } catch (e: ActivityNotFoundException) {
                    toast(com.simplemobiletools.commons.R.string.system_service_disabled, Toast.LENGTH_LONG)
                } catch (e: Exception) {
                    showErrorToast(e)
                }
            }
        } else {
            handlePermission(PERMISSION_READ_STORAGE) {
                if (it) {
                    importContacts()
                }
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
            uri.scheme == "file" -> showImportContactsDialog(uri.path!!)
            uri.scheme == "content" -> {
                val tempFile = getTempFile()
                if (tempFile == null) {
                    toast(com.simplemobiletools.commons.R.string.unknown_error_occurred)
                    return
                }

                try {
                    val inputStream = contentResolver.openInputStream(uri)
                    val out = FileOutputStream(tempFile)
                    inputStream!!.copyTo(out)
                    showImportContactsDialog(tempFile.absolutePath)
                } catch (e: Exception) {
                    showErrorToast(e)
                }
            }

            else -> toast(com.simplemobiletools.commons.R.string.invalid_file_format)
        }
    }

    private fun tryExportContacts() {
        if (isQPlus()) {
            ExportContactsDialog(this, config.lastExportPath, true) { file, ignoredContactSources ->
                ignoredExportContactSources = ignoredContactSources

                Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                    type = "text/x-vcard"
                    putExtra(Intent.EXTRA_TITLE, file.name)
                    addCategory(Intent.CATEGORY_OPENABLE)

                    try {
                        startActivityForResult(this, PICK_EXPORT_FILE_INTENT)
                    } catch (e: ActivityNotFoundException) {
                        toast(com.simplemobiletools.commons.R.string.no_app_found, Toast.LENGTH_LONG)
                    } catch (e: Exception) {
                        showErrorToast(e)
                    }
                }
            }
        } else {
            handlePermission(PERMISSION_WRITE_STORAGE) {
                if (it) {
                    ExportContactsDialog(this, config.lastExportPath, false) { file, ignoredContactSources ->
                        getFileOutputStream(file.toFileDirItem(this), true) {
                            exportContactsTo(ignoredContactSources, it)
                        }
                    }
                }
            }
        }
    }

    private fun exportContactsTo(ignoredContactSources: HashSet<String>, outputStream: OutputStream?) {
        ContactsHelper(this).getContacts(true, false, ignoredContactSources) { contacts ->
            if (contacts.isEmpty()) {
                toast(com.simplemobiletools.commons.R.string.no_entries_for_exporting)
            } else {
                VcfExporter().exportContacts(this, outputStream, contacts, true) { result ->
                    toast(
                        when (result) {
                            VcfExporter.ExportResult.EXPORT_OK -> com.simplemobiletools.commons.R.string.exporting_successful
                            VcfExporter.ExportResult.EXPORT_PARTIAL -> com.simplemobiletools.commons.R.string.exporting_some_entries_failed
                            else -> com.simplemobiletools.commons.R.string.exporting_failed
                        }
                    )
                }
            }
        }
    }

    private fun launchSettings() {
        hideKeyboard()
        startActivity(Intent(applicationContext, SettingsActivity::class.java))
    }

    private fun launchAbout() {
        val licenses = LICENSE_JODA or LICENSE_GLIDE or LICENSE_GSON or LICENSE_INDICATOR_FAST_SCROLL or LICENSE_AUTOFITTEXTVIEW

        val faqItems = arrayListOf(
            FAQItem(R.string.faq_1_title, R.string.faq_1_text),
            FAQItem(com.simplemobiletools.commons.R.string.faq_9_title_commons, com.simplemobiletools.commons.R.string.faq_9_text_commons)
        )

        if (!resources.getBoolean(com.simplemobiletools.commons.R.bool.hide_google_relations)) {
            faqItems.add(FAQItem(com.simplemobiletools.commons.R.string.faq_2_title_commons, com.simplemobiletools.commons.R.string.faq_2_text_commons))
            faqItems.add(FAQItem(com.simplemobiletools.commons.R.string.faq_6_title_commons, com.simplemobiletools.commons.R.string.faq_6_text_commons))
            faqItems.add(FAQItem(com.simplemobiletools.commons.R.string.faq_7_title_commons, com.simplemobiletools.commons.R.string.faq_7_text_commons))
        }

        startAboutActivity(R.string.app_name, licenses, BuildConfig.VERSION_NAME, faqItems, true)
    }

    override fun refreshContacts(refreshTabsMask: Int) {
        if (isDestroyed || isFinishing || isGettingContacts) {
            return
        }

        isGettingContacts = true

        if (binding.viewPager.adapter == null) {
            binding.viewPager.adapter = ViewPagerAdapter(this, tabsList, config.showTabs)
            binding.viewPager.currentItem = getDefaultTab()
        }

        ContactsHelper(this).getContacts { contacts ->
            isGettingContacts = false
            if (isDestroyed || isFinishing) {
                return@getContacts
            }

            if (refreshTabsMask and TAB_CONTACTS != 0) {
                findViewById<MyViewPagerFragment<*>>(R.id.contacts_fragment)?.apply {
                    skipHashComparing = true
                    refreshContacts(contacts)
                }
            }

            if (refreshTabsMask and TAB_FAVORITES != 0) {
                findViewById<MyViewPagerFragment<*>>(R.id.favorites_fragment)?.apply {
                    skipHashComparing = true
                    refreshContacts(contacts)
                }
            }

            if (refreshTabsMask and TAB_GROUPS != 0) {
                findViewById<MyViewPagerFragment<*>>(R.id.groups_fragment)?.apply {
                    if (refreshTabsMask == TAB_GROUPS) {
                        skipHashComparing = true
                    }
                    refreshContacts(contacts)
                }
            }

            if (binding.mainMenu.isSearchOpen) {
                getCurrentFragment()?.onSearchQueryChanged(binding.mainMenu.getCurrentQuery())
            }
        }
    }

    override fun contactClicked(contact: Contact) {
        handleGenericContactClick(contact)
    }

    private fun getAllFragments() = arrayListOf<MyViewPagerFragment<*>?>(
        findViewById(R.id.contacts_fragment),
        findViewById(R.id.favorites_fragment),
        findViewById(R.id.groups_fragment)
    )

    private fun getDefaultTab(): Int {
        val showTabsMask = config.showTabs
        return when (config.defaultTab) {
            TAB_LAST_USED -> config.lastUsedViewPagerPage
            TAB_CONTACTS -> 0
            TAB_FAVORITES -> if (showTabsMask and TAB_CONTACTS > 0) 1 else 0
            else -> {
                if (showTabsMask and TAB_GROUPS > 0) {
                    if (showTabsMask and TAB_CONTACTS > 0) {
                        if (showTabsMask and TAB_FAVORITES > 0) {
                            2
                        } else {
                            1
                        }
                    } else {
                        if (showTabsMask and TAB_FAVORITES > 0) {
                            1
                        } else {
                            0
                        }
                    }
                } else {
                    0
                }
            }
        }
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
            add(Release(40, R.string.release_40))
            add(Release(47, R.string.release_47))
            add(Release(56, R.string.release_56))
            checkWhatsNew(this, BuildConfig.VERSION_CODE)
        }
    }
}
