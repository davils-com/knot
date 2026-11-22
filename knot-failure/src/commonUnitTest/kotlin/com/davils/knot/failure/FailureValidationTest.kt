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

import com.davils.knot.failure.fixture.RecordingSink
import com.davils.kore.core.validation.report.ValidationException
import com.davils.kore.core.validation.violation.ViolationCode
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlin.time.Duration

private val detached: CoroutineScope = CoroutineScope(Job())

private fun violationsOf(block: FailureHandlerBuilder.() -> Unit): List<Pair<String, ViolationCode>> {
    val failure = shouldThrow<ValidationException> { failures(detached, block) }
    return failure.report.violations.map { violation -> violation.path.toString() to violation.code }
}

class FailureValidationTest : FunSpec({
    test("refuses a broken storm, capacity and threshold") {
        violationsOf {
            eventCapacity = 0
            degradedAt = 0
            storm {
                window = Duration.ZERO
                maxReports = 0
            }
        } shouldContainExactlyInAnyOrder listOf(
            "eventCapacity" to FailureViolationCodes.INVALID_LIMIT,
            "degradedAt" to FailureViolationCodes.INVALID_LIMIT,
            "storm.window" to FailureViolationCodes.INVALID_WINDOW,
            "storm.maxReports" to FailureViolationCodes.INVALID_LIMIT,
        )
    }

    test("refuses a broken sink") {
        violationsOf {
            sinks {
                sink("", RecordingSink())
                sink("slow", RecordingSink()) {
                    timeout = Duration.INFINITE
                    severities = emptySet()
                }
                sink("slow", RecordingSink())
            }
        } shouldContainExactlyInAnyOrder listOf(
            """sinks[""].name""" to FailureViolationCodes.INVALID_SINK_NAME,
            """sinks["slow"].timeout""" to FailureViolationCodes.INVALID_TIMEOUT,
            """sinks["slow"].severities""" to FailureViolationCodes.NO_SEVERITY,
            """sinks["slow"]""" to ViolationCode.DUPLICATE,
        )
    }

    test("refuses more sinks than the limit") {
        violationsOf {
            sinks { repeat(FailureLimits.MAX_SINKS + 2) { index -> sink("s$index", RecordingSink()) } }
        } shouldBe listOf("sinks" to FailureViolationCodes.TOO_MANY_SINKS)
    }

    test("refuses a name that is too long or not printable") {
        violationsOf {
            sinks {
                sink("x".repeat(FailureLimits.MAX_SINK_NAME_LENGTH + 1), RecordingSink())
                sink("tab\tname", RecordingSink())
            }
        }.map { (_, code) -> code } shouldBe List(2) { FailureViolationCodes.INVALID_SINK_NAME }
    }

    test("shows its declaration") {
        val handler = failures(detached) { sinks { sink("disk", RecordingSink()) } }

        handler.configuration.toString() shouldBe "FailureHandlerConfiguration(storm=FailureStormConfiguration(" +
            "window=1m, maxReports=10), sinks=[disk], FailureClassification(fatal=1, ignore=1))"
        handler.configuration.sinks
            .single()
            .toString() shouldBe "SinkConfiguration(\"disk\", timeout=5s, " +
            "severities=[Recoverable, Fatal], renderer=CrashReportRenderer.text(RenderOptions(hasMessages=true, " +
            "hasStackTrace=true, maxCauseDepth=8, maxLength=1048576)))"
    }

    test("refuses a declaration after the builder was evaluated") {
        val builder = FailureHandlerBuilder()
        builder.build()

        shouldThrow<IllegalStateException> { builder.classify {} }
        shouldThrow<IllegalStateException> { builder.sinks {} }
    }
})
