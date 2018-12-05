package com.simplemobiletools.contacts.pro.helpers

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.simplemobiletools.contacts.pro.models.*

class Converters {
    private val gson = Gson()
    private val longType = object : TypeToken<List<Long>>() {}.type
    private val stringType = object : TypeToken<List<String>>() {}.type
    private val numberType = object : TypeToken<List<PhoneNumber>>() {}.type
    private val emailType = object : TypeToken<List<Email>>() {}.type
    private val addressType = object : TypeToken<List<Address>>() {}.type
    private val eventType = object : TypeToken<List<Event>>() {}.type
    private val imType = object : TypeToken<List<IM>>() {}.type

    @TypeConverter
    fun jsonToStringList(value: String) = gson.fromJson<ArrayList<String>>(value, stringType)

    @TypeConverter
    fun stringListToJson(list: ArrayList<String>) = gson.toJson(list)

    @TypeConverter
    fun jsonToLongList(value: String) = gson.fromJson<ArrayList<Long>>(value, longType)

    @TypeConverter
    fun longListToJson(list: ArrayList<Long>) = gson.toJson(list)

    @TypeConverter
    fun jsonToPhoneNumberList(value: String) = gson.fromJson<ArrayList<PhoneNumber>>(value, numberType)

    @TypeConverter
    fun phoneNumberListToJson(list: ArrayList<PhoneNumber>) = gson.toJson(list)

    @TypeConverter
    fun jsonToEmailList(value: String) = gson.fromJson<ArrayList<Email>>(value, emailType)

    @TypeConverter
    fun emailListToJson(list: ArrayList<Email>) = gson.toJson(list)

    @TypeConverter
    fun jsonToAddressList(value: String) = gson.fromJson<ArrayList<Address>>(value, addressType)

    @TypeConverter
    fun addressListToJson(list: ArrayList<Address>) = gson.toJson(list)

    @TypeConverter
    fun jsonToEventList(value: String) = gson.fromJson<ArrayList<Event>>(value, eventType)

    @TypeConverter
    fun eventListToJson(list: ArrayList<Event>) = gson.toJson(list)

    @TypeConverter
    fun jsonToIMsList(value: String) = gson.fromJson<ArrayList<IM>>(value, imType)

    @TypeConverter
    fun IMsListToJson(list: ArrayList<IM>) = gson.toJson(list)
}
