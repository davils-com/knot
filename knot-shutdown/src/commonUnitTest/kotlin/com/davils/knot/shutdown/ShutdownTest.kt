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
import com.davils.knot.shutdown.fixture.Journal
import com.davils.knot.shutdown.fixture.STARTED_AT
import com.davils.knot.shutdown.fixture.virtualShutdown
import com.davils.knot.shutdown.report.TaskOutcome
import com.davils.knot.shutdown.task.ShutdownContext
import io.kotest.common.ExperimentalKotest
import io.kotest.core.spec.style.FunSpec
import io.kotest.core.test.testCoroutineScheduler
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

private const val FIRST: String = "first"
private const val SECOND: String = "second"
private const val BROKEN: String = "broken"
private const val REQUESTED_CODE: Int = 3

class ShutdownTest : FunSpec({
    coroutineTestScope = true

    context("phases") {
        test("run in their order and skip the phases without tasks") {
            val journal = Journal()
            val shutdown = virtualShutdown {
                flush { task("metrics") { action { journal.write("flush") } } }
                announce { task("readiness") { action { journal.write("announce") } } }
                release { task("database") { action { journal.write("release") } } }
            }

            val report = shutdown.initiate()

            journal.entries shouldBe listOf("announce", "release", "flush")
            report.phases.map { phase -> phase.phase } shouldBe
                listOf(ShutdownPhase.Announce, ShutdownPhase.Release, ShutdownPhase.Flush)
            report.startedAt shouldBe STARTED_AT
            report.isClean shouldBe true
            report.exitCode shouldBe ExitCode.SUCCESS
        }

        test("run the tasks of a phase side by side") {
            val shutdown = virtualShutdown {
                drain {
                    task(FIRST) { action { delay(1.seconds) } }
                    task(SECOND) { action { delay(1.seconds) } }
                }
            }
            val started = testCoroutineScheduler.timeSource.markNow()

            shutdown.initiate()

            started.elapsedNow() shouldBe 1.seconds
        }

        test("run a task after the tasks of its phase it waits on, even after one that failed") {
            val journal = Journal()
            val shutdown = virtualShutdown {
                release {
                    task(SECOND) {
                        after(FIRST)
                        action { journal.write(SECOND) }
                    }
                    task(FIRST) {
                        action {
                            delay(1.seconds)
                            journal.write(FIRST)
                            error(BROKEN)
                        }
                    }
                }
            }

            val report = shutdown.initiate()

            journal.entries shouldBe listOf(FIRST, SECOND)
            report.isClean shouldBe false
            report.exitCode shouldBe ExitCode.FAILURE
        }

        test("report the state of every phase and the end") {
            val states = mutableListOf<ShutdownState>()
            val shutdown = virtualShutdown {
                drain { task(FIRST) { action { states += shutdownStateOf(this) } } }
            }
            shutdown.state.value shouldBe ShutdownState.Running

            val report = shutdown.initiate(ShutdownReason.ProcessExit)

            shutdown.state.value shouldBe ShutdownState.Terminated(report)
            shutdown.isStarted shouldBe true
        }
    }

    context("running once") {
        test("gives every caller the same report and keeps the first reason") {
            val calls = Journal()
            val shutdown = virtualShutdown { stop { task(FIRST) { action { calls.write(FIRST) } } } }

            val first = async { shutdown.initiate(ShutdownReason.Requested(source = "admin")) }
            val second = async { shutdown.initiate(ShutdownReason.ProcessExit) }
            val waiting = async { shutdown.awaitTermination() }

            first.await() shouldBe second.await()
            waiting.await() shouldBe first.await()
            first.await().reason shouldBe ShutdownReason.Requested(source = "admin")
            calls.entries shouldBe listOf(FIRST)
        }
    }

    context("exit codes") {
        test("follow the request, a failure and a fatal error") {
            val requested = ShutdownReason.Requested(exitCode = ExitCode(REQUESTED_CODE))
            val fatal = ShutdownReason.FatalError(IllegalStateException(BROKEN), "main")
            val failing = virtualShutdown { stop { task(FIRST) { action { error(BROKEN) } } } }

            virtualShutdown().initiate(requested).exitCode shouldBe ExitCode(REQUESTED_CODE)
            virtualShutdown().initiate(fatal).exitCode shouldBe ExitCode.SOFTWARE
            failing.initiate(requested).exitCode shouldBe ExitCode.FAILURE
        }

        test("follow the declared codes") {
            val codes = ExitCodes(completed = ExitCode(REQUESTED_CODE))

            virtualShutdown { exitCodes = codes }.initiate().exitCode shouldBe ExitCode(REQUESTED_CODE)
        }
    }

    context("budgets") {
        test("cancel a task that runs out of its timeout") {
            val shutdown = virtualShutdown {
                stop {
                    task(FIRST) {
                        timeout = 2.seconds
                        action { delay(1.minutes) }
                    }
                }
            }

            shutdown
                .initiate()
                .tasks
                .single()
                .outcome shouldBe TaskOutcome.TimedOut(2.seconds)
        }

        test("abandon a task that does not end after it was cancelled") {
            val shutdown = virtualShutdown {
                taskTimeout = 1.seconds
                stop { task(FIRST) { action { withContext(NonCancellable) { delay(1.minutes) } } } }
            }

            shutdown
                .initiate()
                .tasks
                .single()
                .outcome shouldBe TaskOutcome.Abandoned(1.seconds)
        }

        test("cut a task short by the grace period and skip the tasks after it") {
            val shutdown = virtualShutdown {
                gracePeriod = 2.seconds
                taskTimeout = 1.minutes
                stop { task(FIRST) { action { delay(1.minutes) } } }
                release { task(SECOND) { action {} } }
            }

            val outcomes = shutdown.initiate().tasks.map { task -> task.outcome }

            outcomes shouldBe listOf(TaskOutcome.TimedOut(2.seconds), TaskOutcome.DeadlineExceeded)
        }

        test("tell an action its remaining budget") {
            val remaining = mutableListOf<Duration>()
            val shutdown = virtualShutdown {
                stop {
                    task(FIRST) {
                        timeout = 4.seconds
                        action {
                            delay(1.seconds)
                            remaining += this.remaining
                        }
                    }
                }
            }

            shutdown.initiate()

            remaining shouldBe listOf(3.seconds)
        }

        test("report a failure with its cause and how long the task ran") {
            val shutdown = virtualShutdown {
                stop {
                    task(FIRST) {
                        action {
                            delay(5.milliseconds)
                            error(BROKEN)
                        }
                    }
                }
            }

            val outcome = shutdown
                .initiate()
                .tasks
                .single()
                .outcome

            outcome.shouldBeInstanceOf<TaskOutcome.Failed>()
            outcome.cause.message shouldBe BROKEN
            outcome.elapsed shouldBe 5.milliseconds
        }
    }

    context("closing") {
        test("closes a resource in its phase") {
            val journal = Journal()
            val shutdown = virtualShutdown {
                release { closing("resource", AutoCloseable { journal.write("closed") }) }
            }

            shutdown.initiate().isClean shouldBe true
            journal.entries shouldBe listOf("closed")
        }
    }

    context("health") {
        test("is up while the shutdown has not started and down afterwards") {
            val shutdown = virtualShutdown()

            shutdown.check().status.name shouldBe "Up"
            shutdown.initiate()
            shutdown.check().status.name shouldBe "Down"
        }
    }
})

private fun shutdownStateOf(context: ShutdownContext): ShutdownState =
    ShutdownState.ShuttingDown(context.reason, context.phase)
