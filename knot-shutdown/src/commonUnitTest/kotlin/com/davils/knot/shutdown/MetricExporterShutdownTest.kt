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

package com.davils.knot.shutdown

import com.davils.knot.shutdown.fixture.RecordingTransport
import com.davils.knot.shutdown.fixture.virtualShutdown
import com.davils.knot.shutdown.fixture.withScope
import com.davils.kollect.core.export.Delivery
import com.davils.kollect.core.export.DropReason
import com.davils.kollect.core.export.metricExporter
import com.davils.kollect.core.meterRegistry
import com.davils.kollect.core.otlp.resource
import io.kotest.common.ExperimentalKotest
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.longs.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.awaitCancellation
import kotlin.time.Duration.Companion.seconds

private const val SERVICE: String = "knot-test"
private const val REQUESTS: String = "app.requests"

class MetricExporterShutdownTest : FunSpec({
    coroutineTestScope = true

    context("the metric exporter helper") {
        test("sends a last snapshot in the flush phase and leaves nothing pending") {
            withScope { scope ->
                val registry = meterRegistry()
                val requests = registry.counter(REQUESTS).family
                requests.meter.increment()
                val transport = RecordingTransport { Delivery.Accepted() }
                val exporter = metricExporter(
                    scope = scope,
                    registry = registry,
                    resource = resource(SERVICE),
                    transport = transport,
                )
                val shutdown = virtualShutdown { metricExporter(exporter) }

                val report = shutdown.initiate()

                report.tasks.map { task -> task.name.value } shouldBe listOf("metric-exporter")
                report.phases.single { phase -> phase.tasks.isNotEmpty() }.phase shouldBe ShutdownPhase.Flush
                report.isClean shouldBe true
                transport.sent shouldBe 1
                exporter.accounting().pending shouldBe 0L
                exporter.accounting().delivered shouldBeGreaterThan 0L
            }
        }

        test("drops what a hanging receiver does not take once the budget is spent") {
            withScope { scope ->
                val registry = meterRegistry()
                val requests = registry.counter(REQUESTS).family
                requests.meter.increment()
                val transport = RecordingTransport { awaitCancellation() }
                val exporter = metricExporter(
                    scope = scope,
                    registry = registry,
                    resource = resource(SERVICE),
                    transport = transport,
                )
                val shutdown = virtualShutdown { metricExporter(exporter, flushTimeout = 2.seconds, name = "metrics") }

                val report = shutdown.initiate()

                report.tasks.map { task -> task.name.value } shouldBe listOf("metrics")
                exporter.accounting().pending shouldBe 0L
                exporter.accounting().dropped.getValue(DropReason.ShutdownDeadline) shouldBeGreaterThan 0L
            }
        }
    }
})
