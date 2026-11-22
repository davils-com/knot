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

import com.davils.knot.shutdown.exit.ExitCode
import com.davils.knot.shutdown.exit.ExitCodes
import com.davils.knot.shutdown.fixture.BROKEN_CLOCK
import com.davils.knot.shutdown.fixture.BrokenClock
import com.davils.knot.shutdown.fixture.STARTED_AT
import com.davils.knot.shutdown.fixture.sum
import com.davils.knot.shutdown.fixture.virtualShutdown
import com.davils.knot.shutdown.fixture.withScope
import com.davils.knot.shutdown.report.PhaseReport
import com.davils.knot.shutdown.report.TaskOutcome
import com.davils.knot.shutdown.report.TaskReport
import com.davils.knot.shutdown.task.ShutdownContext
import com.davils.knot.shutdown.task.ShutdownTaskName
import com.davils.kollect.core.meterRegistry
import com.davils.konfetti.testing.testEventBus
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.common.ExperimentalKotest
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

private const val TASK: String = "task"
private const val OTHER: String = "other"
private const val BROKEN: String = "broken"
private const val NAME: String = "application"
private const val TASKS_METRIC: String = "knot.shutdown.tasks"

class ShutdownEdgeTest : FunSpec({
    coroutineTestScope = true

    context("the event bus helper") {
        test("drains the bus in the drain phase and closes it in the release phase") {
            withScope { scope ->
                val bus = testEventBus(scope)
                val shutdown = virtualShutdown { eventBus(bus) }

                val report = shutdown.initiate()

                report.tasks.map { task -> task.name.value } shouldBe listOf("event-bus-drain", "event-bus-close")
                report.isClean shouldBe true
            }
        }
    }

    context("a broken shutdown") {
        test("fails every waiting caller when the shutdown itself breaks") {
            val shutdown = virtualShutdown { clock = BrokenClock }

            shouldThrow<IllegalStateException> { shutdown.initiate() }.message shouldBe BROKEN_CLOCK
            shutdown.state.value.shouldBeInstanceOf<ShutdownState.ShuttingDown>()
        }
    }

    context("meters of every outcome") {
        test("count timed out, abandoned and skipped tasks") {
            val registry = meterRegistry()
            val shutdown = virtualShutdown {
                meters = registry
                gracePeriod = 2.seconds
                taskTimeout = 1.seconds
                stop {
                    task(TASK) { action { delay(1.minutes) } }
                    task(OTHER) { action { withContext(NonCancellable) { delay(1.minutes) } } }
                }
                release { task(BROKEN) { action {} } }
            }

            shutdown.initiate()

            registry.sum(TASKS_METRIC, NAME, "timed_out") shouldBe 1.0
            registry.sum(TASKS_METRIC, NAME, "abandoned") shouldBe 1.0
            registry.sum(TASKS_METRIC, NAME, "deadline_exceeded") shouldBe 1.0
        }
    }

    context("events a full topic refuses") {
        test("are counted as rejected") {
            withScope { scope ->
                val bus = testEventBus(scope)
                val gate = CompletableDeferred<Unit>()
                val registry = meterRegistry()
                bus.declare(ShutdownEvent.TOPIC) { orderBy { "" } }
                bus[ShutdownEvent.TOPIC].consume("stuck") { handle { gate.await() } }
                delay(1.milliseconds)
                val shutdown = virtualShutdown {
                    events = bus
                    meters = registry
                }

                shutdown.initiate()

                shutdown.metrics().rejectedEvents shouldNotBe 0L
                registry.sum("knot.shutdown.rejected_events", NAME) shouldNotBe 0.0
            }
        }
    }

    context("events a topic refuses without meters") {
        test("are still counted") {
            withScope { scope ->
                val bus = testEventBus(scope)
                val gate = CompletableDeferred<Unit>()
                bus.declare(ShutdownEvent.TOPIC) { orderBy { "" } }
                bus[ShutdownEvent.TOPIC].consume("stuck") { handle { gate.await() } }
                delay(1.milliseconds)
                val shutdown = virtualShutdown { events = bus }

                shutdown.initiate()

                shutdown.metrics().rejectedEvents shouldNotBe 0L
            }
        }
    }

    context("declarations beyond their bounds") {
        test("are refused") {
            val builder = ShutdownBuilder()
            builder.eventCapacity = ShutdownLimits.MAX_EVENT_CAPACITY + 1
            builder.gracePeriod = Duration.ZERO
            builder.stop { repeat(ShutdownLimits.MAX_TASKS + 2) { index -> task("t$index") { action {} } } }

            builder.validate().errors.map { violation -> violation.code } shouldBe listOf(
                ShutdownViolationCodes.INVALID_GRACE_PERIOD,
                ShutdownViolationCodes.INVALID_EVENT_CAPACITY,
                ShutdownViolationCodes.TOO_MANY_TASKS,
            )
        }

        test("show a plan by its tasks") {
            val builder = ShutdownRegistrationBuilder { emptyList() }
            builder.stop { task(TASK) { action {} } }

            builder.build().toString() shouldBe "ShutdownPlan(tasks=1)"
        }

        test("show a request with its source and exit code") {
            ShutdownReason.Requested(TASK, ExitCode.FAILURE).toString() shouldBe
                "Requested(source=\"task\", exitCode=1)"
        }

        test("show a task with its own timeout") {
            val shutdown = virtualShutdown {
                stop {
                    task(TASK) {
                        timeout = 1.seconds
                        action {}
                    }
                }
            }

            shutdown.configuration.tasks
                .single()
                .toString() shouldBe
                "ShutdownTaskConfiguration(task in Stop, after=[], timeout=1s)"
        }
    }

    context("names") {
        test("start with a letter or digit and hold letters, digits and four marks") {
            isValidName("a.b_c:d-e9") shouldBe true
            isValidName("9z") shouldBe true
            isValidName("Z") shouldBe true
            isValidName("") shouldBe false
            isValidName(".a") shouldBe false
            isValidName("a b") shouldBe false
            isValidName("ä") shouldBe false
            isValidName("a/b") shouldBe false
        }

        test("name the type of a failure, or a placeholder for an anonymous one") {
            typeNameOf(IllegalStateException(BROKEN)) shouldBe "IllegalStateException"
            typeNameOf(object : RuntimeException(BROKEN) {}) shouldBe "Throwable"
        }
    }

    context("registration limits") {
        test("refuse a registration that declares more tasks than the limit") {
            val shutdown = virtualShutdown()

            val registration = shutdown.register {
                stop { repeat(ShutdownLimits.MAX_TASKS + 1) { index -> task("t$index") { action {} } } }
            }

            val refusal = (registration as ShutdownRegistration.Refused).refusal
            refusal.toString() shouldBe "Invalid(1 violations)"
            registration.toString() shouldBe "Refused(Invalid(1 violations))"
        }
    }

    context("values") {
        test("tell apart what differs in one part only") {
            val name = ShutdownTaskName(TASK)
            val completed = TaskOutcome.Completed(1.seconds)
            val report = TaskReport(name, ShutdownPhase.Stop, completed)
            val cause = IllegalStateException(BROKEN)

            report shouldNotBe TaskReport(ShutdownTaskName(OTHER), ShutdownPhase.Stop, completed)
            report shouldNotBe TaskReport(name, ShutdownPhase.Release, completed)
            report shouldNotBe TaskReport(name, ShutdownPhase.Stop, TaskOutcome.DeadlineExceeded)
            val otherCause = IllegalStateException(OTHER)
            TaskOutcome.Failed(cause, 1.seconds) shouldNotBe TaskOutcome.Failed(otherCause, 1.seconds)
            PhaseReport(ShutdownPhase.Stop, listOf(report), 1.seconds) shouldNotBe
                PhaseReport(ShutdownPhase.Release, listOf(report), 1.seconds)
            PhaseReport(ShutdownPhase.Stop, listOf(report), 1.seconds) shouldNotBe
                PhaseReport(ShutdownPhase.Stop, emptyList(), 1.seconds)
            ExitCodes() shouldNotBe ExitCodes(completed = ExitCode.FAILURE)
            ExitCodes() shouldNotBe ExitCodes(failed = ExitCode.SUCCESS)
            ShutdownReason.Requested(TASK, ExitCode.SUCCESS) shouldNotBe ShutdownReason.Requested(TASK)
            ShutdownReason.Requested(TASK).equals(TASK) shouldBe false
            ShutdownReason.FatalError(
                cause,
                TASK,
            ) shouldNotBe ShutdownReason.FatalError(IllegalStateException(OTHER), TASK)
            ShutdownReason.FatalError(cause, TASK).equals(TASK) shouldBe false
            ShutdownState.ShuttingDown(ShutdownReason.ProcessExit, ShutdownPhase.Stop) shouldNotBe
                ShutdownState.ShuttingDown(ShutdownReason.Requested(), ShutdownPhase.Stop)
        }

        test("show the declaration of a shutdown and of a task") {
            val shutdown = virtualShutdown { stop { task(TASK) { action {} } } }

            shutdown.configuration.toString() shouldBe
                "ShutdownConfiguration(name=\"application\", gracePeriod=30s, taskTimeout=5s, tasks=1)"
            shutdown.configuration.tasks
                .single()
                .toString() shouldBe
                "ShutdownTaskConfiguration(task in Stop, after=[], timeout=default)"
            shutdown.register { stop { task(OTHER) { action {} } } }.toString() shouldBe "Registered([other])"
        }

        test("show the context of a task") {
            val contexts = mutableListOf<String>()
            val shutdown = virtualShutdown {
                stop { task(TASK) { action { contexts += this.toString() } } }
            }

            shutdown.initiate()

            contexts shouldBe listOf("ShutdownContext(task in Stop, reason=Requested(source=none, exitCode=none))")
        }

        test("create a shutdown without a declaration") {
            shutdown(this).configuration.name shouldBe ShutdownBuilder.DEFAULT_NAME
            STARTED_AT.toString() shouldNotBe ""
            ShutdownContext::class.simpleName shouldBe "ShutdownContext"
            5.milliseconds.isPositive() shouldBe true
        }
    }
})
