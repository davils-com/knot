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

@file:OptIn(DavilsInternal::class)

package com.davils.knot.environment

import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.arguments.conversion.Converter
import com.davils.kore.core.arguments.conversion.Converters
import com.davils.kore.core.collection.immutableList
import com.davils.kore.core.collection.immutableMap
import com.davils.kore.core.failure.contained
import com.davils.kore.core.identity.Uuid
import com.davils.kore.core.size.DataSize
import com.davils.kore.core.text.SafeText
import kotlin.time.Duration

private const val DEFAULT_SEPARATOR: Char = ','
private const val DEFAULT_ASSIGNMENT: Char = '='
private const val MISSING_REASON: String = "is required but not set"
private const val TOO_MANY_ELEMENTS_REASON: String =
    "holds more than ${EnvironmentLimits.MAX_ELEMENTS} elements"
private const val MALFORMED_ENTRY_REASON: String = "holds an entry without a key and a value"

/**
 * Tells whether the variable [name] is set.
 *
 * @param name the name of the variable.
 * @return `true` if [Environment.variable] returns a value for it.
 * @throws EnvironmentException if the environment expands references and the value cannot be expanded.
 * @since 1.0.0
 */
public fun Environment.isSet(name: String): Boolean = variable(name) != null

/**
 * Reads the variable [name] and converts it with [converter], one of Kore's
 * [Converters][com.davils.kore.core.arguments.conversion.Converters] or a converter of the caller.
 *
 * The converter receives the value as it is, untrusted and not trimmed. Whatever it throws is contained and reported
 * as [EnvironmentViolationCodes.UNREADABLE_VALUE] without the value and without the message of the failure, which may
 * repeat it; an `Error`, an interruption and a cancellation pass through.
 *
 * @param T the type of the value.
 * @param name the name of the variable.
 * @param converter converts the text of the value.
 * @return the converted value, or `null` if the variable is not set.
 * @throws EnvironmentException if the value cannot be converted or expanded.
 * @since 1.0.0
 */
public fun <T> Environment.value(name: String, converter: Converter<T>): T? {
    val raw = variable(name) ?: return null
    return converted(name = name, raw = raw, converter = converter)
}

/**
 * Reads the variable [name] like [value], or returns [default] if it is not set.
 *
 * @param T the type of the value.
 * @param name the name of the variable.
 * @param converter converts the text of the value.
 * @param default the value when the variable is not set.
 * @return the converted value or [default].
 * @throws EnvironmentException if the value is set but cannot be converted or expanded.
 * @since 1.0.0
 */
public fun <T> Environment.value(name: String, converter: Converter<T>, default: T): T =
    value(name, converter) ?: default

/**
 * Reads the variable [name] like [value] and refuses to go on without it.
 *
 * @param T the type of the value.
 * @param name the name of the variable.
 * @param converter converts the text of the value.
 * @return the converted value.
 * @throws EnvironmentException with [EnvironmentViolationCodes.MISSING_REQUIRED] if the variable is not set, or
 * another code if it cannot be converted or expanded.
 * @since 1.0.0
 */
public fun <T> Environment.requireValue(name: String, converter: Converter<T>): T = value(name, converter)
    ?: throw EnvironmentException(code = EnvironmentViolationCodes.MISSING_REQUIRED, variable = name, MISSING_REASON)

/**
 * Reads the variable [name] as an [Int], see
 * [Converters.int][com.davils.kore.core.arguments.conversion.Converters.int].
 *
 * @param name the name of the variable.
 * @return the number, or `null` if the variable is not set.
 * @throws EnvironmentException if the value is not a number in the range of an [Int].
 * @since 1.0.0
 */
public fun Environment.int(name: String): Int? = value(name, Converters.int)

/**
 * Reads the variable [name] as a [Long], see
 * [Converters.long][com.davils.kore.core.arguments.conversion.Converters.long].
 *
 * @param name the name of the variable.
 * @return the number, or `null` if the variable is not set.
 * @throws EnvironmentException if the value is not a number in the range of a [Long].
 * @since 1.0.0
 */
public fun Environment.long(name: String): Long? = value(name, Converters.long)

/**
 * Reads the variable [name] as a finite [Double], see
 * [Converters.double][com.davils.kore.core.arguments.conversion.Converters.double].
 *
 * @param name the name of the variable.
 * @return the number, or `null` if the variable is not set.
 * @throws EnvironmentException if the value is not a finite decimal number.
 * @since 1.0.0
 */
public fun Environment.double(name: String): Double? = value(name, Converters.double)

/**
 * Reads the variable [name] as a [Boolean], see
 * [Converters.boolean][com.davils.kore.core.arguments.conversion.Converters.boolean].
 *
 * @param name the name of the variable.
 * @return the flag, or `null` if the variable is not set.
 * @throws EnvironmentException if the value is not one of the spellings of a flag Kore accepts.
 * @since 1.0.0
 */
public fun Environment.boolean(name: String): Boolean? = value(name, Converters.boolean)

/**
 * Reads the variable [name] as a [Duration] between [min] and [max], see
 * [Converters.duration][com.davils.kore.core.arguments.conversion.Converters.duration].
 *
 * @param name the name of the variable.
 * @param min the shortest duration accepted; zero by default.
 * @param max the longest duration accepted; unbounded by default.
 * @return the duration, or `null` if the variable is not set.
 * @throws EnvironmentException if the value is not a duration between [min] and [max].
 * @throws IllegalArgumentException if [min] is negative or infinite or [max] is shorter than [min].
 * @since 1.0.0
 */
public fun Environment.duration(
    name: String,
    min: Duration = Duration.ZERO,
    max: Duration = Duration.INFINITE,
): Duration? = value(name, Converters.duration(min = min, max = max))

/**
 * Reads the variable [name] as a [DataSize] between [min] and [max], such as `512 KiB` or `64MiB`, see
 * [Converters.dataSize][com.davils.kore.core.arguments.conversion.Converters.dataSize].
 *
 * Only binary units in their exact case are read; `1 GB` is refused rather than guessed.
 *
 * @param name the name of the variable.
 * @param min the smallest size accepted; zero by default.
 * @param max the largest size accepted; unbounded by default.
 * @return the size, or `null` if the variable is not set.
 * @throws EnvironmentException if the value is not a data size between [min] and [max].
 * @throws IllegalArgumentException if [max] is smaller than [min].
 * @since 1.1.0
 */
public fun Environment.dataSize(
    name: String,
    min: DataSize = DataSize.ZERO,
    max: DataSize = DataSize(Long.MAX_VALUE),
): DataSize? = value(name, Converters.dataSize(min = min, max = max))

/**
 * Reads the variable [name] as a [Uuid] of version 4 or 7 in canonical form, see
 * [Converters.uuid][com.davils.kore.core.arguments.conversion.Converters.uuid].
 *
 * @param name the name of the variable.
 * @return the identifier, or `null` if the variable is not set.
 * @throws EnvironmentException if the value is not a canonical identifier of version 4 or 7.
 * @since 1.1.0
 */
public fun Environment.uuid(name: String): Uuid? = value(name, Converters.uuid)

/**
 * Reads the variable [name] as one of the constants of [E], compared by their names without regard to ASCII case.
 *
 * @param E the enum.
 * @param name the name of the variable.
 * @return the constant, or `null` if the variable is not set.
 * @throws EnvironmentException if the value names no constant of [E].
 * @since 1.0.0
 */
public inline fun <reified E : Enum<E>> Environment.enum(name: String): E? = value(name, Converters.enum<E>())

/**
 * Reads the variable [name] as a list of the texts between [separator]s, each trimmed, without the empty ones.
 *
 * @param name the name of the variable.
 * @param separator the character between the elements; `, ` by default.
 * @return the elements, or `null` if the variable is not set.
 * @throws EnvironmentException if the value holds more than [EnvironmentLimits.MAX_ELEMENTS] elements.
 * @since 1.0.0
 */
public fun Environment.list(name: String, separator: Char = DEFAULT_SEPARATOR): List<String>? {
    val raw = variable(name) ?: return null
    return elementsOf(name = name, raw = raw, separator = separator)
}

/**
 * Reads the variable [name] like [list] and converts every element with [converter], like [value].
 *
 * @param T the type of the elements.
 * @param name the name of the variable.
 * @param converter converts the text of each element.
 * @param separator the character between the elements; `, ` by default.
 * @return the converted elements, or `null` if the variable is not set.
 * @throws EnvironmentException if the value holds too many elements or one that cannot be converted.
 * @since 1.0.0
 */
public fun <T : Any> Environment.list(
    name: String,
    converter: Converter<T>,
    separator: Char = DEFAULT_SEPARATOR,
): List<T>? {
    val elements = list(name, separator) ?: return null
    return elements.map { element -> converted(name = name, raw = element, converter = converter) }.immutableList()
}

/**
 * Reads the variable [name] as a map of entries `key=value` between [separator]s, such as `a=1,b=2`.
 *
 * Keys and values are trimmed; a later entry wins over an earlier one with the same key.
 *
 * @param name the name of the variable.
 * @param separator the character between the entries; `, ` by default.
 * @param assignment the character between the key and the value of an entry; `=` by default.
 * @return the entries in the order of the value, or `null` if the variable is not set.
 * @throws EnvironmentException if the value holds more than [EnvironmentLimits.MAX_ELEMENTS] entries or an entry
 * without an [assignment] or a key.
 * @since 1.0.0
 */
public fun Environment.map(
    name: String,
    separator: Char = DEFAULT_SEPARATOR,
    assignment: Char = DEFAULT_ASSIGNMENT,
): Map<String, String>? {
    val elements = list(name, separator) ?: return null
    val entries = LinkedHashMap<String, String>()
    elements.forEach { element ->
        val key = element.substringBefore(assignment, missingDelimiterValue = "").trim()
        if (key.isEmpty()) throw unreadable(name, MALFORMED_ENTRY_REASON)
        entries[key] = element.substringAfter(assignment).trim()
    }
    return entries.immutableMap()
}

private fun elementsOf(name: String, raw: String, separator: Char): List<String> {
    val parts = raw.split(separator)
    if (parts.size > EnvironmentLimits.MAX_ELEMENTS) throw unreadable(name, TOO_MANY_ELEMENTS_REASON)
    val trimmed = parts.map(String::trim)
    return trimmed.filter(String::isNotEmpty).immutableList()
}

private fun <T> converted(name: String, raw: String, converter: Converter<T>): T {
    val result = contained { converter.convert(raw) }
    val reason = "cannot be read as ${SafeText.quoted(converter.typeName)}"
    return result.getOrElse { throw unreadable(name, reason) }
}

private fun unreadable(name: String, reason: String): EnvironmentException =
    EnvironmentException(code = EnvironmentViolationCodes.UNREADABLE_VALUE, variable = name, reason = reason)
