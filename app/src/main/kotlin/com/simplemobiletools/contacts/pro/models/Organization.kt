package com.simplemobiletools.contacts.pro.models

data class Organization(var company: String, var jobPosition: String) {
    fun isEmpty() = company.isEmpty() && jobPosition.isEmpty()

    fun isNotEmpty() = !isEmpty()
}
