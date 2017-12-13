package com.simplemobiletools.contacts.activities

import android.os.Bundle
import com.simplemobiletools.commons.extensions.beVisibleIf
import com.simplemobiletools.commons.extensions.updateTextColors
import com.simplemobiletools.commons.extensions.useEnglishToggled
import com.simplemobiletools.contacts.R
import com.simplemobiletools.contacts.extensions.config
import kotlinx.android.synthetic.main.activity_settings.*
import java.util.*

class SettingsActivity : SimpleActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
    }

    override fun onResume() {
        super.onResume()

        setupCustomizeColors()
        setupUseEnglish()
        setupShowInfoBubble()
        setupCallContactOnClick()
        setupStartNameWithSurname()
        updateTextColors(settings_holder)
    }

    private fun setupCustomizeColors() {
        settings_customize_colors_holder.setOnClickListener {
            startCustomizationActivity()
        }
    }

    private fun setupUseEnglish() {
        settings_use_english_holder.beVisibleIf(config.wasUseEnglishToggled || Locale.getDefault().language != "en")
        settings_use_english.isChecked = config.useEnglish
        settings_use_english_holder.setOnClickListener {
            settings_use_english.toggle()
            config.useEnglish = settings_use_english.isChecked
            useEnglishToggled()
        }
    }

    private fun setupShowInfoBubble() {
        settings_show_info_bubble.isChecked = config.showInfoBubble
        settings_show_info_bubble_holder.setOnClickListener {
            settings_show_info_bubble.toggle()
            config.showInfoBubble = settings_show_info_bubble.isChecked
        }
    }

    private fun setupCallContactOnClick() {
        settings_call_contact_on_click.isChecked = config.callContact
        settings_call_contact_on_click_holder.setOnClickListener {
            settings_call_contact_on_click.toggle()
            config.callContact = settings_call_contact_on_click.isChecked
        }
    }

    private fun setupStartNameWithSurname() {
        settings_start_with_surname.isChecked = config.startNameWithSurname
        settings_start_with_surname_holder.setOnClickListener {
            settings_start_with_surname.toggle()
            config.startNameWithSurname = settings_start_with_surname.isChecked
        }
    }
}
