package com.simplemobiletools.contacts.pro.activities

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import com.simplemobiletools.commons.extensions.beVisibleIf
import com.simplemobiletools.commons.extensions.getAdjustedPrimaryColor
import com.simplemobiletools.commons.extensions.underlineText
import com.simplemobiletools.commons.extensions.updateTextColors
import com.simplemobiletools.commons.helpers.ensureBackgroundThread
import com.simplemobiletools.commons.interfaces.RefreshRecyclerViewListener
import com.simplemobiletools.contacts.pro.R
import com.simplemobiletools.contacts.pro.adapters.ManageBlockedNumbersAdapter
import com.simplemobiletools.contacts.pro.dialogs.AddBlockedNumberDialog
import com.simplemobiletools.contacts.pro.extensions.getBlockedNumbers
import com.simplemobiletools.contacts.pro.extensions.isDefaultDialer
import com.simplemobiletools.contacts.pro.helpers.REQUEST_CODE_SET_DEFAULT_DIALER
import com.simplemobiletools.contacts.pro.models.BlockedNumber
import kotlinx.android.synthetic.main.activity_manage_blocked_numbers.*

class ManageBlockedNumbersActivity : SimpleActivity(), RefreshRecyclerViewListener {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manage_blocked_numbers)
        updateBlockedNumbers()
        updateTextColors(manage_blocked_numbers_wrapper)
        updatePlaceholderTexts()

        manage_blocked_numbers_placeholder_2.apply {
            underlineText()
            setTextColor(getAdjustedPrimaryColor())
            setOnClickListener {
                if (isDefaultDialer()) {
                    addOrEditBlockedNumber()
                } else {
                    launchSetDefaultDialerIntent()
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_add_blocked_number, menu)
        updateMenuItemColors(menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.add_blocked_number -> addOrEditBlockedNumber()
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    override fun refreshItems() {
        updateBlockedNumbers()
    }

    private fun updatePlaceholderTexts() {
        manage_blocked_numbers_placeholder.text = getString(if (isDefaultDialer()) R.string.not_blocking_anyone else R.string.must_make_default_dialer)
        manage_blocked_numbers_placeholder_2.text = getString(if (isDefaultDialer()) R.string.add_a_blocked_number else R.string.set_as_default)
    }

    private fun updateBlockedNumbers() {
        ensureBackgroundThread {
            val blockedNumbers = getBlockedNumbers()
            runOnUiThread {
                ManageBlockedNumbersAdapter(this, blockedNumbers, this, manage_blocked_numbers_list) {
                    addOrEditBlockedNumber(it as BlockedNumber)
                }.apply {
                    manage_blocked_numbers_list.adapter = this
                }

                manage_blocked_numbers_placeholder.beVisibleIf(blockedNumbers.isEmpty())
                manage_blocked_numbers_placeholder_2.beVisibleIf(blockedNumbers.isEmpty())
            }
        }
    }

    private fun addOrEditBlockedNumber(currentNumber: BlockedNumber? = null) {
        AddBlockedNumberDialog(this, currentNumber) {
            updateBlockedNumbers()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        super.onActivityResult(requestCode, resultCode, resultData)
        if (requestCode == REQUEST_CODE_SET_DEFAULT_DIALER && isDefaultDialer()) {
            updatePlaceholderTexts()
            updateBlockedNumbers()
        }
    }
}
