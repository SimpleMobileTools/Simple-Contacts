package com.simplemobiletools.contacts.helpers

import java.io.ByteArrayOutputStream
import java.net.URLEncoder

// https://alvinalexander.com/java/jwarehouse/android/core/java/com/google/android/mms/pdu/QuotedPrintable.java.shtml
object QuotedPrintable {
    private const val ESCAPE_CHAR: Byte = '='.toByte()
    fun decode(value: String?): String {
        val bytes = value?.toByteArray()
        if (bytes == null || bytes.isEmpty()) {
            return ""
        }

        val buffer = ByteArrayOutputStream()
        var i = 0
        while (i < bytes.size) {
            val b = bytes[i].toInt()
            if (b == ESCAPE_CHAR.toInt()) {
                try {
                    if ('\r' == bytes[i + 1].toChar() && '\n' == bytes[i + 2].toChar()) {
                        i += 3
                        continue
                    }

                    val u = Character.digit(bytes[++i].toChar(), 16)
                    val l = Character.digit(bytes[++i].toChar(), 16)
                    if (u == -1 || l == -1) {
                        return ""
                    }

                    buffer.write(((u shl 4) + l).toChar().toInt())
                } catch (e: ArrayIndexOutOfBoundsException) {
                    return ""
                }

            } else {
                buffer.write(b)
            }
            i++
        }
        return String(buffer.toByteArray())
    }

    fun encode(value: String): String {
        val result = StringBuilder()
        value.forEach {
            if (it == ' ') {
                result.append(' ')
            } else {
                val urlEncoded = urlEncode(it.toString())
                if (urlEncoded == it.toString()) {
                    val hex = String.format("%04x", it.toInt()).trimStart('0').toUpperCase()
                    result.append("=$hex")
                } else {
                    result.append(urlEncoded)
                }
            }
        }
        return result.toString()
    }

    fun urlEncode(value: String) = URLEncoder.encode(value, "UTF-8").replace("+", " ").replace('%', '=')
}
