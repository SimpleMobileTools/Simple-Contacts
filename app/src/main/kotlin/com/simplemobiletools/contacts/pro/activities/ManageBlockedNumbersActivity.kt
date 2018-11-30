package com.simplemobiletools.contacts.pro.activities

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import com.simplemobiletools.commons.extensions.beVisibleIf
import com.simplemobiletools.commons.extensions.getAdjustedPrimaryColor
import com.simplemobiletools.commons.extensions.underlineText
import com.simplemobiletools.commons.extensions.updateTextColors
import com.simplemobiletools.commons.interfaces.RefreshRecyclerViewListener
import com.simplemobiletools.contacts.pro.R
import com.simplemobiletools.contacts.pro.adapters.ManageBlockedNumbersAdapter
import com.simplemobiletools.contacts.pro.dialogs.AddBlockedNumberDialog
import com.simplemobiletools.contacts.pro.extensions.getBlockedNumbers
import com.simplemobiletools.contacts.pro.models.BlockedNumber
import kotlinx.android.synthetic.main.activity_manage_blocked_numbers.*

class ManageBlockedNumbersActivity : SimpleActivity(), RefreshRecyclerViewListener {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manage_blocked_numbers)
        updateBlockedNumbers()
        updateTextColors(manage_blocked_numbers_wrapper)

        manage_blocked_numbers_placeholder_2.apply {
            underlineText()
            setTextColor(getAdjustedPrimaryColor())
            setOnClickListener {
                addOrEditBlockedNumber()
            }
        }
    }

    private fun updateBlockedNumbers() {
        Thread {
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
        }.start()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_add_blocked_number, menu)
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

    private fun addOrEditBlockedNumber(currentNumber: BlockedNumber? = null) {
        AddBlockedNumberDialog(this, currentNumber) {
            updateBlockedNumbers()
        }
    }
}
