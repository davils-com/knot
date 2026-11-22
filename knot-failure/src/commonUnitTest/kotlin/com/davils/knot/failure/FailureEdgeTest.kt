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

@file:OptIn(ExperimentalKotest::class)

package com.davils.knot.failure

import com.davils.knot.failure.fixture.BROKEN
import com.davils.knot.failure.fixture.ORIGIN
import com.davils.knot.failure.fixture.RecordingSink
import com.davils.knot.failure.fixture.SilentFailure
import com.davils.knot.failure.fixture.failure
import com.davils.knot.failure.fixture.sum
import com.davils.knot.failure.fixture.testHandler
import com.davils.knot.failure.fixture.unexpected
import com.davils.knot.failure.fixture.withScope
import com.davils.knot.failure.render.CrashReportRenderer
import com.davils.knot.failure.render.RenderOptions
import com.davils.knot.failure.render.codePointAt
import com.davils.kollect.core.meterRegistry
import com.davils.konfetti.testing.testEventBus
import com.davils.kore.core.validation.report.ValidationException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.common.ExperimentalKotest
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

private const val HIGH: Char = '\uD83D'
private const val LOW: Char = '\uDE00'
private const val SMILE: Int = 0x1F600
private const val BOUND_VIOLATIONS: Int = 5

class FailureEdgeTest : FunSpec({
    coroutineTestScope = true

    context("bounds of a declaration") {
        test("are refused beyond their upper limits") {
            shouldThrow<ValidationException> {
                testHandler {
                    eventCapacity = FailureLimits.MAX_EVENT_CAPACITY + 1
                    storm {
                        window = Duration.INFINITE
                        maxReports = FailureLimits.MAX_STORM_REPORTS + 1
                    }
                    sinks {
                        sink(name = "   ", sink = RecordingSink())
                        sink(name = "zero", sink = RecordingSink()) { timeout = Duration.ZERO }
                    }
                }
            }.report.violations.size shouldBe BOUND_VIOLATIONS
            shouldThrow<IllegalArgumentException> { RenderOptions(maxLength = FailureLimits.MAX_REPORT_LENGTH + 1) }
        }

        test("let the shutdown go again") {
            val handler = testHandler {
                shutdown = null
                classify { fatal<IllegalStateException>() }
            }

            handler.report(failure(), ORIGIN).shouldBeInstanceOf<FailureReport.Unrecorded>()
        }
    }

    context("rendering") {
        test("shows a failure without a thread and cuts the chain of causes at its depth") {
            val cause = IllegalStateException(BROKEN, IllegalStateException(BROKEN, IllegalStateException(BROKEN)))
            val renderer = CrashReportRenderer.text(RenderOptions(maxCauseDepth = 1))
            val text = renderer.render(unexpected(cause, thread = null))

            text shouldContain "thread:      none"
            text.lines().count { line -> line.startsWith("caused by") } shouldBe 1
        }

        test("reads surrogate pairs and lone surrogates as code points") {
            codePointAt("$HIGH$LOW", 0) shouldBe SMILE
            codePointAt("${HIGH}x", 0) shouldBe HIGH.code
            codePointAt("$LOW$LOW", 0) shouldBe LOW.code
            codePointAt("$HIGH", 0) shouldBe HIGH.code
        }

        test("escapes a lone surrogate in JSON") {
            val json = CrashReportRenderer
                .json(RenderOptions(hasStackTrace = false, maxCauseDepth = 0))
                .render(unexpected(IllegalStateException("$HIGH")))

            json shouldContain "\\ud83d"
        }

        test("names an anonymous failure by a placeholder") {
            typeNameOf(object : IllegalStateException(BROKEN) {}) shouldBe "Throwable"
        }
    }

    context("events") {
        test("are skipped while the topic has no consumers") {
            withScope { scope ->
                val handler = testHandler { events = testEventBus(scope) }

                handler.report(failure(), ORIGIN)

                handler.metrics().rejectedEvents shouldBe 0L
            }
        }

        test("a topic refuses are counted without meters") {
            withScope { scope ->
                val bus = testEventBus(scope)
                val gate = CompletableDeferred<Unit>()
                bus.declare(FailureEvent.TOPIC) { orderBy { "" } }
                bus[FailureEvent.TOPIC].consume("stuck") { handle { gate.await() } }
                delay(1.milliseconds)
                val handler = testHandler { events = bus }

                handler.report(failure(), ORIGIN)

                (handler.metrics().rejectedEvents > 0L) shouldBe true
            }
        }
    }

    context("meters of failing sinks") {
        test("count failed and timed out writes by sink") {
            val registry = meterRegistry()
            val handler = testHandler {
                meters = registry
                sinks {
                    sink(name = "throwing", sink = { error(BROKEN) })
                    sink(name = "slow", sink = { delay(1.seconds) }) { timeout = 1.milliseconds }
                }
            }

            handler.report(failure(), ORIGIN)

            registry.sum("knot.failure.sink_writes", "throwing", "failed") shouldBe 1.0
            registry.sum("knot.failure.sink_writes", "slow", "timed_out") shouldBe 1.0
            handler.configuration.meters shouldBe registry
        }
    }

    context("failures without a message") {
        test("render a null message and a thread") {
            val renderer = CrashReportRenderer.json(RenderOptions(hasStackTrace = false, maxCauseDepth = 0))
            val json = renderer.render(unexpected(SilentFailure()))
            val builder = FailureHandlerBuilder()

            json shouldContain """"message":null"""
            json shouldContain """"thread":"main""""
            CrashReportRenderer.text().render(
                unexpected(SilentFailure()),
            ) shouldContain "thread:      main"
            builder.shutdown shouldBe null
        }
    }

    context("sinks") {
        test("that take only fatal failures leave a recoverable one unrecorded") {
            val handler = testHandler {
                sinks { sink(name = "alerts", sink = RecordingSink()) { severities = setOf(FailureSeverity.Fatal) } }
            }

            handler.report(failure(), ORIGIN).shouldBeInstanceOf<FailureReport.Unrecorded>()
        }

        test("time out without meters") {
            val handler = testHandler {
                sinks { sink(name = "slow", sink = { delay(1.seconds) }) { timeout = 1.milliseconds } }
            }

            handler.report(failure(), ORIGIN).shouldBeInstanceOf<FailureReport.Recorded>()
        }
    }
})
