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
import com.simplemobiletools.contacts.pro.helpers.*
import com.simplemobiletools.commons.models.contacts.ContactName
import com.simplemobiletools.commons.models.contacts.ContactNameFormat

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
        setupLanguage()
        setupShowContactThumbnails()
        setupShowPhoneNumbers()
        setupShowContactsWithNumbers()
        setupStartNameWithSurname()
        setupContactListShowStructuredName()
        setupContactListNameFormat()
        setupContactActivityShowStructuredName()
        setupContactActivityNameFormat()
        setupContactActivitySelectSharedFields()
        setupAutoFormatNameFormat()
        setupPermitCustomEventTypes()
        setupShowRemoveButtons()
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
        settings_use_english_holder.beVisibleIf((config.wasUseEnglishToggled || Locale.getDefault().language != "en") && !isTiramisuPlus())
        settings_use_english.isChecked = config.useEnglish
        settings_use_english_holder.setOnClickListener {
            settings_use_english.toggle()
            config.useEnglish = settings_use_english.isChecked
            System.exit(0)
        }
    }

    private fun setupLanguage() {
        settings_language.text = Locale.getDefault().displayLanguage
        settings_language_holder.beVisibleIf(isTiramisuPlus())

        if (settings_use_english_holder.isGone() && settings_language_holder.isGone()) {
            settings_font_size_holder.background = resources.getDrawable(R.drawable.ripple_top_corners, theme)
        }

        settings_language_holder.setOnClickListener {
            launchChangeAppLanguageIntent()
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
        /*
        settings_start_name_with_surname.isChecked = config.startNameWithSurname
        settings_start_name_with_surname_holder.setOnClickListener {
            settings_start_name_with_surname.toggle()
            config.startNameWithSurname = settings_start_name_with_surname.isChecked
        }
        */
        settings_start_name_with_surname.beGone()
        settings_start_name_with_surname_holder.beGone()
    }

    private fun setupContactListShowStructuredName() {
        settings_contactlist_show_structuredname.isChecked = !config.contactListShowFormattedName
        settings_contactlist_show_structuredname_holder.setOnClickListener {
            settings_contactlist_show_structuredname.toggle()
            config.contactListShowFormattedName = !settings_contactlist_show_structuredname.isChecked
        }
    }

    private fun setupContactListNameFormat() {
        settings_contactlist_nameformat.text = getNameFormatText(config.contactListNameFormat)
        settings_contactlist_nameformat_holder.setOnClickListener {
            val items = getNameFormatList()
            RadioGroupDialog(this@SettingsActivity, items, config.contactListNameFormat.ordinal) {
                val format = ContactNameFormat.values()[it as Int]
                config.contactListNameFormat = format
                config.startNameWithSurname = format.startsWithFamilyName()
                settings_contactlist_nameformat.text = getNameFormatText(format)
            }
        }
    }

    private fun setupContactActivityShowStructuredName() {
        settings_contactactivity_show_structuredname.isChecked = !config.contactNameShowFormattedName
        settings_contactactivity_show_structuredname_holder.setOnClickListener {
            settings_contactactivity_show_structuredname.toggle()
            config.contactNameShowFormattedName = !settings_contactactivity_show_structuredname.isChecked
        }
    }

    private fun setupContactActivityNameFormat() {
        settings_contactactivity_nameformat.text = getNameFormatText(config.contactNameFormat)
        settings_contactactivity_nameformat_holder.setOnClickListener {
            val items = getNameFormatList()
            RadioGroupDialog(this@SettingsActivity, items, config.contactNameFormat.ordinal) {
                val format = ContactNameFormat.values()[it as Int]
                config.contactNameFormat = format
                settings_contactactivity_nameformat.text = getNameFormatText(format)
            }
        }
    }

    private fun setupContactActivitySelectSharedFields() {
        settings_contactlist_export_selected_fields_only.isChecked = config.exportSelectedFieldsOnly
        settings_contactlist_export_selected_fields_only_holder.setOnClickListener {
            settings_contactlist_export_selected_fields_only.toggle()
            config.exportSelectedFieldsOnly = settings_contactlist_export_selected_fields_only.isChecked
        }
    }

    private fun setupAutoFormatNameFormat() {
        settings_autoformat_nameformat.text = getNameFormatText(config.autoFormattedNameFormat)
        settings_autoformat_nameformat_holder.setOnClickListener {
            val items = getNameFormatList()
            RadioGroupDialog(this@SettingsActivity, items, config.autoFormattedNameFormat.ordinal) {
                val format = ContactNameFormat.values()[it as Int]
                config.autoFormattedNameFormat = format
                settings_autoformat_nameformat.text = getNameFormatText(format)
            }
        }
    }

    private fun setupPermitCustomEventTypes() {
        settings_permit_custom_event_types.isChecked = config.permitCustomEventTypes
        settings_permit_custom_event_types_holder.setOnClickListener {
            settings_permit_custom_event_types.toggle()
            config.permitCustomEventTypes = settings_permit_custom_event_types.isChecked
        }
    }

    private fun setupShowRemoveButtons() {
        settings_show_remove_buttons.isChecked = config.showRemoveButtons
        settings_show_remove_buttons_holder.setOnClickListener {
            settings_show_remove_buttons.toggle()
            config.showRemoveButtons = settings_show_remove_buttons.isChecked
        }
    }

    private fun getNameFormatList(): ArrayList<RadioItem> {
        val items = arrayListOf(
            // RadioItem(ContactNameFormat.NAMEFORMAT_FORMATTED_NAME.ordinal, getString(R.string.nameformat_formatted_name)),
            RadioItem(ContactNameFormat.NAMEFORMAT_FAMILY_GIVEN.ordinal, getString(R.string.nameformat_family_given)),
            RadioItem(ContactNameFormat.NAMEFORMAT_FAMILY_GIVEN_M.ordinal, getString(R.string.nameformat_family_given_m)),
            RadioItem(ContactNameFormat.NAMEFORMAT_FAMILY_GIVEN_MIDDLE.ordinal, getString(R.string.nameformat_family_given_middle)),
            RadioItem(ContactNameFormat.NAMEFORMAT_FAMILY_MIDDLE_GIVEN.ordinal, getString(R.string.nameformat_family_middle_given)),
            RadioItem(ContactNameFormat.NAMEFORMAT_FAMILY_PREFIX_GIVEN_MIDDLE_SUFFIX.ordinal, getString(R.string.nameformat_family_prefix_given_middle_suffix)),
            RadioItem(ContactNameFormat.NAMEFORMAT_GIVEN_FAMILY.ordinal, getString(R.string.nameformat_given_family)),
            RadioItem(ContactNameFormat.NAMEFORMAT_GIVEN_M_FAMILY.ordinal, getString(R.string.nameformat_given_m_family)),
            RadioItem(ContactNameFormat.NAMEFORMAT_GIVEN_MIDDLE_FAMILY.ordinal, getString(R.string.nameformat_given_middle_family)),
            RadioItem(ContactNameFormat.NAMEFORMAT_PREFIX_GIVEN_MIDDLE_FAMILY_SUFFIX.ordinal, getString(R.string.nameformat_prefix_given_middle_family_suffix))
            )
        return(items)
    }

    private fun getNameFormatText(format : ContactNameFormat): String = getString(
        when (format) {
            // ContactNameFormat.NAMEFORMAT_DEFAULT -> R.string.nameformat_invalid
            ContactNameFormat.NAMEFORMAT_FORMATTED_NAME -> R.string.nameformat_formatted_name
            ContactNameFormat.NAMEFORMAT_FAMILY_GIVEN -> R.string.nameformat_family_given
            ContactNameFormat.NAMEFORMAT_FAMILY_GIVEN_M -> R.string.nameformat_family_given_m
            ContactNameFormat.NAMEFORMAT_FAMILY_GIVEN_MIDDLE -> R.string.nameformat_family_given_middle
            ContactNameFormat.NAMEFORMAT_FAMILY_MIDDLE_GIVEN -> R.string.nameformat_family_middle_given
            ContactNameFormat.NAMEFORMAT_FAMILY_PREFIX_GIVEN_MIDDLE_SUFFIX -> R.string.nameformat_family_prefix_given_middle_suffix
            ContactNameFormat.NAMEFORMAT_FAMILY_GIVEN_MIDDLE_PREFIX_SUFFIX -> R.string.nameformat_family_given_middle_prefix_suffix
            ContactNameFormat.NAMEFORMAT_GIVEN_FAMILY -> R.string.nameformat_given_family
            ContactNameFormat.NAMEFORMAT_GIVEN_M_FAMILY -> R.string.nameformat_given_m_family
            ContactNameFormat.NAMEFORMAT_GIVEN_MIDDLE_FAMILY -> R.string.nameformat_given_middle_family
            ContactNameFormat.NAMEFORMAT_GIVEN_FAMILY_MIDDLE -> R.string.nameformat_given_family_middle
            ContactNameFormat.NAMEFORMAT_PREFIX_GIVEN_MIDDLE_FAMILY_SUFFIX -> R.string.nameformat_prefix_given_middle_family_suffix
            ContactNameFormat.NAMEFORMAT_GIVEN_MIDDLE_FAMILY_PREFIX_SUFFIX -> R.string.nameformat_given_middle_family_prefix_suffix
            // else -> R.string.nameformat_invalid
        }
    )

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
