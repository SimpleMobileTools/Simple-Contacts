package com.simplemobiletools.contacts.helpers

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.MediaStore
import android.text.TextUtils
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.simplemobiletools.commons.extensions.getBlobValue
import com.simplemobiletools.commons.extensions.getIntValue
import com.simplemobiletools.commons.extensions.getStringValue
import com.simplemobiletools.contacts.extensions.getByteArray
import com.simplemobiletools.contacts.extensions.getPhotoThumbnailSize
import com.simplemobiletools.contacts.models.*

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
    private val COL_ADDRESSES = "addresses"
    private val COL_NOTES = "notes"

    private val FIRST_CONTACT_ID = 1000000

    private val mDb = writableDatabase

    companion object {
        private const val DB_VERSION = 2
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
                "$COL_SURNAME TEXT, $COL_PHOTO BLOB, $COL_PHONE_NUMBERS TEXT, $COL_EMAILS TEXT, $COL_EVENTS TEXT, $COL_STARRED INTEGER, " +
                "$COL_ADDRESSES TEXT, $COL_NOTES TEXT)")

        // start autoincrement ID from FIRST_CONTACT_ID to avoid conflicts
        db.execSQL("REPLACE INTO sqlite_sequence (name, seq) VALUES ('$CONTACTS_TABLE_NAME', $FIRST_CONTACT_ID)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion == 1) {
            db.execSQL("ALTER TABLE $CONTACTS_TABLE_NAME ADD COLUMN $COL_ADDRESSES TEXT DEFAULT ''")
            db.execSQL("ALTER TABLE $CONTACTS_TABLE_NAME ADD COLUMN $COL_NOTES TEXT DEFAULT ''")
        }
    }

    fun insert(contact: Contact): Boolean {
        val contactValues = fillContactValues(contact)
        val id = mDb.insert(CONTACTS_TABLE_NAME, null, contactValues).toInt()
        return id != -1
    }

    fun update(contact: Contact): Boolean {
        val contactValues = fillContactValues(contact)
        val selection = "$COL_ID = ?"
        val selectionArgs = arrayOf(contact.id.toString())
        return mDb.update(CONTACTS_TABLE_NAME, contactValues, selection, selectionArgs) == 1
    }

    fun deleteContact(id: Int) = deleteContacts(arrayOf(id.toString()))

    fun deleteContacts(ids: Array<String>) {
        val args = TextUtils.join(", ", ids)
        val selection = "$CONTACTS_TABLE_NAME.$COL_ID IN ($args)"
        mDb.delete(CONTACTS_TABLE_NAME, selection, null)
    }

    private fun fillContactValues(contact: Contact): ContentValues {
        return ContentValues().apply {
            put(COL_FIRST_NAME, contact.firstName)
            put(COL_MIDDLE_NAME, contact.middleName)
            put(COL_SURNAME, contact.surname)
            put(COL_PHONE_NUMBERS, Gson().toJson(contact.phoneNumbers))
            put(COL_EMAILS, Gson().toJson(contact.emails))
            put(COL_ADDRESSES, Gson().toJson(contact.addresses))
            put(COL_EVENTS, Gson().toJson(contact.events))
            put(COL_STARRED, contact.starred)
            put(COL_NOTES, contact.notes)

            if (contact.photoUri.isNotEmpty()) {
                put(COL_PHOTO, getPhotoByteArray(contact.photoUri))
            } else if (contact.photo == null) {
                putNull(COL_PHOTO)
            }
        }
    }

    private fun getPhotoByteArray(uri: String): ByteArray {
        val photoUri = Uri.parse(uri)
        val bitmap = MediaStore.Images.Media.getBitmap(context.contentResolver, photoUri)

        val thumbnailSize = context.getPhotoThumbnailSize()
        val scaledPhoto = Bitmap.createScaledBitmap(bitmap, thumbnailSize * 2, thumbnailSize * 2, false)
        val scaledSizePhotoData = scaledPhoto.getByteArray()
        scaledPhoto.recycle()
        return scaledSizePhotoData
    }

    fun toggleFavorites(ids: Array<String>, addToFavorites: Boolean) {
        val contactValues = ContentValues()
        contactValues.put(COL_STARRED, if (addToFavorites) 1 else 0)

        val args = TextUtils.join(", ", ids)
        val selection = "$COL_ID IN ($args)"
        mDb.update(CONTACTS_TABLE_NAME, contactValues, selection, null)
    }

    fun getContacts(selection: String? = null, selectionArgs: Array<String>? = null): ArrayList<Contact> {
        val contacts = ArrayList<Contact>()
        val projection = arrayOf(COL_ID, COL_FIRST_NAME, COL_MIDDLE_NAME, COL_SURNAME, COL_PHONE_NUMBERS, COL_EMAILS, COL_EVENTS, COL_STARRED,
                COL_PHOTO, COL_ADDRESSES, COL_NOTES)
        val cursor = mDb.query(CONTACTS_TABLE_NAME, projection, selection, selectionArgs, null, null, null)
        cursor.use {
            while (cursor.moveToNext()) {
                val id = cursor.getIntValue(COL_ID)
                val firstName = cursor.getStringValue(COL_FIRST_NAME)
                val middleName = cursor.getStringValue(COL_MIDDLE_NAME)
                val surname = cursor.getStringValue(COL_SURNAME)

                val phoneNumbersJson = cursor.getStringValue(COL_PHONE_NUMBERS)
                val phoneNumbersToken = object : TypeToken<List<PhoneNumber>>() {}.type
                val phoneNumbers = Gson().fromJson<ArrayList<PhoneNumber>>(phoneNumbersJson, phoneNumbersToken) ?: ArrayList(1)

                val emailsJson = cursor.getStringValue(COL_EMAILS)
                val emailsToken = object : TypeToken<List<Email>>() {}.type
                val emails = Gson().fromJson<ArrayList<Email>>(emailsJson, emailsToken) ?: ArrayList(1)

                val addressesJson = cursor.getStringValue(COL_ADDRESSES)
                val addressesToken = object : TypeToken<List<Address>>() {}.type
                val addresses = Gson().fromJson<ArrayList<Address>>(addressesJson, addressesToken) ?: ArrayList(1)

                val eventsJson = cursor.getStringValue(COL_EVENTS)
                val eventsToken = object : TypeToken<List<Event>>() {}.type
                val events = Gson().fromJson<ArrayList<Event>>(eventsJson, eventsToken) ?: ArrayList(1)

                val photoByteArray = cursor.getBlobValue(COL_PHOTO) ?: null
                val photo = if (photoByteArray?.isNotEmpty() == true) {
                    BitmapFactory.decodeByteArray(photoByteArray, 0, photoByteArray.size)
                } else {
                    null
                }

                val notes = cursor.getStringValue(COL_NOTES)
                val starred = cursor.getIntValue(COL_STARRED)
                val groups = ArrayList<Group>()

                val contact = Contact(id, firstName, middleName, surname, "", phoneNumbers, emails, addresses, events, SMT_PRIVATE, starred, id, "", photo, notes, groups)
                contacts.add(contact)
            }
        }
        return contacts
    }

    fun getContactWithId(id: Int): Contact? {
        val selection = "$COL_ID = ?"
        val selectionArgs = arrayOf(id.toString())
        return getContacts(selection, selectionArgs).firstOrNull()
    }
}
