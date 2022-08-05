package com.simplemobiletools.contacts.pro.activities

import android.os.Bundle
import com.simplemobiletools.commons.dialogs.RadioGroupDialog
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.helpers.*
import com.simplemobiletools.commons.models.RadioItem
import com.simplemobiletools.contacts.pro.R
import com.simplemobiletools.contacts.pro.dialogs.ManageVisibleFieldsDialog
import com.simplemobiletools.contacts.pro.dialogs.ManageVisibleTabsDialog
import com.simplemobiletools.contacts.pro.extensions.config
import com.simplemobiletools.contacts.pro.helpers.ON_CLICK_CALL_CONTACT
import com.simplemobiletools.contacts.pro.helpers.ON_CLICK_EDIT_CONTACT
import com.simplemobiletools.contacts.pro.helpers.ON_CLICK_VIEW_CONTACT
import kotlinx.android.synthetic.main.activity_settings.*
import java.util.*

class SettingsActivity : SimpleActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
    }

    override fun onResume() {
        super.onResume()
        setupToolbar(settings_toolbar, NavigationIcon.Arrow)

        setupCustomizeColors()
        setupManageShownContactFields()
        setupManageShownTabs()
        setupFontSize()
        setupUseEnglish()
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
        updateTextColors(settings_holder)

        arrayOf(settings_color_customization_label, settings_general_settings_label, settings_main_screen_label, settings_list_view_label).forEach {
            it.setTextColor(getProperPrimaryColor())
        }

        arrayOf(
            settings_color_customization_holder,
            settings_general_settings_holder,
            settings_main_screen_holder,
            settings_list_view_holder
        ).forEach {
            it.background.applyColorFilter(getProperBackgroundColor().getContrastColor())
        }
    }

    private fun setupCustomizeColors() {
        settings_customize_colors_holder.setOnClickListener {
            startCustomizationActivity()
        }
    }

    private fun setupManageShownContactFields() {
        settings_manage_contact_fields_holder.setOnClickListener {
            ManageVisibleFieldsDialog(this) {}
        }
    }

    private fun setupManageShownTabs() {
        settings_manage_shown_tabs_holder.setOnClickListener {
            ManageVisibleTabsDialog(this)
        }
    }

    private fun setupDefaultTab() {
        settings_default_tab.text = getDefaultTabText()
        settings_default_tab_holder.setOnClickListener {
            val items = arrayListOf(
                RadioItem(TAB_CONTACTS, getString(R.string.contacts_tab)),
                RadioItem(TAB_FAVORITES, getString(R.string.favorites_tab)),
                RadioItem(TAB_GROUPS, getString(R.string.groups_tab)),
                RadioItem(TAB_LAST_USED, getString(R.string.last_used_tab))
            )

            RadioGroupDialog(this@SettingsActivity, items, config.defaultTab) {
                config.defaultTab = it as Int
                settings_default_tab.text = getDefaultTabText()
            }
        }
    }

    private fun getDefaultTabText() = getString(
        when (baseConfig.defaultTab) {
            TAB_CONTACTS -> R.string.contacts_tab
            TAB_FAVORITES -> R.string.favorites_tab
            TAB_GROUPS -> R.string.groups_tab
            else -> R.string.last_used_tab
        }
    )

    private fun setupFontSize() {
        settings_font_size.text = getFontSizeText()
        settings_font_size_holder.setOnClickListener {
            val items = arrayListOf(
                RadioItem(FONT_SIZE_SMALL, getString(R.string.small)),
                RadioItem(FONT_SIZE_MEDIUM, getString(R.string.medium)),
                RadioItem(FONT_SIZE_LARGE, getString(R.string.large)),
                RadioItem(FONT_SIZE_EXTRA_LARGE, getString(R.string.extra_large))
            )

            RadioGroupDialog(this@SettingsActivity, items, config.fontSize) {
                config.fontSize = it as Int
                settings_font_size.text = getFontSizeText()
            }
        }
    }

    private fun setupUseEnglish() {
        settings_use_english_holder.beVisibleIf(config.wasUseEnglishToggled || Locale.getDefault().language != "en")
        settings_use_english.isChecked = config.useEnglish

        if (settings_use_english_holder.isGone()) {
            settings_font_size_holder.background = resources.getDrawable(R.drawable.ripple_top_corners, theme)
        }

        settings_use_english_holder.setOnClickListener {
            settings_use_english.toggle()
            config.useEnglish = settings_use_english.isChecked
            System.exit(0)
        }
    }

    private fun setupShowContactThumbnails() {
        settings_show_contact_thumbnails.isChecked = config.showContactThumbnails
        settings_show_contact_thumbnails_holder.setOnClickListener {
            settings_show_contact_thumbnails.toggle()
            config.showContactThumbnails = settings_show_contact_thumbnails.isChecked
        }
    }

    private fun setupShowPhoneNumbers() {
        settings_show_phone_numbers.isChecked = config.showPhoneNumbers
        settings_show_phone_numbers_holder.setOnClickListener {
            settings_show_phone_numbers.toggle()
            config.showPhoneNumbers = settings_show_phone_numbers.isChecked
        }
    }

    private fun setupShowContactsWithNumbers() {
        settings_show_only_contacts_with_numbers.isChecked = config.showOnlyContactsWithNumbers
        settings_show_only_contacts_with_numbers_holder.setOnClickListener {
            settings_show_only_contacts_with_numbers.toggle()
            config.showOnlyContactsWithNumbers = settings_show_only_contacts_with_numbers.isChecked
        }
    }

    private fun setupStartNameWithSurname() {
        settings_start_name_with_surname.isChecked = config.startNameWithSurname
        settings_start_name_with_surname_holder.setOnClickListener {
            settings_start_name_with_surname.toggle()
            config.startNameWithSurname = settings_start_name_with_surname.isChecked
        }
    }

    private fun setupShowDialpadButton() {
        settings_show_dialpad_button.isChecked = config.showDialpadButton
        settings_show_dialpad_button_holder.setOnClickListener {
            settings_show_dialpad_button.toggle()
            config.showDialpadButton = settings_show_dialpad_button.isChecked
        }
    }

    private fun setupShowPrivateContacts() {
        settings_show_private_contacts.isChecked = config.showPrivateContacts
        settings_show_private_contacts_holder.setOnClickListener {
            settings_show_private_contacts.toggle()
            config.showPrivateContacts = settings_show_private_contacts.isChecked
        }
    }

    private fun setupOnContactClick() {
        settings_on_contact_click.text = getOnContactClickText()
        settings_on_contact_click_holder.setOnClickListener {
            val items = arrayListOf(
                RadioItem(ON_CLICK_CALL_CONTACT, getString(R.string.call_contact)),
                RadioItem(ON_CLICK_VIEW_CONTACT, getString(R.string.view_contact)),
                RadioItem(ON_CLICK_EDIT_CONTACT, getString(R.string.edit_contact))
            )

            RadioGroupDialog(this@SettingsActivity, items, config.onContactClick) {
                config.onContactClick = it as Int
                settings_on_contact_click.text = getOnContactClickText()
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
        settings_show_call_confirmation.isChecked = config.showCallConfirmation
        settings_show_call_confirmation_holder.setOnClickListener {
            settings_show_call_confirmation.toggle()
            config.showCallConfirmation = settings_show_call_confirmation.isChecked
        }
    }

    private fun setupMergeDuplicateContacts() {
        settings_merge_duplicate_contacts.isChecked = config.mergeDuplicateContacts
        settings_merge_duplicate_contacts_holder.setOnClickListener {
            settings_merge_duplicate_contacts.toggle()
            config.mergeDuplicateContacts = settings_merge_duplicate_contacts.isChecked
        }
    }
}
