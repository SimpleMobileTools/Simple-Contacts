package com.simplemobiletools.contacts.pro.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "contacts", indices = [(Index(value = ["id"], unique = true))])
data class LocalContact(
        @PrimaryKey(autoGenerate = true) var id: Long?,
        @ColumnInfo(name = "prefix") var prefix: String,
        @ColumnInfo(name = "first_name") var firstName: String,
        @ColumnInfo(name = "middle_name") var middleName: String,
        @ColumnInfo(name = "surname") var surname: String,
        @ColumnInfo(name = "suffix") var suffix: String,
        @ColumnInfo(name = "nickname") var nickname: String,
        @ColumnInfo(name = "photo", typeAffinity = ColumnInfo.BLOB) var photo: ByteArray?,
        @ColumnInfo(name = "phone_numbers") var phoneNumbers: String,
        @ColumnInfo(name = "emails") var emails: String,
        @ColumnInfo(name = "events") var events: String,
        @ColumnInfo(name = "starred") var starred: Boolean,
        @ColumnInfo(name = "addresses") var addresses: String,
        @ColumnInfo(name = "notes") var notes: String,
        @ColumnInfo(name = "groups") var groups: String,
        @ColumnInfo(name = "company") var company: String,
        @ColumnInfo(name = "job_position") var jobPosition: String,
        @ColumnInfo(name = "websites") var websites: String,
        @ColumnInfo(name = "ims") var ims: String) {

    companion object {
        private const val serialVersionUID = -655314977575622L
    }
}
