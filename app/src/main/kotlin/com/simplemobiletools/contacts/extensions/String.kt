package com.simplemobiletools.contacts.extensions

import com.simplemobiletools.contacts.helpers.normalizeRegex
import java.text.Normalizer

fun String.normalizeString() = Normalizer.normalize(this, Normalizer.Form.NFD).replace(normalizeRegex, "")
