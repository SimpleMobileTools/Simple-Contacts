package com.simplemobiletools.contacts.helpers

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.google.gson.Gson
import com.simplemobiletools.contacts.models.Contact


class DBHelper private constructor(val context: Context) : SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {
    private val CONTACTS_TABLE_NAME = "contacts"
    private val COL_ID = "id"
    private val COL_FIRST_NAME = "first_name"
    private val COL_MIDDLE_NAME = "middle_name"
    private val COL_SURNAME = "surname"
    private val COL_PHOTO = "photo"
    private val COL_PHONE_NUMBERS = "phone_numbers"
    private val COL_EMAILS = "emails"
    private val COL_EVENTS = "events"
    private val COL_STARRED = "starred"

    private val mDb: SQLiteDatabase = writableDatabase

    companion object {
        private const val DB_VERSION = 1
        const val DB_NAME = "contacts.db"
        var dbInstance: DBHelper? = null

        fun newInstance(context: Context): DBHelper {
            if (dbInstance == null)
                dbInstance = DBHelper(context)

            return dbInstance!!
        }
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE $CONTACTS_TABLE_NAME ($COL_ID INTEGER PRIMARY KEY AUTOINCREMENT, $COL_FIRST_NAME TEXT, $COL_MIDDLE_NAME TEXT, " +
                "$COL_SURNAME TEXT, $COL_PHOTO BLOB, $COL_PHONE_NUMBERS TEXT, $COL_EMAILS TEXT, $COL_EVENTS TEXT, $COL_STARRED INTEGER)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {

    }

    fun insert(contact: Contact): Boolean {
        val contactValues = fillContactValues(contact)
        val id = mDb.insert(CONTACTS_TABLE_NAME, null, contactValues).toInt()
        return id != -1
    }

    private fun fillContactValues(contact: Contact): ContentValues {
        return ContentValues().apply {
            put(COL_FIRST_NAME, contact.firstName)
            put(COL_MIDDLE_NAME, contact.middleName)
            put(COL_SURNAME, contact.surname)
            put(COL_PHONE_NUMBERS, Gson().toJson(contact.phoneNumbers))
            put(COL_EMAILS, Gson().toJson(contact.emails))
            put(COL_EVENTS, Gson().toJson(contact.events))
            put(COL_STARRED, contact.starred)
        }
    }
}
