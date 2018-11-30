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
import com.simplemobiletools.contacts.pro.extensions.getBlockedNumbers
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
                addBlockedNumber()
            }
        }
    }

    private fun updateBlockedNumbers() {
        Thread {
            val blockedNumbers = getBlockedNumbers()
            runOnUiThread {
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
            R.id.add_blocked_number -> addBlockedNumber()
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    override fun refreshItems() {
        updateBlockedNumbers()
    }

    private fun addBlockedNumber() {

    }
}
