package com.simplemobiletools.contacts.pro.models

import com.simplemobiletools.contacts.pro.helpers.SMT_PRIVATE

data class ContactSource(var name: String?, var type: String?, var publicName: String) {
    fun getFullIdentifier(): String {
        return if (type == SMT_PRIVATE) {
            type
        } else if (name == null && type == null) {
            ":"
        } else if (name == null && type != null) {
            ":$type"
        } else if (name != null && type == null) {
            "$name:"
        } else {
            "$name:$type"
        }
    }
}
