package com.simplemobiletools.contacts.helpers

import android.app.Activity
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
import com.simplemobiletools.commons.extensions.getLongValue
import com.simplemobiletools.commons.extensions.getStringValue
import com.simplemobiletools.contacts.extensions.getByteArray
import com.simplemobiletools.contacts.extensions.getPhotoThumbnailSize
import com.simplemobiletools.contacts.models.*

class DBHelper private constructor(val context: Context) : SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {
    private val CONTACTS_TABLE_NAME = "contacts"
    private val COL_ID = "id"
    private val COL_PREFIX = "prefix"
    private val COL_FIRST_NAME = "first_name"
    private val COL_MIDDLE_NAME = "middle_name"
    private val COL_SURNAME = "surname"
    private val COL_SUFFIX = "suffix"
    private val COL_PHOTO = "photo"
    private val COL_PHONE_NUMBERS = "phone_numbers"
    private val COL_EMAILS = "emails"
    private val COL_EVENTS = "events"
    private val COL_STARRED = "starred"
    private val COL_ADDRESSES = "addresses"
    private val COL_NOTES = "notes"
    private val COL_COMPANY = "company"
    private val COL_JOB_POSITION = "job_position"
    private val COL_GROUPS = "groups"
    private val COL_WEBSITES = "websites"

    private val GROUPS_TABLE_NAME = "groups"
    private val COL_TITLE = "title"

    private val FIRST_CONTACT_ID = 1000000

    private val mDb = writableDatabase

    companion object {
        private const val DB_VERSION = 5
        const val DB_NAME = "contacts.db"
        var dbInstance: DBHelper? = null
        var gson = Gson()

        fun newInstance(context: Context): DBHelper {
            if (dbInstance == null)
                dbInstance = DBHelper(context)

            return dbInstance!!
        }
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE $CONTACTS_TABLE_NAME ($COL_ID INTEGER PRIMARY KEY AUTOINCREMENT, $COL_FIRST_NAME TEXT, $COL_MIDDLE_NAME TEXT, " +
                "$COL_SURNAME TEXT, $COL_PHOTO BLOB, $COL_PHONE_NUMBERS TEXT, $COL_EMAILS TEXT, $COL_EVENTS TEXT, $COL_STARRED INTEGER, " +
                "$COL_ADDRESSES TEXT, $COL_NOTES TEXT, $COL_GROUPS TEXT, $COL_PREFIX TEXT, $COL_SUFFIX TEXT, $COL_COMPANY TEXT, $COL_JOB_POSITION TEXT," +
                "$COL_WEBSITES TEXT)")

        // start autoincrement ID from FIRST_CONTACT_ID to avoid conflicts
        db.execSQL("REPLACE INTO sqlite_sequence (name, seq) VALUES ('$CONTACTS_TABLE_NAME', $FIRST_CONTACT_ID)")

        createGroupsTable(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion == 1) {
            db.execSQL("ALTER TABLE $CONTACTS_TABLE_NAME ADD COLUMN $COL_ADDRESSES TEXT DEFAULT ''")
            db.execSQL("ALTER TABLE $CONTACTS_TABLE_NAME ADD COLUMN $COL_NOTES TEXT DEFAULT ''")
        }

        if (oldVersion < 3) {
            createGroupsTable(db)
            db.execSQL("ALTER TABLE $CONTACTS_TABLE_NAME ADD COLUMN $COL_GROUPS TEXT DEFAULT ''")
        }

        if (oldVersion < 4) {
            db.execSQL("ALTER TABLE $CONTACTS_TABLE_NAME ADD COLUMN $COL_PREFIX TEXT DEFAULT ''")
            db.execSQL("ALTER TABLE $CONTACTS_TABLE_NAME ADD COLUMN $COL_SUFFIX TEXT DEFAULT ''")
            db.execSQL("ALTER TABLE $CONTACTS_TABLE_NAME ADD COLUMN $COL_COMPANY TEXT DEFAULT ''")
            db.execSQL("ALTER TABLE $CONTACTS_TABLE_NAME ADD COLUMN $COL_JOB_POSITION TEXT DEFAULT ''")
        }

        if (oldVersion < 5) {
            db.execSQL("ALTER TABLE $CONTACTS_TABLE_NAME ADD COLUMN $COL_WEBSITES TEXT DEFAULT ''")
        }
    }

    private fun createGroupsTable(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE $GROUPS_TABLE_NAME ($COL_ID INTEGER PRIMARY KEY AUTOINCREMENT, $COL_TITLE TEXT)")

        // start autoincrement ID from FIRST_GROUP_ID to avoid conflicts
        db.execSQL("REPLACE INTO sqlite_sequence (name, seq) VALUES ('$GROUPS_TABLE_NAME', $FIRST_GROUP_ID)")
    }

    fun insertContact(contact: Contact): Boolean {
        val contactValues = fillContactValues(contact)
        val id = mDb.insert(CONTACTS_TABLE_NAME, null, contactValues).toInt()
        return id != -1
    }

    fun updateContact(contact: Contact): Boolean {
        val contactValues = fillContactValues(contact)
        val selection = "$COL_ID = ?"
        val selectionArgs = arrayOf(contact.id.toString())
        return mDb.update(CONTACTS_TABLE_NAME, contactValues, selection, selectionArgs) == 1
    }

    fun deleteContact(id: Int) = deleteContacts(arrayOf(id.toString()))

    fun deleteContacts(ids: Array<String>) {
        if (ids.isEmpty()) {
            return
        }

        val args = TextUtils.join(", ", ids)
        val selection = "$CONTACTS_TABLE_NAME.$COL_ID IN ($args)"
        mDb.delete(CONTACTS_TABLE_NAME, selection, null)
    }

    private fun fillContactValues(contact: Contact): ContentValues {
        return ContentValues().apply {
            put(COL_PREFIX, contact.prefix)
            put(COL_FIRST_NAME, contact.firstName)
            put(COL_MIDDLE_NAME, contact.middleName)
            put(COL_SURNAME, contact.surname)
            put(COL_SUFFIX, contact.suffix)
            put(COL_PHONE_NUMBERS, gson.toJson(contact.phoneNumbers))
            put(COL_EMAILS, gson.toJson(contact.emails))
            put(COL_ADDRESSES, gson.toJson(contact.addresses))
            put(COL_EVENTS, gson.toJson(contact.events))
            put(COL_STARRED, contact.starred)
            put(COL_NOTES, contact.notes)
            put(COL_GROUPS, gson.toJson(contact.groups.map { it.id }))
            put(COL_COMPANY, contact.organization.company)
            put(COL_JOB_POSITION, contact.organization.jobPosition)
            put(COL_WEBSITES, gson.toJson(contact.websites))

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

    fun insertGroup(group: Group): Group? {
        val contactValues = fillGroupValues(group)
        val id = mDb.insert(GROUPS_TABLE_NAME, null, contactValues)
        return if (id == -1L) {
            null
        } else {
            Group(id, group.title)
        }
    }

    fun renameGroup(group: Group): Boolean {
        val contactValues = fillGroupValues(group)
        val selection = "$COL_ID = ?"
        val selectionArgs = arrayOf(group.id.toString())
        return mDb.update(GROUPS_TABLE_NAME, contactValues, selection, selectionArgs) == 1
    }

    fun deleteGroup(id: Long) = deleteGroups(arrayOf(id.toString()))

    private fun deleteGroups(ids: Array<String>) {
        val args = TextUtils.join(", ", ids)
        val selection = "$GROUPS_TABLE_NAME.$COL_ID IN ($args)"
        mDb.delete(GROUPS_TABLE_NAME, selection, null)
    }

    fun getGroups(): ArrayList<Group> {
        val groups = ArrayList<Group>()
        val projection = arrayOf(COL_ID, COL_TITLE)
        val cursor = mDb.query(GROUPS_TABLE_NAME, projection, null, null, null, null, null)
        cursor.use {
            while (cursor.moveToNext()) {
                val id = cursor.getLongValue(COL_ID)
                val title = cursor.getStringValue(COL_TITLE)
                val group = Group(id, title)
                groups.add(group)
            }
        }
        return groups
    }

    private fun fillGroupValues(group: Group): ContentValues {
        return ContentValues().apply {
            put(COL_TITLE, group.title)
        }
    }

    fun addContactsToGroup(contacts: ArrayList<Contact>, groupId: Long) {
        contacts.forEach {
            val currentGroupIds = it.groups.map { it.id } as ArrayList<Long>
            currentGroupIds.add(groupId)
            updateContactGroups(it, currentGroupIds)
        }
    }

    fun removeContactsFromGroup(contacts: ArrayList<Contact>, groupId: Long) {
        contacts.forEach {
            val currentGroupIds = it.groups.map { it.id } as ArrayList<Long>
            currentGroupIds.remove(groupId)
            updateContactGroups(it, currentGroupIds)
        }
    }

    private fun updateContactGroups(contact: Contact, groupIds: ArrayList<Long>) {
        val contactValues = fillContactGroupValues(groupIds)
        val selection = "$COL_ID = ?"
        val selectionArgs = arrayOf(contact.id.toString())
        mDb.update(CONTACTS_TABLE_NAME, contactValues, selection, selectionArgs)
    }

    private fun fillContactGroupValues(groupIds: ArrayList<Long>): ContentValues {
        return ContentValues().apply {
            put(COL_GROUPS, gson.toJson(groupIds))
        }
    }

    fun getContacts(activity: Activity, selection: String? = null, selectionArgs: Array<String>? = null): ArrayList<Contact> {
        val storedGroups = ContactsHelper(activity).getStoredGroups()
        val contacts = ArrayList<Contact>()
        val projection = arrayOf(COL_ID, COL_PREFIX, COL_FIRST_NAME, COL_MIDDLE_NAME, COL_SURNAME, COL_SUFFIX, COL_PHONE_NUMBERS, COL_EMAILS,
                COL_EVENTS, COL_STARRED, COL_PHOTO, COL_ADDRESSES, COL_NOTES, COL_GROUPS, COL_COMPANY, COL_JOB_POSITION, COL_WEBSITES)

        val phoneNumbersToken = object : TypeToken<List<PhoneNumber>>() {}.type
        val emailsToken = object : TypeToken<List<Email>>() {}.type
        val addressesToken = object : TypeToken<List<Address>>() {}.type
        val eventsToken = object : TypeToken<List<Event>>() {}.type
        val groupIdsToken = object : TypeToken<List<Long>>() {}.type
        val websitesToken = object : TypeToken<List<String>>() {}.type

        val cursor = mDb.query(CONTACTS_TABLE_NAME, projection, selection, selectionArgs, null, null, null)
        cursor.use {
            while (cursor.moveToNext()) {
                val id = cursor.getIntValue(COL_ID)
                val prefix = cursor.getStringValue(COL_PREFIX)
                val firstName = cursor.getStringValue(COL_FIRST_NAME)
                val middleName = cursor.getStringValue(COL_MIDDLE_NAME)
                val surname = cursor.getStringValue(COL_SURNAME)
                val suffix = cursor.getStringValue(COL_SUFFIX)

                val phoneNumbersJson = cursor.getStringValue(COL_PHONE_NUMBERS)
                val phoneNumbers = if (phoneNumbersJson == "[]") ArrayList() else gson.fromJson<ArrayList<PhoneNumber>>(phoneNumbersJson, phoneNumbersToken)
                        ?: ArrayList(1)

                val emailsJson = cursor.getStringValue(COL_EMAILS)
                val emails = if (emailsJson == "[]") ArrayList() else gson.fromJson<ArrayList<Email>>(emailsJson, emailsToken)
                        ?: ArrayList(1)

                val addressesJson = cursor.getStringValue(COL_ADDRESSES)
                val addresses = if (addressesJson == "[]") ArrayList() else gson.fromJson<ArrayList<Address>>(addressesJson, addressesToken)
                        ?: ArrayList(1)

                val eventsJson = cursor.getStringValue(COL_EVENTS)
                val events = if (eventsJson == "[]") ArrayList() else gson.fromJson<ArrayList<Event>>(eventsJson, eventsToken)
                        ?: ArrayList(1)

                val photoByteArray = cursor.getBlobValue(COL_PHOTO) ?: null
                val photo = if (photoByteArray?.isNotEmpty() == true) {
                    try {
                        BitmapFactory.decodeByteArray(photoByteArray, 0, photoByteArray.size)
                    } catch (e: OutOfMemoryError) {
                        null
                    }
                } else {
                    null
                }

                val notes = cursor.getStringValue(COL_NOTES)
                val starred = cursor.getIntValue(COL_STARRED)

                val groupIdsJson = cursor.getStringValue(COL_GROUPS)
                val groupIds = if (groupIdsJson == "[]") ArrayList() else gson.fromJson<ArrayList<Long>>(groupIdsJson, groupIdsToken)
                        ?: ArrayList(1)
                val groups = storedGroups.filter { groupIds.contains(it.id) } as ArrayList<Group>

                val company = cursor.getStringValue(COL_COMPANY)
                val jobPosition = cursor.getStringValue(COL_JOB_POSITION)
                val organization = Organization(company, jobPosition)

                val websitesJson = cursor.getStringValue(COL_WEBSITES)
                val websites = if (websitesJson == "[]") ArrayList() else gson.fromJson<ArrayList<String>>(websitesJson, websitesToken)
                        ?: ArrayList(1)

                val contact = Contact(id, prefix, firstName, middleName, surname, suffix, "", phoneNumbers, emails, addresses, events,
                        SMT_PRIVATE, starred, id, "", photo, notes, groups, organization, websites)
                contacts.add(contact)
            }
        }
        return contacts
    }

    fun getContactWithId(activity: Activity, id: Int): Contact? {
        val selection = "$COL_ID = ?"
        val selectionArgs = arrayOf(id.toString())
        return getContacts(activity, selection, selectionArgs).firstOrNull()
    }
}
