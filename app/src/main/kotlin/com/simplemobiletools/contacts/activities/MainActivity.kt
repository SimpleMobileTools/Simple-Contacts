package com.simplemobiletools.contacts.activities

import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.support.v4.view.MenuItemCompat
import android.support.v4.view.ViewPager
import android.support.v7.widget.SearchView
import android.view.Menu
import android.view.MenuItem
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.helpers.*
import com.simplemobiletools.contacts.BuildConfig
import com.simplemobiletools.contacts.R
import com.simplemobiletools.contacts.adapters.ViewPagerAdapter
import com.simplemobiletools.contacts.dialogs.ChangeSortingDialog
import com.simplemobiletools.contacts.dialogs.FilterContactSourcesDialog
import com.simplemobiletools.contacts.extensions.config
import com.simplemobiletools.contacts.extensions.onTabSelectionChanged
import com.simplemobiletools.contacts.interfaces.RefreshContactsListener
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_contacts.*
import kotlinx.android.synthetic.main.fragment_favorites.*

class MainActivity : SimpleActivity(), RefreshContactsListener {
    private var isFirstResume = true
    private var isSearchOpen = false
    private var searchMenuItem: MenuItem? = null

    private var storedUseEnglish = false
    private var storedTextColor = 0
    private var storedBackgroundColor = 0
    private var storedPrimaryColor = 0
    private var storedShowPhoneNumbers = false
    private var storedStartNameWithSurname = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        appLaunched()
        setupTabColors()

        handlePermission(PERMISSION_READ_CONTACTS) {
            if (it) {
                handlePermission(PERMISSION_WRITE_CONTACTS) {
                    if (it) {
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

        val configTextColor = config.textColor
        if (storedTextColor != configTextColor) {
            main_tabs_holder.getTabAt(getOtherViewPagerItem(viewpager.currentItem))?.icon?.applyColorFilter(configTextColor)
            contacts_fragment.textColorChanged(configTextColor)
            favorites_fragment.textColorChanged(configTextColor)
        }

        val configBackgroundColor = config.backgroundColor
        if (storedBackgroundColor != configBackgroundColor) {
            main_tabs_holder.background = ColorDrawable(configBackgroundColor)
        }

        val configPrimaryColor = config.primaryColor
        if (storedPrimaryColor != configPrimaryColor) {
            main_tabs_holder.setSelectedTabIndicatorColor(getAdjustedPrimaryColor())
            main_tabs_holder.getTabAt(viewpager.currentItem)?.icon?.applyColorFilter(getAdjustedPrimaryColor())
            contacts_fragment.primaryColorChanged(configPrimaryColor)
            favorites_fragment.primaryColorChanged(configPrimaryColor)
        }

        val configStartNameWithSurname = config.startNameWithSurname
        if (storedStartNameWithSurname != configStartNameWithSurname) {
            contacts_fragment.startNameWithSurnameChanged(configStartNameWithSurname)
            favorites_fragment.startNameWithSurnameChanged(configStartNameWithSurname)
        }

        if (!isFirstResume) {
            if (viewpager.adapter == null) {
                initFragments()
            }
            contacts_fragment.initContacts()
            contacts_fragment.onActivityResume()
            favorites_fragment.initContacts()
            favorites_fragment.onActivityResume()
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
                        getCurrentFragment().onSearchQueryChanged(newText)
                    }
                    return true
                }
            })
        }

        MenuItemCompat.setOnActionExpandListener(searchMenuItem, object : MenuItemCompat.OnActionExpandListener {
            override fun onMenuItemActionExpand(item: MenuItem?): Boolean {
                getCurrentFragment().onSearchOpened()
                isSearchOpen = true
                return true
            }

            override fun onMenuItemActionCollapse(item: MenuItem?): Boolean {
                getCurrentFragment().onSearchClosed()
                isSearchOpen = false
                return true
            }
        })
    }

    private fun getCurrentFragment() = if (viewpager.currentItem == 0) contacts_fragment else favorites_fragment

    private fun setupTabColors() {
        val lastUsedPage = config.lastUsedViewPagerPage
        main_tabs_holder.apply {
            background = ColorDrawable(config.backgroundColor)
            setSelectedTabIndicatorColor(getAdjustedPrimaryColor())
            getTabAt(lastUsedPage)?.select()
            getTabAt(lastUsedPage)?.icon?.applyColorFilter(getAdjustedPrimaryColor())
            getTabAt(getOtherViewPagerItem(lastUsedPage))?.icon?.applyColorFilter(config.textColor)
        }
    }

    private fun getOtherViewPagerItem(used: Int) = if (used == 1) 0 else 1

    private fun initFragments() {
        viewpager.adapter = ViewPagerAdapter(this)
        viewpager.setOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrollStateChanged(state: Int) {
                if (isSearchOpen) {
                    getCurrentFragment().onSearchQueryChanged("")
                    if (searchMenuItem != null) {
                        MenuItemCompat.collapseActionView(searchMenuItem)
                    }
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
    }

    private fun showSortingDialog() {
        ChangeSortingDialog(this) {
            contacts_fragment.initContacts()
            favorites_fragment.initContacts()
        }
    }

    fun showFilterDialog() {
        FilterContactSourcesDialog(this) {
            contacts_fragment.initContacts()
        }
    }

    private fun launchAbout() {
        startAboutActivity(R.string.app_name, LICENSE_KOTLIN or LICENSE_MULTISELECT or LICENSE_JODA or LICENSE_GLIDE, BuildConfig.VERSION_NAME)
    }

    override fun refreshContacts() {
        contacts_fragment.initContacts()
        favorites_fragment.initContacts()
    }

    override fun refreshFavorites() {
        favorites_fragment?.initContacts()
    }
}
