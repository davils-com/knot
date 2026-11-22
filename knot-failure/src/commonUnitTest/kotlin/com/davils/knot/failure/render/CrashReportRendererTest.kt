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

package com.davils.knot.failure.render

import com.davils.knot.failure.FailureLimits
import com.davils.knot.failure.FailureOrigin
import com.davils.knot.failure.fixture.BROKEN
import com.davils.knot.failure.fixture.FAILURE_ID
import com.davils.knot.failure.fixture.unexpected
import com.davils.knot.failure.internal.qualifiedTypeOf
import com.davils.kore.testing.HostileFragments
import com.davils.kore.testing.isSafeToShow
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldEndWith
import io.kotest.matchers.string.shouldNotContain
import io.kotest.matchers.string.shouldStartWith

private const val SECRET: String = "p4ssw0rd-in-message"
private const val CUT_LENGTH: Int = 40
private const val QUOTE: String = "\""
private const val BACKSLASH: String = "\\"
private const val NEWLINE: String = "\n"
private const val TAB: String = "\t"
private const val RETURN: String = "\r"

class CrashReportRendererTest : FunSpec({
    context("the text report") {
        test("lists the facts of the failure and its causes") {
            val cause = IllegalStateException(BROKEN, IllegalArgumentException("inner"))
            val text = CrashReportRenderer.text().render(unexpected(cause))

            val header = listOf(
                "crash report",
                "",
                "id:          $FAILURE_ID",
                "time:        2026-11-22T12:30:45.123Z",
                "severity:    fatal",
                "origin:      thread \"main\"",
                "thread:      main",
                "fingerprint: 00000000000000ff",
                "suppressed:  2",
                "",
            )

            text.lines().take(header.size) shouldBe header
            text shouldContain "IllegalStateException: broken"
            text shouldContain "caused by: "
            text shouldContain "IllegalArgumentException: inner"
            CrashReportRenderer.text().contentType shouldBe "text/plain; charset=utf-8"
            CrashReportRenderer.text().extension shouldBe "txt"
        }

        test("leaves out messages and frames when asked to") {
            val options = RenderOptions(hasMessages = false, hasStackTrace = false, maxCauseDepth = 0)
            val text = CrashReportRenderer.text(options).render(unexpected(IllegalStateException(SECRET)))

            text shouldNotContain SECRET
            text shouldNotContain "    at "
            text shouldEndWith "IllegalStateException\n"
        }

        test("shows hostile text escaped") {
            HostileFragments.all.forEach { fragment ->
                val failure = unexpected(IllegalStateException(fragment), FailureOrigin.Thread(fragment), fragment)
                val text = CrashReportRenderer.text().render(failure)
                val lines = text.lines()

                lines.all { line -> isSafeToShow(line) } shouldBe true
            }
        }

        test("cuts a long report and marks it") {
            val text = CrashReportRenderer.text(RenderOptions(maxLength = CUT_LENGTH)).render(unexpected())

            text.length shouldBe CUT_LENGTH
            text shouldEndWith "[cut]"
        }
    }

    context("the JSON report") {
        test("holds the facts in one object") {
            val options = RenderOptions(hasStackTrace = false, maxCauseDepth = 0)
            val json = CrashReportRenderer.json(options).render(unexpected(thread = null))

            val type = qualifiedTypeOf(IllegalStateException(BROKEN))

            json shouldBe """{"id":"$FAILURE_ID","time":"2026-11-22T12:30:45.123Z","severity":"fatal",""" +
                """"origin":{"kind":"thread","subject":"main"},"thread":null,"fingerprint":"00000000000000ff",""" +
                """"suppressed":2,"causes":[{"type":"$type","message":"broken","frames":[]}]}"""
            CrashReportRenderer.json().contentType shouldBe "application/json"
            CrashReportRenderer.json().extension shouldBe "json"
        }

        test("escapes quotes, controls and unsafe code points") {
            val options = RenderOptions(hasStackTrace = false, maxCauseDepth = 0, hasMessages = true)
            val message = "a" +
                QUOTE +
                "b" +
                BACKSLASH +
                "c" +
                NEWLINE +
                "d" +
                TAB +
                "e" +
                RETURN +
                HostileFragments.RIGHT_TO_LEFT_OVERRIDE +
                HostileFragments.TAG_LATIN_A +
                "é"
            val json = CrashReportRenderer.json(options).render(unexpected(IllegalStateException(message)))

            val escaped = """"message":"a\"b\\c\nd\te\r\u202e\udb40\udc41é""""

            json shouldContain escaped
        }

        test("leaves out a message when asked to") {
            val options = RenderOptions(hasMessages = false, hasStackTrace = false, maxCauseDepth = 0)
            val json = CrashReportRenderer.json(options).render(unexpected(IllegalStateException(SECRET)))

            json shouldNotContain SECRET
            json shouldContain "\"message\":null"
        }

        test("shows its renderer") {
            CrashReportRenderer.json().toString() shouldStartWith "CrashReportRenderer.json(RenderOptions("
            CrashReportRenderer.text().toString() shouldStartWith "CrashReportRenderer.text(RenderOptions("
        }
    }

    context("options") {
        test("refuse bounds they cannot keep") {
            shouldThrow<IllegalArgumentException> { RenderOptions(maxCauseDepth = -1) }
            shouldThrow<IllegalArgumentException> { RenderOptions(maxCauseDepth = FailureLimits.MAX_CAUSE_DEPTH + 1) }
            shouldThrow<IllegalArgumentException> { RenderOptions(maxLength = 0) }
            RenderOptions().toString() shouldBe "RenderOptions(hasMessages=true, hasStackTrace=true, " +
                "maxCauseDepth=8, maxLength=1048576)"
        }
    }
})
