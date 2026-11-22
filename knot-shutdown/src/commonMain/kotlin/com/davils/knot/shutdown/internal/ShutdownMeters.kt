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

package com.davils.knot.shutdown.internal

import com.davils.kollect.core.Family
import com.davils.kollect.core.MeterRegistry
import com.davils.kollect.core.declare.FamilyBuilder
import com.davils.kollect.core.meter.Counter
import com.davils.kollect.core.meter.Gauge
import com.davils.kollect.core.meter.MeasureUnit
import com.davils.kollect.core.meter.StateSet
import com.davils.kollect.core.meter.Timer
import kotlin.time.Duration
import kotlin.time.DurationUnit

private const val NAME: String = "name"
private const val OUTCOME: String = "outcome"
private const val REASON: String = "reason"

internal class ShutdownMeters(registry: MeterRegistry, private val name: String) {
    private val tasks: Family<Counter> = counterOf(
        registry = registry,
        metric = "knot.shutdown.tasks",
        text = TASKS_TEXT,
        labels = { labelKeys(NAME, OUTCOME) },
    )
    private val taskDurations: Family<Timer> = timerOf(registry)
    private val duration: Gauge = gaugeOf(
        registry = registry,
        metric = "knot.shutdown.duration",
        text = DURATION_TEXT,
        measure = MeasureUnit.SECONDS,
    ).with(name)
    private val stage: StateSet<ShutdownStage> = stageOf(registry).with(name)
    private val registered: Gauge = gaugeOf(
        registry = registry,
        metric = "knot.shutdown.registered_tasks",
        text = REGISTERED_TEXT,
    ).with(name)
    private val refusals: Family<Counter> = counterOf(
        registry = registry,
        metric = "knot.shutdown.refused_registrations",
        text = REFUSALS_TEXT,
        labels = { labelKeys(NAME, REASON) },
    )
    private val escalations: Counter = counterOf(
        registry = registry,
        metric = "knot.shutdown.escalations",
        text = ESCALATIONS_TEXT,
        labels = { labelKeys(NAME) },
    ).with(name)
    private val rejectedEvents: Counter = counterOf(
        registry = registry,
        metric = "knot.shutdown.rejected_events",
        text = REJECTED_TEXT,
        labels = { labelKeys(NAME) },
    ).with(name)

    fun stage(next: ShutdownStage) {
        stage.set(next)
    }

    fun registered(count: Int) {
        registered.set(count.toDouble())
    }

    fun task(outcome: OutcomeKind, elapsed: Duration?) {
        val counter = tasks.with(name, outcome.label)
        counter.increment()
        if (elapsed == null) return
        val timer = taskDurations.with(name, outcome.label)
        timer.record(elapsed)
    }

    fun terminated(elapsed: Duration) {
        duration.set(elapsed.toDouble(DurationUnit.SECONDS))
    }

    fun refused(reason: String) {
        val counter = refusals.with(name, reason)
        counter.increment()
    }

    fun escalated() {
        escalations.increment()
    }

    fun rejectedEvent() {
        rejectedEvents.increment()
    }

    private companion object {
        const val TASKS_TEXT: String = "The tasks of a shutdown by how they ended."
        const val DURATION_TEXT: String = "How long the last run of a shutdown took."
        const val REGISTERED_TEXT: String = "The tasks a shutdown holds."
        const val REFUSALS_TEXT: String = "The registrations a shutdown refused, by why."
        const val ESCALATIONS_TEXT: String = "The times the watchdog of a shutdown halted the process."
        const val REJECTED_TEXT: String = "The events of a shutdown that its event bus did not take."

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

        fun gaugeOf(
            registry: MeterRegistry,
            metric: String,
            text: String,
            measure: MeasureUnit = MeasureUnit.NONE,
        ): Family<Gauge> {
            val registration = registry.gauge(metric) {
                description = text
                unit = measure
                labelKeys(NAME)
            }
            return registration.family
        }

        fun timerOf(registry: MeterRegistry): Family<Timer> {
            val registration = registry.timer("knot.shutdown.task.duration") {
                description = "How long the tasks of a shutdown ran, by how they ended."
                labelKeys(NAME, OUTCOME)
            }
            return registration.family
        }

        fun stageOf(registry: MeterRegistry): Family<StateSet<ShutdownStage>> {
            val registration = registry.stateSet(name = "knot.shutdown.state", initial = ShutdownStage.Running) {
                description = "Whether a shutdown is running, shutting down or terminated."
                labelKeys(NAME)
            }
            return registration.family
        }
    }
}
