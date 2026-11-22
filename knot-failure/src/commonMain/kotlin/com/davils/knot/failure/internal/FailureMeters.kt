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

package com.davils.knot.failure.internal

import com.davils.knot.failure.FailureOrigin
import com.davils.knot.failure.FailureSeverity
import com.davils.kollect.core.Family
import com.davils.kollect.core.MeterRegistry
import com.davils.kollect.core.declare.FamilyBuilder
import com.davils.kollect.core.meter.Counter
import com.davils.kollect.core.meter.Gauge

private const val ORIGIN: String = "origin"
private const val SEVERITY: String = "severity"
private const val SINK: String = "sink"
private const val OUTCOME: String = "outcome"

internal class FailureMeters(registry: MeterRegistry) {
    private val failures: Family<Counter> = counterOf(
        registry = registry,
        metric = "knot.failure.failures",
        text = "The unexpected failures that were taken in, by origin and severity.",
        labels = { labelKeys(ORIGIN, SEVERITY) },
    )
    private val suppressed: Counter = counterOf(
        registry = registry,
        metric = "knot.failure.suppressed",
        text = "The failures the storm guard suppressed.",
        labels = {},
    ).meter
    private val ignored: Counter = counterOf(
        registry = registry,
        metric = "knot.failure.ignored",
        text = "The failures that were ignored.",
        labels = {},
    ).meter
    private val writes: Family<Counter> = counterOf(
        registry = registry,
        metric = "knot.failure.sink_writes",
        text = "The writes of crash reports to the sinks, by sink and outcome.",
        labels = { labelKeys(SINK, OUTCOME) },
    )
    private val rejectedEvents: Counter = counterOf(
        registry = registry,
        metric = "knot.failure.rejected_events",
        text = "The events the event bus did not take.",
        labels = {},
    ).meter
    private val recent: Gauge = gaugeOf(registry)

    fun occurred(origin: FailureOrigin, severity: FailureSeverity) {
        val counter = failures.with(origin.kind, severity.name.lowercase())
        counter.increment()
    }

    fun suppressed() {
        suppressed.increment()
    }

    fun ignored() {
        ignored.increment()
    }

    fun written(sink: String, outcome: String) {
        val counter = writes.with(sink, outcome)
        counter.increment()
    }

    fun rejectedEvent() {
        rejectedEvents.increment()
    }

    fun recent(count: Int) {
        recent.set(count.toDouble())
    }

    private companion object {
        fun counterOf(
            registry: MeterRegistry,
            metric: String,
            text: String,
            labels: FamilyBuilder.() -> Unit,
        ): Family<Counter> {
            val registration = registry.counter(metric) {
                description = text
                labels()
            }
            return registration.family
        }

        fun gaugeOf(registry: MeterRegistry): Gauge {
            val registration = registry.gauge("knot.failure.recent") {
                description = "The failures within the last storm window."
            }
            return registration.family.meter
        }
    }
}
