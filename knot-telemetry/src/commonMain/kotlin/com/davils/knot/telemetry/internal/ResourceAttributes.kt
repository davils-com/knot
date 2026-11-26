/*
 * Copyright 2026 Davils
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.davils.knot.telemetry.internal

private const val ENTRY_SEPARATOR: Char = ','
private const val ASSIGNMENT: Char = '='
private const val PERCENT: Char = '%'
private const val HEX: Int = 16
private const val ESCAPE_LENGTH: Int = 3
private const val MALFORMED: String =
    "OTEL_RESOURCE_ATTRIBUTES is not a list of key=value pairs with percent-encoded UTF-8 values"

internal fun resourceAttributesOf(text: String): List<Pair<String, String>> {
    val entries = text.split(ENTRY_SEPARATOR).map { entry -> entry.trim() }
    return entries.filter { entry -> entry.isNotEmpty() }.map(::attributeOf)
}

private fun attributeOf(entry: String): Pair<String, String> {
    val assignment = entry.indexOf(ASSIGNMENT)
    require(assignment > 0) { MALFORMED }
    val key = entry.substring(startIndex = 0, endIndex = assignment).trim()
    val value = entry.substring(assignment + 1).trim()
    return key to percentDecoded(value)
}

private fun percentDecoded(value: String): String {
    val decoded = ByteArray(utf8BytesOf(value).size)
    var size = 0
    var start = 0
    var escape = value.indexOf(PERCENT)
    while (escape >= 0) {
        val run = utf8BytesOf(value.substring(startIndex = start, endIndex = escape))
        run.copyInto(decoded, destinationOffset = size)
        size += run.size
        decoded[size] = escapedByte(value, escape)
        size += 1
        start = escape + ESCAPE_LENGTH
        escape = value.indexOf(PERCENT, startIndex = start)
    }
    val tail = utf8BytesOf(value.substring(start))
    tail.copyInto(decoded, destinationOffset = size)
    return utf8Of(decoded.copyOf(size + tail.size))
}

private fun escapedByte(value: String, escape: Int): Byte {
    val end = escape + ESCAPE_LENGTH
    require(end <= value.length) { MALFORMED }
    val digits = value.substring(startIndex = escape + 1, endIndex = end)
    require(digits.all(::isHexDigit)) { MALFORMED }
    return digits.toInt(HEX).toByte()
}

private fun isHexDigit(character: Char): Boolean =
    character in '0'..'9' || character in 'a'..'f' || character in 'A'..'F'

private fun utf8BytesOf(text: String): ByteArray {
    try {
        return text.encodeToByteArray(throwOnInvalidSequence = true)
    } catch (invalid: CharacterCodingException) {
        throw IllegalArgumentException(MALFORMED, invalid)
    }
}

private fun utf8Of(bytes: ByteArray): String {
    try {
        return bytes.decodeToString(throwOnInvalidSequence = true)
    } catch (invalid: CharacterCodingException) {
        throw IllegalArgumentException(MALFORMED, invalid)
    }
}
