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
import com.davils.knot.failure.fixture.MovableClock
import com.davils.knot.failure.fixture.ORIGIN
import com.davils.knot.failure.fixture.RecordingSink
import com.davils.knot.failure.fixture.failure
import com.davils.knot.failure.fixture.testHandler
import com.davils.knot.failure.sink.SinkResult
import com.davils.knot.shutdown.ShutdownReason
import com.davils.knot.shutdown.shutdown
import io.kotest.common.ExperimentalKotest
import io.kotest.core.spec.style.FunSpec
import io.kotest.core.test.testCoroutineScheduler
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

private const val DISK: String = "disk"
private const val ALERTS: String = "alerts"

class FailureHandlerTest : FunSpec({
    coroutineTestScope = true

    context("reporting") {
        test("hands the report to every sink that takes the severity and waits for them") {
            val disk = RecordingSink()
            val alerts = RecordingSink()
            val handler = testHandler {
                sinks {
                    sink(DISK, disk)
                    sink(ALERTS, alerts) { severities = setOf(FailureSeverity.Fatal) }
                }
            }

            val report = handler.report(failure(), ORIGIN)

            report.shouldBeInstanceOf<FailureReport.Recorded>()
            report.results.map { result -> result.toString() } shouldBe listOf("Written(\"disk\")")
            disk.reports.single().failure shouldBe report.failure
            alerts.reports.shouldBeEmpty()
        }

        test("tells the failure without a sink") {
            val handler = testHandler()

            val report = handler.report(failure(), ORIGIN)

            report.shouldBeInstanceOf<FailureReport.Unrecorded>()
            report.failure.origin shouldBe ORIGIN
            report.failure.severity shouldBe FailureSeverity.Recoverable
            report.failure.suppressed shouldBe 0
        }

        test("ignores a cancellation and a declared failure") {
            val handler = testHandler { classify { ignore<IllegalArgumentException>() } }

            handler.report(CancellationException(BROKEN), ORIGIN).shouldBeInstanceOf<FailureReport.Ignored>()
            handler.report(IllegalArgumentException(BROKEN), ORIGIN).shouldBeInstanceOf<FailureReport.Ignored>()
            handler.metrics().ignored shouldBe 2L
        }

        test("contains a sink that throws and one that does not end in time") {
            val handler = testHandler {
                sinks {
                    sink(name = "throwing", sink = { error(BROKEN) })
                    sink(name = "hanging", sink = { delay(1.minutes) }) { timeout = 1.seconds }
                }
            }

            val report = handler.report(failure(), ORIGIN) as FailureReport.Recorded

            report.results.map { result -> result.toString() } shouldBe listOf(
                "Failed(\"throwing\", IllegalStateException)",
                "TimedOut(\"hanging\", 1s)",
            )
            report.results.none { result -> result.isWritten } shouldBe true
            handler.metrics().sinkFailures shouldBe 2L
        }

        test("puts every reported failure on the flow") {
            val handler = testHandler()
            val first = async(start = CoroutineStart.UNDISPATCHED) { handler.failures.first() }

            val report = handler.report(failure(), ORIGIN) as FailureReport.Unrecorded

            first.await() shouldBe report.failure
        }
    }

    context("fatal failures") {
        test("run the shutdown once, after their report was written") {
            val sink = RecordingSink()
            val target = shutdown(this) { stop { task("stop") { action {} } } }
            val handler = testHandler {
                shutdown = target
                classify { fatal<IllegalArgumentException>() }
                sinks { sink(DISK, sink) }
            }

            handler.report(IllegalStateException(BROKEN, IllegalArgumentException(BROKEN)), ORIGIN)
            handler.report(IllegalArgumentException(BROKEN), ORIGIN)

            val terminated = target.awaitTermination()
            terminated.reason.shouldBeInstanceOf<ShutdownReason.FatalError>()
            sink.reports.size shouldBe 2
            val first = sink.reports.first()
            first.failure.severity shouldBe FailureSeverity.Fatal
        }

        test("are never suppressed by the storm guard") {
            val handler = testHandler {
                classify { fatal<IllegalStateException>() }
                storm { maxReports = 1 }
            }

            handler.report(failure(), ORIGIN).shouldBeInstanceOf<FailureReport.Unrecorded>()
            handler.report(failure(), ORIGIN).shouldBeInstanceOf<FailureReport.Unrecorded>()
        }
    }

    context("submitting") {
        test("writes in the background and drains") {
            val sink = RecordingSink()
            val handler = testHandler { sinks { sink(DISK, sink) } }

            handler.submit(failure(), ORIGIN)
            handler.drain()

            sink.reports.size shouldBe 1
        }

        test("runs the fatal action after the report") {
            val target = shutdown(this) {}
            val handler = testHandler {
                shutdown = target
                classify { fatal<IllegalStateException>() }
            }

            handler.submit(failure(), ORIGIN)
            handler.drain()
            testCoroutineScheduler.advanceUntilIdle()

            target.isStarted shouldBe true
        }

        test("takes the failures of coroutines") {
            val handler = testHandler()
            val coroutineHandler = handler.coroutineHandler()

            val job = launch(SupervisorJob() + coroutineHandler) { error(BROKEN) }
            job.join()

            handler.metrics().reported shouldBe 1L
        }

        test("leaves out ignored failures") {
            val sink = RecordingSink()
            val handler = testHandler { sinks { sink(DISK, sink) } }

            handler.submit(CancellationException(BROKEN), ORIGIN)
            handler.drain()

            sink.reports.shouldBeEmpty()
        }
    }

    context("storms") {
        test("suppress the same failure beyond the limit and tell the next report how many") {
            val clock = MovableClock()
            val handler = testHandler(clock) { storm { maxReports = 1 } }
            val cause = failure()

            handler.report(cause, ORIGIN).shouldBeInstanceOf<FailureReport.Unrecorded>()
            handler.report(cause, ORIGIN).shouldBeInstanceOf<FailureReport.Suppressed>()
            handler.report(cause, ORIGIN).shouldBeInstanceOf<FailureReport.Suppressed>()
            clock.advance(2.minutes)
            val next = handler.report(cause, ORIGIN) as FailureReport.Unrecorded

            next.failure.suppressed shouldBe 2
            handler.metrics().suppressed shouldBe 2L
        }
    }

    context("closing") {
        test("ignores every further failure and is idempotent") {
            val handler = testHandler()
            handler.close()
            handler.close()

            handler.report(failure(), ORIGIN).shouldBeInstanceOf<FailureReport.Ignored>()
            handler.isClosed shouldBe true
        }
    }

    context("health") {
        test("is up, degraded after enough recent failures and down once closed") {
            val clock = MovableClock()
            val handler = testHandler(clock) { degradedAt = 2 }

            handler.check().status.name shouldBe "Up"
            repeat(2) { handler.report(failure(), ORIGIN) }
            handler.check().status.name shouldBe "Degraded"
            clock.advance(2.minutes)
            handler.check().status.name shouldBe "Up"
            handler.close()
            handler.check().status.name shouldBe "Down"
        }
    }

    context("sink results") {
        test("tell what happened") {
            val handler = testHandler { sinks { sink(DISK, RecordingSink()) } }

            val report = handler.report(failure(), ORIGIN) as FailureReport.Recorded

            report.results.single().shouldBeInstanceOf<SinkResult.Written>()
            report.results.single().sink shouldBe DISK
        }
    }
})
