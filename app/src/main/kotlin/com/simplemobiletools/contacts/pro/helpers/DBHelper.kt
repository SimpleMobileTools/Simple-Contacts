package com.simplemobiletools.contacts.pro.helpers

import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.graphics.BitmapFactory
import android.text.TextUtils
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.simplemobiletools.commons.extensions.getBlobValue
import com.simplemobiletools.commons.extensions.getIntValue
import com.simplemobiletools.commons.extensions.getLongValue
import com.simplemobiletools.commons.extensions.getStringValue
import com.simplemobiletools.contacts.pro.extensions.applyRegexFiltering
import com.simplemobiletools.contacts.pro.extensions.config
import com.simplemobiletools.contacts.pro.models.*

class DBHelper private constructor(val context: Context) : SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {
    private val CONTACTS_TABLE_NAME = "contacts"
    private val COL_ID = "id"
    private val COL_PREFIX = "prefix"
    private val COL_FIRST_NAME = "first_name"
    private val COL_MIDDLE_NAME = "middle_name"
    private val COL_SURNAME = "surname"
    private val COL_SUFFIX = "suffix"
    private val COL_NICKNAME = "nickname"
    private val COL_PHOTO = "photo"
    private val COL_PHONE_NUMBERS = "phone_numbers"
    private val COL_EMAILS = "emails"
    private val COL_EVENTS = "events"
    private val COL_STARRED = "starred"
    private val COL_ADDRESSES = "addresses"
    private val COL_IMS = "ims"
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
        const val DB_NAME = "contacts.db"
        private const val DB_VERSION = 7
        private var dbInstance: DBHelper? = null
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
                "$COL_WEBSITES TEXT, $COL_NICKNAME TEXT, $COL_IMS TEXT)")

        // start autoincrement ID from FIRST_CONTACT_ID to avoid conflicts
        db.execSQL("REPLACE INTO sqlite_sequence (name, seq) VALUES ('$CONTACTS_TABLE_NAME', $FIRST_CONTACT_ID)")

        createGroupsTable(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {}

    private fun createGroupsTable(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE $GROUPS_TABLE_NAME ($COL_ID INTEGER PRIMARY KEY AUTOINCREMENT, $COL_TITLE TEXT)")

        // start autoincrement ID from FIRST_GROUP_ID to avoid conflicts
        db.execSQL("REPLACE INTO sqlite_sequence (name, seq) VALUES ('$GROUPS_TABLE_NAME', $FIRST_GROUP_ID)")
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
        val filterDuplicates = activity.config.filterDuplicates
        val contacts = ArrayList<Contact>()
        val projection = arrayOf(COL_ID, COL_PREFIX, COL_FIRST_NAME, COL_MIDDLE_NAME, COL_SURNAME, COL_SUFFIX, COL_NICKNAME, COL_PHONE_NUMBERS,
                COL_EMAILS, COL_EVENTS, COL_STARRED, COL_PHOTO, COL_ADDRESSES, COL_IMS, COL_NOTES, COL_GROUPS, COL_COMPANY, COL_JOB_POSITION, COL_WEBSITES)

        val phoneNumbersToken = object : TypeToken<List<PhoneNumber>>() {}.type
        val emailsToken = object : TypeToken<List<Email>>() {}.type
        val addressesToken = object : TypeToken<List<Address>>() {}.type
        val IMsToken = object : TypeToken<List<IM>>() {}.type
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
                val nickname = cursor.getStringValue(COL_NICKNAME)

                val phoneNumbersJson = cursor.getStringValue(COL_PHONE_NUMBERS)
                val phoneNumbers = if (phoneNumbersJson == "[]") ArrayList() else gson.fromJson<ArrayList<PhoneNumber>>(phoneNumbersJson, phoneNumbersToken)
                        ?: ArrayList(1)

                // labels can be null at upgrading from older app versions, when the label wasn't available at all yet
                phoneNumbers.filter { it.label == null }.forEach {
                    it.label = ""
                }

                val emailsJson = cursor.getStringValue(COL_EMAILS)
                val emails = if (emailsJson == "[]") ArrayList() else gson.fromJson<ArrayList<Email>>(emailsJson, emailsToken)
                        ?: ArrayList(1)

                emails.filter { it.label == null }.forEach {
                    it.label = ""
                }

                val addressesJson = cursor.getStringValue(COL_ADDRESSES)
                val addresses = if (addressesJson == "[]") ArrayList() else gson.fromJson<ArrayList<Address>>(addressesJson, addressesToken)
                        ?: ArrayList(1)

                addresses.filter { it.label == null }.forEach {
                    it.label = ""
                }

                val IMsJson = cursor.getStringValue(COL_IMS)
                val IMs = if (IMsJson == "[]") ArrayList() else gson.fromJson<ArrayList<IM>>(IMsJson, IMsToken) ?: ArrayList(1)

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

                val cleanPhoneNumbers = ArrayList<PhoneNumber>()
                if (filterDuplicates) {
                    phoneNumbers.mapTo(cleanPhoneNumbers) { PhoneNumber(it.value.applyRegexFiltering(), 0, "") }
                }

                val contact = Contact(id, prefix, firstName, middleName, surname, suffix, nickname, "", phoneNumbers, emails, addresses,
                        events, SMT_PRIVATE, starred, id, "", photo, notes, groups, organization, websites, cleanPhoneNumbers, IMs)
                contacts.add(contact)
            }
        }
        return contacts
    }
}
