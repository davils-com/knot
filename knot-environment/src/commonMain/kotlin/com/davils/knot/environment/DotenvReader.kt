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
import com.davils.kore.core.collection.immutableList
import com.davils.kore.core.collection.immutableMap

private const val COMMENT: Char = '#'
private const val ASSIGNMENT: Char = '='
private const val DOUBLE_QUOTE: Char = '"'
private const val SINGLE_QUOTE: Char = '\''
private const val BACKSLASH: Char = '\\'
private const val NEWLINE: Char = '\n'
private const val EXPORT: String = "export "
private const val INLINE_COMMENT: String = " #"
private const val OPEN_QUOTE: Int = -1
private const val BAD_ESCAPE: Int = -2
private const val NO_ASSIGNMENT_REASON: String = "holds no assignment NAME=value"
private const val NO_NAME_REASON: String = "assigns no name"
private const val SPACED_NAME_REASON: String = "has whitespace inside the name"
private const val UNCLOSED_REASON: String = "opens a quote that is never closed"
private const val TRAILING_REASON: String = "has text after the closing quote"
private const val ESCAPE_REASON: String = "holds an unknown escape"

private val escapes: Map<Char, Char> = mapOf(
    'n' to NEWLINE,
    'r' to '\r',
    't' to '\t',
    DOUBLE_QUOTE to DOUBLE_QUOTE,
    BACKSLASH to BACKSLASH,
    '$' to '$',
)

private val scanProblems: Map<Int, String> = mapOf(
    OPEN_QUOTE to UNCLOSED_REASON,
    BAD_ESCAPE to ESCAPE_REASON,
)

internal class DotenvReader(private val lines: List<String>) {
    private val variables: MutableMap<String, String> = linkedMapOf()
    private val problems: MutableList<DotenvProblem> = mutableListOf()
    private var index: Int = 0

    fun read(): DotenvResult {
        while (index < lines.size) {
            val number = index + 1
            val line = lines[index].trim()
            index += 1
            val isSkipped = line.isEmpty() || line.first() == COMMENT
            if (!isSkipped) assignment(number, line.removePrefix(EXPORT).trimStart())
        }
        return DotenvResult(variables = variables.immutableMap(), problems = problems.immutableList())
    }

    private fun assignment(number: Int, line: String) {
        val separator = line.indexOf(ASSIGNMENT)
        val name = line.substring(startIndex = 0, endIndex = separator.coerceAtLeast(minimumValue = 0)).trim()
        val refusal = nameProblemOf(separator, name)
        if (refusal != null) return fail(number, refusal)
        val raw = line.substring(startIndex = separator + 1)
        valueOf(number, raw.trimStart())?.let { value -> variables[name] = value }
    }

    private fun nameProblemOf(separator: Int, name: String): String? {
        val problems = listOf(
            NO_ASSIGNMENT_REASON to (separator < 0),
            NO_NAME_REASON to name.isEmpty(),
            SPACED_NAME_REASON to name.any(Char::isWhitespace),
        )
        return problems.firstOrNull { (_, isProblem) -> isProblem }?.first
    }

    private fun valueOf(number: Int, raw: String): String? {
        val first = raw.firstOrNull()
        val isQuoted = first == DOUBLE_QUOTE || first == SINGLE_QUOTE
        if (isQuoted) return quoted(number = number, start = raw, isEscaping = first == DOUBLE_QUOTE)
        return raw.substringBefore(INLINE_COMMENT).trimEnd()
    }

    private fun quoted(number: Int, start: String, isEscaping: Boolean): String? {
        val quote = start.first()
        val value = StringBuilder()
        var rest = start.substring(startIndex = 1)
        var closed = scan(text = rest, quote = quote, isEscaping = isEscaping, value = value)
        while (closed == OPEN_QUOTE && index < lines.size) {
            value.append(NEWLINE)
            rest = lines[index]
            index += 1
            closed = scan(text = rest, quote = quote, isEscaping = isEscaping, value = value)
        }
        val problem = scanProblems[closed]
        if (problem != null) return failed(number, problem)
        return closing(number = number, trailing = rest.substring(startIndex = closed + 1), value = value)
    }

    private fun scan(text: String, quote: Char, isEscaping: Boolean, value: StringBuilder): Int {
        var position = 0
        var result = OPEN_QUOTE
        while (position < text.length && result == OPEN_QUOTE) {
            val character = text[position]
            val isEscape = isEscaping && character == BACKSLASH
            val escaped = text.getOrNull(position + 1)?.let(escapes::get)
            if (character == quote) result = position
            if (isEscape && escaped == null) result = BAD_ESCAPE
            if (result == OPEN_QUOTE) value.append(escaped.takeIf { isEscape } ?: character)
            position += 1
            if (isEscape) position += 1
        }
        return result
    }

    private fun closing(number: Int, trailing: String, value: StringBuilder): String? {
        val rest = trailing.trim()
        val isClean = rest.isEmpty() || rest.first() == COMMENT
        if (!isClean) return failed(number, TRAILING_REASON)
        return value.toString()
    }

    private fun failed(number: Int, reason: String): String? {
        fail(number, reason)
        return null
    }

    private fun fail(number: Int, reason: String) {
        problems += DotenvProblem(line = number, reason = reason)
    }
}
