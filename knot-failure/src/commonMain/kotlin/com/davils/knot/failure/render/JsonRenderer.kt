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

package com.davils.knot.failure.render

import com.davils.knot.failure.UnexpectedFailure
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.text.SafeText

private const val JSON_TYPE: String = "application/json"
private const val JSON_EXTENSION: String = "json"
private const val HEX_DIGITS: Int = 4
private const val RADIX: Int = 16
private const val NULL: String = "null"
private const val SUPPLEMENTARY: Int = 0x10000

private val escapes: Map<Char, String> = mapOf(
    '"' to "\\\"",
    '\\' to "\\\\",
    '\n' to "\\n",
    '\r' to "\\r",
    '\t' to "\\t",
)

internal class JsonRenderer(private val options: RenderOptions) : CrashReportRenderer {
    override val contentType: String get() = JSON_TYPE

    override val extension: String get() = JSON_EXTENSION

    override fun render(failure: UnexpectedFailure): String {
        val fields = listOf(
            "id" to string(failure.id.value),
            "time" to string(failure.at.toString()),
            "severity" to string(failure.severity.name.lowercase()),
            "origin" to originOf(failure),
            "thread" to (failure.thread?.let(::string) ?: NULL),
            "fingerprint" to string(failure.fingerprint.value),
            "suppressed" to failure.suppressed.toString(),
            "causes" to arrayOf(causeChainOf(failure.cause, options).map(::causeOf)),
        )
        return cut(objectOf(fields), options.maxLength)
    }

    override fun toString(): String = "CrashReportRenderer.json($options)"

    private fun originOf(failure: UnexpectedFailure): String =
        objectOf(listOf("kind" to string(failure.origin.kind), "subject" to string(failure.origin.subject)))

    private fun causeOf(cause: CauseEntry): String = objectOf(
        listOf(
            "type" to string(cause.type),
            "message" to (cause.message?.let(::string) ?: NULL),
            "frames" to arrayOf(cause.frames.map(::string)),
        ),
    )

    private fun objectOf(fields: List<Pair<String, String>>): String =
        fields.joinToString(prefix = "{", postfix = "}", separator = ",") { (key, value) -> "${string(key)}:$value" }

    private fun arrayOf(values: List<String>): String = values.joinToString(
        prefix = "[",
        postfix = "]",
        separator = ",",
    )

    private fun string(text: String): String = buildString(text.length + 2) {
        append('"')
        var index = 0
        while (index < text.length) {
            val codePoint = codePointAt(text, index)
            append(escapedOf(codePoint))
            index += unitsOf(codePoint).size
        }
        append('"')
    }

    private fun escapedOf(codePoint: Int): String {
        val simple = escapes[codePoint.toChar()].takeIf { codePoint < SUPPLEMENTARY }
        if (simple != null) return simple
        val isSafe = SafeText.isSafe(codePoint)
        return unitsOf(codePoint).joinToString(separator = "") { unit -> unicodeOf(unit, isSafe) }
    }

    private fun unicodeOf(unit: Char, isSafe: Boolean): String {
        if (isSafe) return unit.toString()
        return "\\u" + unit.code.toString(RADIX).padStart(HEX_DIGITS, '0')
    }
}
