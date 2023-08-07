package com.simplemobiletools.contacts.pro.activities

import android.os.Bundle
import com.simplemobiletools.commons.dialogs.RadioGroupDialog
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.helpers.*
import com.simplemobiletools.commons.models.RadioItem
import com.simplemobiletools.contacts.pro.R
import com.simplemobiletools.contacts.pro.databinding.ActivitySettingsBinding
import com.simplemobiletools.contacts.pro.dialogs.ManageAutoBackupsDialog
import com.simplemobiletools.contacts.pro.dialogs.ManageVisibleFieldsDialog
import com.simplemobiletools.contacts.pro.dialogs.ManageVisibleTabsDialog
import com.simplemobiletools.contacts.pro.extensions.cancelScheduledAutomaticBackup
import com.simplemobiletools.contacts.pro.extensions.config
import com.simplemobiletools.contacts.pro.extensions.scheduleNextAutomaticBackup
import java.util.Locale
import kotlin.system.exitProcess

class SettingsActivity : SimpleActivity() {

    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        isMaterialActivity = true
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        updateMaterialActivityViews(binding.settingsCoordinator, binding.settingsHolder, useTransparentNavigation = true, useTopSearchMenu = false)
        setupMaterialScrollListener(binding.settingsNestedScrollview, binding.settingsToolbar)
    }

    override fun onResume() {
        super.onResume()
        setupToolbar(binding.settingsToolbar, NavigationIcon.Arrow)

        setupCustomizeColors()
        setupManageShownContactFields()
        setupManageShownTabs()
        setupFontSize()
        setupUseEnglish()
        setupLanguage()
        setupShowContactThumbnails()
        setupShowPhoneNumbers()
        setupShowContactsWithNumbers()
        setupStartNameWithSurname()
        setupMergeDuplicateContacts()
        setupShowCallConfirmation()
        setupShowDialpadButton()
        setupShowPrivateContacts()
        setupOnContactClick()
        setupDefaultTab()
        setupEnableAutomaticBackups()
        setupManageAutomaticBackups()
        updateTextColors(binding.settingsHolder)

        arrayOf(
            binding.settingsColorCustomizationSectionLabel,
            binding.settingsGeneralSettingsLabel,
            binding.settingsMainScreenLabel,
            binding.settingsListViewLabel,
            binding.settingsBackupsLabel
        ).forEach {
            it.setTextColor(getProperPrimaryColor())
        }
    }

    private fun setupCustomizeColors() {
        binding.settingsColorCustomizationHolder.setOnClickListener {
            startCustomizationActivity()
        }
    }

    private fun setupManageShownContactFields() {
        binding.settingsManageContactFieldsHolder.setOnClickListener {
            ManageVisibleFieldsDialog(this) {}
        }
    }

    private fun setupManageShownTabs() {
        binding.settingsManageShownTabsHolder.setOnClickListener {
            ManageVisibleTabsDialog(this)
        }
    }

    private fun setupDefaultTab() {
        binding.settingsDefaultTab.text = getDefaultTabText()
        binding.settingsDefaultTabHolder.setOnClickListener {
            val items = arrayListOf(
                RadioItem(TAB_CONTACTS, getString(com.simplemobiletools.commons.R.string.contacts_tab)),
                RadioItem(TAB_FAVORITES, getString(com.simplemobiletools.commons.R.string.favorites_tab)),
                RadioItem(TAB_GROUPS, getString(com.simplemobiletools.commons.R.string.groups_tab)),
                RadioItem(TAB_LAST_USED, getString(com.simplemobiletools.commons.R.string.last_used_tab))
            )

            RadioGroupDialog(this@SettingsActivity, items, config.defaultTab) {
                config.defaultTab = it as Int
                binding.settingsDefaultTab.text = getDefaultTabText()
            }
        }
    }

    private fun getDefaultTabText() = getString(
        when (baseConfig.defaultTab) {
            TAB_CONTACTS -> com.simplemobiletools.commons.R.string.contacts_tab
            TAB_FAVORITES -> com.simplemobiletools.commons.R.string.favorites_tab
            TAB_GROUPS -> com.simplemobiletools.commons.R.string.groups_tab
            else -> com.simplemobiletools.commons.R.string.last_used_tab
        }
    )

    private fun setupFontSize() {
        binding.settingsFontSize.text = getFontSizeText()
        binding.settingsFontSizeHolder.setOnClickListener {
            val items = arrayListOf(
                RadioItem(FONT_SIZE_SMALL, getString(com.simplemobiletools.commons.R.string.small)),
                RadioItem(FONT_SIZE_MEDIUM, getString(com.simplemobiletools.commons.R.string.medium)),
                RadioItem(FONT_SIZE_LARGE, getString(com.simplemobiletools.commons.R.string.large)),
                RadioItem(FONT_SIZE_EXTRA_LARGE, getString(com.simplemobiletools.commons.R.string.extra_large))
            )

            RadioGroupDialog(this@SettingsActivity, items, config.fontSize) {
                config.fontSize = it as Int
                binding.settingsFontSize.text = getFontSizeText()
            }
        }
    }

    private fun setupUseEnglish() {
        binding.settingsUseEnglishHolder.beVisibleIf((config.wasUseEnglishToggled || Locale.getDefault().language != "en") && !isTiramisuPlus())
        binding.settingsUseEnglish.isChecked = config.useEnglish
        binding.settingsUseEnglishHolder.setOnClickListener {
            binding.settingsUseEnglish.toggle()
            config.useEnglish = binding.settingsUseEnglish.isChecked
            exitProcess(0)
        }
    }

    private fun setupLanguage() {
        binding.settingsLanguage.text = Locale.getDefault().displayLanguage
        binding.settingsLanguageHolder.beVisibleIf(isTiramisuPlus())
        binding.settingsLanguageHolder.setOnClickListener {
            launchChangeAppLanguageIntent()
        }
    }

    private fun setupShowContactThumbnails() {
        binding.settingsShowContactThumbnails.isChecked = config.showContactThumbnails
        binding.settingsShowContactThumbnailsHolder.setOnClickListener {
            binding.settingsShowContactThumbnails.toggle()
            config.showContactThumbnails = binding.settingsShowContactThumbnails.isChecked
        }
    }

    private fun setupShowPhoneNumbers() {
        binding.settingsShowPhoneNumbers.isChecked = config.showPhoneNumbers
        binding.settingsShowPhoneNumbersHolder.setOnClickListener {
            binding.settingsShowPhoneNumbers.toggle()
            config.showPhoneNumbers = binding.settingsShowPhoneNumbers.isChecked
        }
    }

    private fun setupShowContactsWithNumbers() {
        binding.settingsShowOnlyContactsWithNumbers.isChecked = config.showOnlyContactsWithNumbers
        binding.settingsShowOnlyContactsWithNumbersHolder.setOnClickListener {
            binding.settingsShowOnlyContactsWithNumbers.toggle()
            config.showOnlyContactsWithNumbers = binding.settingsShowOnlyContactsWithNumbers.isChecked
        }
    }

    private fun setupStartNameWithSurname() {
        binding.settingsStartNameWithSurname.isChecked = config.startNameWithSurname
        binding.settingsStartNameWithSurnameHolder.setOnClickListener {
            binding.settingsStartNameWithSurname.toggle()
            config.startNameWithSurname = binding.settingsStartNameWithSurname.isChecked
        }
    }

    private fun setupShowDialpadButton() {
        binding.settingsShowDialpadButton.isChecked = config.showDialpadButton
        binding.settingsShowDialpadButtonHolder.setOnClickListener {
            binding.settingsShowDialpadButton.toggle()
            config.showDialpadButton = binding.settingsShowDialpadButton.isChecked
        }
    }

    private fun setupShowPrivateContacts() {
        binding.settingsShowPrivateContacts.isChecked = config.showPrivateContacts
        binding.settingsShowPrivateContactsHolder.setOnClickListener {
            binding.settingsShowPrivateContacts.toggle()
            config.showPrivateContacts = binding.settingsShowPrivateContacts.isChecked
        }
    }

    private fun setupOnContactClick() {
        binding.settingsOnContactClick.text = getOnContactClickText()
        binding.settingsOnContactClickHolder.setOnClickListener {
            val items = arrayListOf(
                RadioItem(ON_CLICK_CALL_CONTACT, getString(R.string.call_contact)),
                RadioItem(ON_CLICK_VIEW_CONTACT, getString(R.string.view_contact)),
                RadioItem(ON_CLICK_EDIT_CONTACT, getString(R.string.edit_contact))
            )

            RadioGroupDialog(this@SettingsActivity, items, config.onContactClick) {
                config.onContactClick = it as Int
                binding.settingsOnContactClick.text = getOnContactClickText()
            }
        }
    }

    private fun getOnContactClickText() = getString(
        when (config.onContactClick) {
            ON_CLICK_CALL_CONTACT -> R.string.call_contact
            ON_CLICK_VIEW_CONTACT -> R.string.view_contact
            else -> R.string.edit_contact
        }
    )

    private fun setupShowCallConfirmation() {
        binding.settingsShowCallConfirmation.isChecked = config.showCallConfirmation
        binding.settingsShowCallConfirmationHolder.setOnClickListener {
            binding.settingsShowCallConfirmation.toggle()
            config.showCallConfirmation = binding.settingsShowCallConfirmation.isChecked
        }
    }

    private fun setupMergeDuplicateContacts() {
        binding.settingsMergeDuplicateContacts.isChecked = config.mergeDuplicateContacts
        binding.settingsMergeDuplicateContactsHolder.setOnClickListener {
            binding.settingsMergeDuplicateContacts.toggle()
            config.mergeDuplicateContacts = binding.settingsMergeDuplicateContacts.isChecked
        }
    }

    private fun setupEnableAutomaticBackups() {
        binding.settingsBackupsLabel.beVisibleIf(isRPlus())
        binding.settingsEnableAutomaticBackupsHolder.beVisibleIf(isRPlus())
        binding.settingsEnableAutomaticBackups.isChecked = config.autoBackup
        binding.settingsEnableAutomaticBackupsHolder.setOnClickListener {
            val wasBackupDisabled = !config.autoBackup
            if (wasBackupDisabled) {
                ManageAutoBackupsDialog(
                    activity = this,
                    onSuccess = {
                        enableOrDisableAutomaticBackups(true)
                        scheduleNextAutomaticBackup()
                    }
                )
            } else {
                cancelScheduledAutomaticBackup()
                enableOrDisableAutomaticBackups(false)
            }
        }
    }

    private fun setupManageAutomaticBackups() {
        binding.settingsManageAutomaticBackupsHolder.beVisibleIf(isRPlus() && config.autoBackup)
        binding.settingsManageAutomaticBackupsHolder.setOnClickListener {
            ManageAutoBackupsDialog(
                activity = this,
                onSuccess = {
                    scheduleNextAutomaticBackup()
                }
            )
        }
    }

    private fun enableOrDisableAutomaticBackups(enable: Boolean) {
        config.autoBackup = enable
        binding.settingsEnableAutomaticBackups.isChecked = enable
        binding.settingsManageAutomaticBackupsHolder.beVisibleIf(enable)
    }
}
