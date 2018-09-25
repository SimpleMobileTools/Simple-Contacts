package com.simplemobiletools.contacts.activities

import android.annotation.TargetApi
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.view.KeyEvent
import com.simplemobiletools.commons.extensions.applyColorFilter
import com.simplemobiletools.commons.extensions.updateTextColors
import com.simplemobiletools.commons.helpers.isLollipopPlus
import com.simplemobiletools.contacts.R
import com.simplemobiletools.contacts.adapters.ContactsAdapter
import com.simplemobiletools.contacts.extensions.afterTextChanged
import com.simplemobiletools.contacts.extensions.callContact
import com.simplemobiletools.contacts.extensions.config
import com.simplemobiletools.contacts.helpers.ContactsHelper
import com.simplemobiletools.contacts.helpers.LOCATION_DIALPAD
import com.simplemobiletools.contacts.models.Contact
import kotlinx.android.synthetic.main.activity_dialpad.*

class DialpadActivity : SimpleActivity() {
    var contacts = ArrayList<Contact>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dialpad)

        dialpad_0.setOnClickListener { dialpadPressed("0") }
        dialpad_1.setOnClickListener { dialpadPressed("1") }
        dialpad_2.setOnClickListener { dialpadPressed("2") }
        dialpad_3.setOnClickListener { dialpadPressed("3") }
        dialpad_4.setOnClickListener { dialpadPressed("4") }
        dialpad_5.setOnClickListener { dialpadPressed("5") }
        dialpad_6.setOnClickListener { dialpadPressed("6") }
        dialpad_7.setOnClickListener { dialpadPressed("7") }
        dialpad_8.setOnClickListener { dialpadPressed("8") }
        dialpad_9.setOnClickListener { dialpadPressed("9") }
        dialpad_asterisk.setOnClickListener { dialpadPressed("*") }
        dialpad_hashtag.setOnClickListener { dialpadPressed("#") }
        dialpad_clear_char.setOnClickListener { clearChar() }
        dialpad_clear_char.setOnLongClickListener { clearInput(); true }
        dialpad_input.afterTextChanged { dialpadValueChanged(it) }
        ContactsHelper(this).getContacts { gotContacts(it) }
        disableKeyboardPopping()
    }

    override fun onResume() {
        super.onResume()
        updateTextColors(dialpad_holder)
        dialpad_clear_char.applyColorFilter(config.textColor)
    }

    private fun dialpadPressed(char: String) {
        dialpad_input.dispatchKeyEvent(getKeyEvent(getCharKeyCode(char)))
    }

    private fun clearChar() {
        dialpad_input.dispatchKeyEvent(getKeyEvent(KeyEvent.KEYCODE_DEL))
    }

    private fun clearInput() {
        dialpad_input.setText("")
    }

    private fun getKeyEvent(keyCode: Int) = KeyEvent(0, 0, KeyEvent.ACTION_DOWN, keyCode, 0)

    private fun getCharKeyCode(char: String) = when (char) {
        "0" -> KeyEvent.KEYCODE_0
        "1" -> KeyEvent.KEYCODE_1
        "2" -> KeyEvent.KEYCODE_2
        "3" -> KeyEvent.KEYCODE_3
        "4" -> KeyEvent.KEYCODE_4
        "5" -> KeyEvent.KEYCODE_5
        "6" -> KeyEvent.KEYCODE_6
        "7" -> KeyEvent.KEYCODE_7
        "8" -> KeyEvent.KEYCODE_8
        "9" -> KeyEvent.KEYCODE_9
        "*" -> KeyEvent.KEYCODE_STAR
        else -> KeyEvent.KEYCODE_POUND
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private fun disableKeyboardPopping() {
        if (isLollipopPlus()) {
            dialpad_input.showSoftInputOnFocus = false
        } else {
            dialpad_input.inputType = InputType.TYPE_NULL
        }
    }

    private fun gotContacts(newContacts: ArrayList<Contact>) {
        contacts = newContacts
        Contact.sorting = config.sorting
        Contact.startWithSurname = config.startNameWithSurname
        contacts.sort()
    }

    private fun dialpadValueChanged(text: String) {
        (dialpad_list.adapter as? ContactsAdapter)?.finishActMode()
        val filtered = contacts.filter { it.doesContainPhoneNumber(text) } as ArrayList<Contact>

        ContactsAdapter(this, filtered, null, LOCATION_DIALPAD, null, dialpad_list, dialpad_fastscroller, text) {
            callContact(it as Contact)
        }.apply {
            addVerticalDividers(true)
            dialpad_list.adapter = this
        }

        dialpad_fastscroller.setScrollToY(0)
        dialpad_fastscroller.setViews(dialpad_list) {
            val item = (dialpad_list.adapter as ContactsAdapter).contactItems.getOrNull(it)
            dialpad_fastscroller.updateBubbleText(item?.getBubbleText() ?: "")
        }
    }
}
