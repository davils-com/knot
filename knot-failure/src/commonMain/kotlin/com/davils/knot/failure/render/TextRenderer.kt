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

private const val TITLE: String = "crash report"
private const val WIDTH: Int = 13
private const val INDENT: String = "    at "
private const val CAUSED_BY: String = "caused by: "
private const val NONE: String = "none"
private const val LINE_BREAK: String = "\n"
private const val CUT: String = "\n[cut]"
private const val LINE_LENGTH: Int = 1_024

internal class TextRenderer(private val options: RenderOptions) : CrashReportRenderer {
    override fun render(failure: UnexpectedFailure): String {
        val text = buildString {
            appendLine(TITLE)
            appendLine()
            entry(key = "id", value = failure.id.value)
            entry(key = "time", value = failure.at.toString())
            entry(key = "severity", value = failure.severity.name.lowercase())
            entry(key = "origin", value = failure.origin.toString())
            entry(key = "thread", value = failure.thread?.let(::safe) ?: NONE)
            entry(key = "fingerprint", value = failure.fingerprint.value)
            entry(key = "suppressed", value = failure.suppressed.toString())
            causeChainOf(failure.cause, options).forEachIndexed { index, cause -> cause(index, cause) }
        }
        return cut(text, options.maxLength)
    }

    override fun toString(): String = "CrashReportRenderer.text($options)"

    private fun StringBuilder.entry(key: String, value: String) {
        append("$key:".padEnd(WIDTH))
        appendLine(value)
    }

    private fun StringBuilder.cause(index: Int, cause: CauseEntry) {
        append(CAUSED_BY.takeIf { index > 0 } ?: LINE_BREAK)
        val message = cause.message?.let { text -> ": ${safe(text)}" }.orEmpty()
        appendLine(safe(cause.type) + message)
        cause.frames.forEach { frame -> appendLine(INDENT + safe(frame)) }
    }

    private fun safe(text: String): String = SafeText.text(text, maxLength = LINE_LENGTH)
}

internal fun cut(text: String, maxLength: Int): String {
    if (text.length <= maxLength) return text
    val kept = (maxLength - CUT.length).coerceAtLeast(0)
    return (text.take(kept) + CUT).take(maxLength)
}
