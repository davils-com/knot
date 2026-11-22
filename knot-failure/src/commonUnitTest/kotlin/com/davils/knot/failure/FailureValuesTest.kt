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

package com.davils.knot.failure

import com.davils.knot.failure.fixture.BROKEN
import com.davils.knot.failure.fixture.FAILURE_ID
import com.davils.knot.failure.fixture.unexpected
import com.davils.knot.failure.render.CrashReportRenderer
import com.davils.knot.failure.sink.CrashReport
import com.davils.kore.testing.HostileFragments
import com.davils.kore.testing.isSafeToShow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldNotContain

private const val SECRET: String = "s3cr3t-message"
private const val FINGERPRINT_DIGITS: Int = 16
private const val MAIN: String = """"main""""

class FailureValuesTest : FunSpec({
    context("origins") {
        test("are equal by kind and subject and show the subject escaped") {
            FailureOrigin.Thread(BROKEN) shouldBe FailureOrigin.Thread(BROKEN)
            FailureOrigin.Thread(BROKEN).hashCode() shouldBe FailureOrigin.Thread(BROKEN).hashCode()
            FailureOrigin.Thread(BROKEN) shouldNotBe FailureOrigin.Coroutine(BROKEN)
            FailureOrigin.Thread(BROKEN).equals(BROKEN) shouldBe false
            listOf(
                FailureOrigin.Coroutine("c"),
                FailureOrigin.Job("j"),
                FailureOrigin.Action("a"),
                FailureOrigin.Startup("s"),
                FailureOrigin.Shutdown("d"),
                FailureOrigin.Component(kind = "plugin", name = "p"),
            ).map { origin -> origin.toString() } shouldBe listOf(
                "coroutine \"c\"",
                "job \"j\"",
                "action \"a\"",
                "startup \"s\"",
                "shutdown \"d\"",
                "plugin \"p\"",
            )
            HostileFragments.all.forEach { fragment ->
                isSafeToShow(
                    FailureOrigin.Job(fragment).toString(),
                ) shouldBe true
            }
        }
    }

    context("failures") {
        test("are equal by id and never show a message") {
            val failure = unexpected(IllegalStateException(SECRET))

            failure shouldBe unexpected()
            failure.hashCode() shouldBe unexpected().hashCode()
            failure.equals(BROKEN) shouldBe false
            failure.toString() shouldNotContain SECRET
            val shown = "UnexpectedFailure($FAILURE_ID, IllegalStateException from thread $MAIN, Fatal, " +
                "fingerprint=00000000000000ff, suppressed=2, thread="

            failure.toString() shouldBe "$shown$MAIN)"
            unexpected(thread = null).toString() shouldBe "${shown}none)"
        }
    }

    context("fingerprints") {
        test("are 16 hex digits, the same for the same failure and different for another type") {
            val first = FailureFingerprint.of(IllegalStateException(BROKEN))

            first.value.length shouldBe FINGERPRINT_DIGITS
            first.value.all { digit -> digit in '0'..'9' || digit in 'a'..'f' } shouldBe true
            first.toString() shouldBe first.value
            FailureFingerprint.of(IllegalArgumentException(BROKEN)) shouldNotBe first
        }
    }

    context("reports") {
        test("show what happened without a message") {
            val failure = unexpected(IllegalStateException(SECRET))

            FailureReport.Unrecorded(failure).toString() shouldBe "Unrecorded($FAILURE_ID)"
            FailureReport.Suppressed(failure).toString() shouldBe "Suppressed($FAILURE_ID)"
            FailureReport.Recorded(failure, emptyList()).toString() shouldBe "Recorded($FAILURE_ID, [])"
            FailureReport.Ignored(IllegalStateException(SECRET)).toString() shouldBe "Ignored(IllegalStateException)"
            FailureEvent.Occurred(failure).toString() shouldNotContain SECRET
        }
    }

    context("crash reports") {
        test("suggest a file name in UTC and render once") {
            val report = CrashReport(unexpected(), "disk", CrashReportRenderer.json())

            report.suggestedName shouldBe "crash-20261122-123045-123-$FAILURE_ID.json"
            report.contentType shouldBe "application/json"
            report.text shouldBe report.text
            report.toString() shouldBe "CrashReport($FAILURE_ID for \"disk\")"
        }
    }

    context("ids and metrics") {
        test("show their values") {
            FailureId(FAILURE_ID).toString() shouldBe FAILURE_ID
            FailureMetrics(
                reported = 1,
                suppressed = 2,
                ignored = 3,
                written = 4,
                sinkFailures = 5,
                recent = 6,
                rejectedEvents = 7,
            ).toString() shouldBe "FailureMetrics(reported=1, suppressed=2, " +
                "ignored=3, written=4, sinkFailures=5, recent=6, rejectedEvents=7)"
        }
    }
})
