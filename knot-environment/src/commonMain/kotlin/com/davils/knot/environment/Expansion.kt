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

package com.davils.knot.environment

import com.davils.kore.core.validation.violation.ViolationCode

private const val DOLLAR: Char = '$'
private const val OPEN: Char = '{'
private const val CLOSE: Char = '}'
private const val DEFAULT_MARK: String = ":-"
private const val ESCAPE_WIDTH: Int = 2
private const val UNRESOLVED_REASON: String = "refers to a variable that is not set"
private const val CYCLE_REASON: String = "refers back to itself through its references"
private const val DEEP_REASON: String =
    "follows more than ${EnvironmentLimits.MAX_EXPANSION_DEPTH} references or expands to more than " +
        "${EnvironmentLimits.MAX_EXPANDED_LENGTH} characters"
private const val MALFORMED_REASON: String = "holds a reference that is not written as \${NAME} or \${NAME:-default}"

internal class Expansion(
    private val variable: String,
    private val onMissing: MissingReference,
    private val lookup: (String) -> String?,
) {
    private val expanding: MutableSet<String> = mutableSetOf()
    private val expanded: MutableMap<String, String?> = mutableMapOf()

    fun of(value: String): String = expand(value, depth = 0)

    private fun expand(value: String, depth: Int): String {
        if (DOLLAR !in value) return value
        val result = StringBuilder()
        var position = 0
        while (position < value.length) {
            position = step(value = value, position = position, depth = depth, result = result)
            if (result.length > EnvironmentLimits.MAX_EXPANDED_LENGTH) throw failure(EXPANSION_TOO_LARGE, DEEP_REASON)
        }
        return result.toString()
    }

    private fun step(value: String, position: Int, depth: Int, result: StringBuilder): Int {
        val character = value[position]
        val next = value.getOrNull(position + 1)
        val isReference = character == DOLLAR && next == OPEN
        if (isReference) return referenceStep(value = value, position = position, depth = depth, result = result)
        result.append(character)
        return position + widthOf(isEscape = character == DOLLAR && next == DOLLAR)
    }

    private fun referenceStep(value: String, position: Int, depth: Int, result: StringBuilder): Int {
        val end = value.indexOf(CLOSE, startIndex = position + 2)
        if (end < 0) throw failure(INVALID_REFERENCE, MALFORMED_REASON)
        val body = value.substring(startIndex = position + 2, endIndex = end)
        val written = value.substring(startIndex = position, endIndex = end + 1)
        result.append(reference(body = body, depth = depth, written = written))
        return end + 1
    }

    private fun widthOf(isEscape: Boolean): Int {
        if (isEscape) return ESCAPE_WIDTH
        return 1
    }

    private fun reference(body: String, depth: Int, written: String): String {
        val name = body.substringBefore(DEFAULT_MARK)
        val fallback = body.substringAfter(DEFAULT_MARK, missingDelimiterValue = "").takeIf { DEFAULT_MARK in body }
        if (nameRefusal(name) != null) throw failure(INVALID_REFERENCE, MALFORMED_REASON)
        val resolved = resolved(name, depth)
        val replacement = resolved ?: fallback?.let { text -> expand(text, depth + 1) }
        return replacement ?: missing(written)
    }

    private fun resolved(name: String, depth: Int): String? {
        if (name in expanded) return expanded[name]
        if (depth >= EnvironmentLimits.MAX_EXPANSION_DEPTH) throw failure(EXPANSION_TOO_LARGE, DEEP_REASON)
        if (!expanding.add(name)) throw failure(EXPANSION_CYCLE, CYCLE_REASON)
        val value = lookup(name)?.let { raw -> expand(raw, depth + 1) }
        expanding -= name
        expanded[name] = value
        return value
    }

    private fun missing(written: String): String = when (onMissing) {
        MissingReference.Fail -> throw failure(UNRESOLVED_REFERENCE, UNRESOLVED_REASON)
        MissingReference.Empty -> ""
        MissingReference.Keep -> written
    }

    private fun failure(code: ViolationCode, reason: String): EnvironmentException =
        EnvironmentException(code = code, variable = variable, reason = reason)

    private companion object {
        val EXPANSION_TOO_LARGE: ViolationCode = EnvironmentViolationCodes.EXPANSION_TOO_LARGE
        val INVALID_REFERENCE: ViolationCode = EnvironmentViolationCodes.INVALID_REFERENCE
        val EXPANSION_CYCLE: ViolationCode = EnvironmentViolationCodes.EXPANSION_CYCLE
        val UNRESOLVED_REFERENCE: ViolationCode = EnvironmentViolationCodes.UNRESOLVED_REFERENCE
    }
}
