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

import com.davils.knot.failure.fixture.ORIGIN
import com.davils.knot.failure.fixture.RecordingSink
import com.davils.knot.failure.fixture.RecordingTracer
import com.davils.knot.failure.fixture.failure
import com.davils.knot.failure.fixture.gaugeValue
import com.davils.knot.failure.fixture.sum
import com.davils.knot.failure.fixture.testHandler
import com.davils.knot.failure.fixture.withScope
import com.davils.knot.shutdown.shutdown
import com.davils.kollect.core.meterRegistry
import com.davils.konfetti.testing.EventRecorder
import com.davils.konfetti.testing.testEventBus
import io.kotest.common.ExperimentalKotest
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

private const val DISK: String = "disk"

class FailureTelemetryTest : FunSpec({
    coroutineTestScope = true

    test("the events reach the topic of the event bus") {
        withScope { scope ->
            val bus = testEventBus(scope)
            val recorder = EventRecorder<FailureEvent>()
            val handler = testHandler {
                events = bus
                sinks { sink(DISK, RecordingSink()) }
            }
            bus[FailureEvent.TOPIC].consume("recorder") { handle(recorder::record) }

            handler.report(failure(), ORIGIN)

            recorder.awaitCount(2)
            recorder.payloads.first().shouldBeInstanceOf<FailureEvent.Occurred>()
            val written = recorder.payloads.last() as FailureEvent.Written
            written.result.isWritten shouldBe true
            written.toString() shouldBe "Written(${written.failure.id}, Written(\"disk\"))"
        }
    }

    test("events a topic refuses are counted") {
        withScope { scope ->
            val bus = testEventBus(scope)
            val gate = CompletableDeferred<Unit>()
            val registry = meterRegistry()
            bus.declare(FailureEvent.TOPIC) { orderBy { "" } }
            bus[FailureEvent.TOPIC].consume("stuck") { handle { gate.await() } }
            delay(1.milliseconds)
            val handler = testHandler {
                events = bus
                meters = registry
            }

            handler.report(failure(), ORIGIN)

            handler.metrics().rejectedEvents shouldNotBe 0L
            registry.sum("knot.failure.rejected_events") shouldNotBe 0.0
        }
    }

    test("the meters count failures by origin and severity, sink writes and the recent ones") {
        val registry = meterRegistry()
        val handler = testHandler {
            meters = registry
            storm { maxReports = 1 }
            sinks { sink(DISK, RecordingSink()) }
        }

        val cause = failure()
        handler.report(cause, ORIGIN)
        handler.report(cause, ORIGIN)
        handler.report(CancellationException(DISK), ORIGIN)

        registry.sum("knot.failure.failures", "job", "recoverable") shouldBe 2.0
        registry.sum("knot.failure.suppressed") shouldBe 1.0
        registry.sum("knot.failure.ignored") shouldBe 1.0
        registry.sum("knot.failure.sink_writes", DISK, "written") shouldBe 1.0
        registry.gaugeValue("knot.failure.recent") shouldBe 2.0
    }

    test("a span is recorded for every reported failure") {
        val tracer = RecordingTracer()
        val handler = testHandler { this.tracer = tracer }

        handler.report(failure(), ORIGIN)

        tracer.spans shouldBe listOf(
            "knot.failure Error [knot.failure.id, knot.failure.origin, knot.failure.severity, " +
                "knot.failure.fingerprint]",
        )
    }

    test("the shutdown drains the handler and closes it") {
        val handler = testHandler()
        val target = shutdown(this) { failures(handler) }

        val report = target.initiate()

        report.tasks.map { task -> task.name.value } shouldBe listOf("failures-drain", "failures-close")
        handler.isClosed shouldBe true
    }

    test("the handler shows itself") {
        testHandler().toString() shouldBe "FailureHandler(closed=false, FailureMetrics(reported=0, suppressed=0, " +
            "ignored=0, written=0, sinkFailures=0, recent=0, rejectedEvents=0))"
    }
})
