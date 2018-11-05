package com.simplemobiletools.contacts.pro.databases

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.simplemobiletools.contacts.pro.helpers.Converters
import com.simplemobiletools.contacts.pro.helpers.getEmptyLocalContact
import com.simplemobiletools.contacts.pro.interfaces.ContactsDao
import com.simplemobiletools.contacts.pro.models.LocalContact
import com.simplemobiletools.contacts.pro.objects.MyExecutor
import java.util.concurrent.Executors

@Database(entities = [(LocalContact::class)], version = 1)
@TypeConverters(Converters::class)
abstract class ContactsDatabase : RoomDatabase() {

    abstract fun ContactsDao(): ContactsDao

    companion object {
        private const val FIRST_CONTACT_ID = 1000000

        private var db: ContactsDatabase? = null

        fun getInstance(context: Context): ContactsDatabase {
            if (db == null) {
                synchronized(ContactsDatabase::class) {
                    if (db == null) {
                        db = Room.databaseBuilder(context.applicationContext, ContactsDatabase::class.java, "local_contacts.db")
                                .setQueryExecutor(MyExecutor.myExecutor)
                                .addCallback(object : Callback() {
                                    override fun onCreate(db: SupportSQLiteDatabase) {
                                        super.onCreate(db)
                                        increaseAutoIncrementId()
                                    }
                                })
                                .build()
                        db!!.openHelper.setWriteAheadLoggingEnabled(true)
                    }
                }
            }
            return db!!
        }

        fun destroyInstance() {
            db = null
        }

        // start autoincrement ID from FIRST_CONTACT_ID to avoid conflicts
        // Room doesn't seem to have a built in way for it, so just create a contact and delete it
        private fun increaseAutoIncrementId() {
            Executors.newSingleThreadExecutor().execute {
                val emptyContact = getEmptyLocalContact()
                emptyContact.id = FIRST_CONTACT_ID
                db!!.ContactsDao().apply {
                    insertOrUpdate(emptyContact)
                    deleteContactIds(FIRST_CONTACT_ID.toString())
                }
            }
        }
    }
}
