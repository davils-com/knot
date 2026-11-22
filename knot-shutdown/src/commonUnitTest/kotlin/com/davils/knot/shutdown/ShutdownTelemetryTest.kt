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

import com.davils.knot.shutdown.fixture.RecordingTracer
import com.davils.knot.shutdown.fixture.gauge
import com.davils.knot.shutdown.fixture.sum
import com.davils.knot.shutdown.fixture.virtualShutdown
import com.davils.knot.shutdown.fixture.withScope
import com.davils.kollect.core.meterRegistry
import com.davils.konfetti.testing.EventRecorder
import com.davils.konfetti.testing.testEventBus
import io.kotest.common.ExperimentalKotest
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList

private const val TASK: String = "task"
private const val BROKEN: String = "broken"
private const val TASKS_METRIC: String = "knot.shutdown.tasks"
private const val NAME: String = "application"
private const val EVENT_COUNT: Int = 5

class ShutdownTelemetryTest : FunSpec({
    coroutineTestScope = true

    context("events") {
        test("tell the start, every phase, every task and the end on the flow") {
            val shutdown = virtualShutdown { stop { task(TASK) { action {} } } }
            val events = async(start = CoroutineStart.UNDISPATCHED) { shutdown.eventFlow.take(EVENT_COUNT).toList() }

            shutdown.initiate()

            events.await().map { event -> event::class.simpleName } shouldBe listOf(
                "Initiated",
                "PhaseStarted",
                "TaskConcluded",
                "PhaseCompleted",
                "Terminated",
            )
        }

        test("reach the topic of the event bus when it has consumers") {
            withScope { scope ->
                val bus = testEventBus(scope)
                val shutdown = virtualShutdown {
                    events = bus
                    stop { task(TASK) { action {} } }
                }
                val recorder = EventRecorder<ShutdownEvent>()
                bus[ShutdownEvent.TOPIC].consume("recorder") { handle(recorder::record) }

                shutdown.initiate()

                recorder.awaitCount(EVENT_COUNT)
                recorder.payloads.first().shouldBeInstanceOf<ShutdownEvent.Initiated>()
                recorder.payloads.last().shouldBeInstanceOf<ShutdownEvent.Terminated>()
            }
        }

        test("skip a topic without consumers") {
            withScope { scope ->
                val shutdown = virtualShutdown { events = testEventBus(scope) }

                shutdown.initiate()

                shutdown.metrics().rejectedEvents shouldBe 0L
            }
        }
    }

    context("meters") {
        test("count the tasks by outcome and keep the state and the duration") {
            val registry = meterRegistry()
            val shutdown = virtualShutdown {
                meters = registry
                stop {
                    task(TASK) { action {} }
                    task(BROKEN) { action { error(BROKEN) } }
                }
            }
            registry.gauge("knot.shutdown.registered_tasks", NAME) shouldBe 2.0

            shutdown.initiate()

            registry.sum(TASKS_METRIC, NAME, "completed") shouldBe 1.0
            registry.sum(TASKS_METRIC, NAME, "failed") shouldBe 1.0
            registry.gauge("knot.shutdown.duration", NAME) shouldBe 0.0
        }

        test("count refused registrations by why") {
            val registry = meterRegistry()
            val shutdown = virtualShutdown { meters = registry }
            shutdown.register {}
            shutdown.initiate()
            shutdown.register { stop { task(TASK) { action {} } } }

            registry.sum("knot.shutdown.refused_registrations", NAME, "invalid") shouldBe 1.0
            registry.sum("knot.shutdown.refused_registrations", NAME, "shutting_down") shouldBe 1.0
        }
    }

    context("metrics") {
        test("count every outcome") {
            val shutdown = virtualShutdown { stop { task(TASK) { action {} } } }

            shutdown.initiate()

            val metrics = shutdown.metrics()
            metrics.completed shouldBe 1L
            metrics.failed shouldBe 0L
            metrics.toString() shouldBe "ShutdownMetrics(registeredTasks=1, completed=1, failed=0, timedOut=0, " +
                "abandoned=0, deadlineExceeded=0, refusedRegistrations=0, escalations=0, rejectedEvents=0)"
        }
    }

    context("spans") {
        test("cover the shutdown and every task") {
            val tracer = RecordingTracer()
            val shutdown = virtualShutdown {
                this.tracer = tracer
                stop {
                    task(TASK) { action {} }
                    task(BROKEN) { action { error(BROKEN) } }
                }
            }

            shutdown.initiate()

            tracer.spans shouldBe listOf(
                "knot.shutdown.task Ok [knot.shutdown.task=task, knot.shutdown.phase=Stop, " +
                    "knot.shutdown.outcome=completed]",
                "knot.shutdown.task Error [knot.shutdown.task=broken, knot.shutdown.phase=Stop, " +
                    "knot.shutdown.outcome=failed]",
                "knot.shutdown Error [knot.shutdown.name=application, " +
                    "knot.shutdown.reason=Requested(source=none, exitCode=none)]",
            )
        }
    }
})
