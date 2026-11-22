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

package com.davils.knot.shutdown.process

import com.davils.knot.shutdown.Shutdown
import com.davils.knot.shutdown.ShutdownReason
import com.davils.knot.shutdown.ShutdownState
import com.davils.knot.shutdown.ShutdownViolationCodes
import com.davils.knot.shutdown.exit.ExitCode
import com.davils.knot.shutdown.process.fixture.ProcessEnd
import com.davils.knot.shutdown.process.fixture.RecordingRuntime
import com.davils.knot.shutdown.shutdown
import com.davils.kollect.core.meterRegistry
import com.davils.kore.core.validation.report.ValidationException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import java.util.concurrent.Executors
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

private const val HOOK: String = "knot-shutdown-application"
private const val BROKEN: String = "broken"
private const val TASK: String = "task"
private const val REQUESTED_CODE: Int = 2

private val background: CoroutineScope = CoroutineScope(Executors.newCachedThreadPool().asCoroutineDispatcher())

private fun quickShutdown(): Shutdown = shutdown(background) { stop { task(TASK) { action {} } } }

private fun gatedShutdown(gate: CompletableDeferred<Unit>): Shutdown = shutdown(background) {
    gracePeriod = 1.minutes
    taskTimeout = 1.minutes
    stop { task(TASK) { action { gate.await() } } }
}

class ProcessInstallationTest : FunSpec({
    context("the shutdown hook") {
        test("runs the shutdown when the JVM shuts down") {
            val runtime = RecordingRuntime()
            val installation = quickShutdown().install(runtime)

            runtime.hookNames shouldBe setOf(HOOK)
            runtime.runHooks()

            installation.shutdown.awaitTermination().reason shouldBe ShutdownReason.ProcessExit
        }

        test("is left out when it is switched off") {
            val runtime = RecordingRuntime()
            quickShutdown().install(runtime) { hasExitHook = false }

            runtime.hookNames shouldBe emptySet()
        }
    }

    context("exiting") {
        test("runs the shutdown and ends the process with its exit code") {
            val runtime = RecordingRuntime()
            val installation = quickShutdown().install(runtime)

            val end = shouldThrow<ProcessEnd> {
                installation.exit(ShutdownReason.Requested(exitCode = ExitCode(REQUESTED_CODE)))
            }

            end.code shouldBe ExitCode(REQUESTED_CODE)
            end.isHalt shouldBe false
        }
    }

    context("the watchdog") {
        test("is armed when the shutdown starts, halts when it fires and is disarmed at the end") {
            val runtime = RecordingRuntime()
            val gate = CompletableDeferred<Unit>()
            val shutdown = gatedShutdown(gate)
            shutdown.install(runtime) { escalation = Escalation.Halt(after = 2.seconds) }
            runtime.timerDelays.shouldBeEmpty()

            val running = background.async { shutdown.initiate() }
            shutdown.state.first { state -> state is ShutdownState.ShuttingDown }

            runtime.timerDelays shouldBe listOf(1.minutes + 2.seconds)
            val halt = shouldThrow<ProcessEnd> { runtime.fireTimers() }
            halt.isHalt shouldBe true
            halt.code shouldBe ExitCode.TIMED_OUT
            shutdown.metrics().escalations shouldBe 1L
            gate.complete(Unit)
            running.await()
            runtime.timerDelays.shouldBeEmpty()
        }

        test("counts an escalation in the meters") {
            val runtime = RecordingRuntime()
            val registry = meterRegistry()
            val gate = CompletableDeferred<Unit>()
            val shutdown = shutdown(background) {
                meters = registry
                stop { task(TASK) { action { gate.await() } } }
            }
            shutdown.install(runtime)

            val running = background.async { shutdown.initiate() }
            shutdown.state.first { state -> state is ShutdownState.ShuttingDown }
            shouldThrow<ProcessEnd> { runtime.fireTimers() }
            gate.complete(Unit)
            running.await()

            shutdown.metrics().escalations shouldBe 1L
        }

        test("is never armed without an escalation") {
            val runtime = RecordingRuntime()
            val shutdown = quickShutdown()
            shutdown.install(runtime) { escalation = Escalation.None }

            shutdown.initiate()

            runtime.timerDelays.shouldBeEmpty()
        }
    }

    context("fatal errors") {
        test("are handed to the previous handler and then end the process with the fatal code") {
            val runtime = RecordingRuntime()
            val seen = mutableListOf<Throwable>()
            runtime.installUncaughtHandler { _, cause -> seen += cause }
            quickShutdown().install(runtime) { fatalErrors = FatalErrorPolicy.Exit() }
            val error = StackOverflowError(BROKEN)

            val end = shouldThrow<ProcessEnd> { runtime.crash(error) }

            seen shouldBe listOf(error)
            end.code shouldBe ExitCode.SOFTWARE
        }

        test("that are not fatal only reach the report of the runtime") {
            val runtime = RecordingRuntime()
            val installation = quickShutdown().install(runtime) { fatalErrors = FatalErrorPolicy.Exit() }
            val failure = IllegalStateException(BROKEN)

            runtime.crash(failure)

            runtime.reported shouldBe listOf(failure)
            installation.shutdown.isStarted shouldBe false
        }

        test("are not fatal when the matcher itself fails") {
            val runtime = RecordingRuntime()
            val installation = quickShutdown().install(runtime) {
                fatalErrors = FatalErrorPolicy.Exit { error(BROKEN) }
            }

            runtime.crash(StackOverflowError(BROKEN))

            installation.shutdown.isStarted shouldBe false
        }

        test("leave a running shutdown alone") {
            val runtime = RecordingRuntime()
            val installation = quickShutdown().install(runtime) { fatalErrors = FatalErrorPolicy.Exit() }
            installation.shutdown.initiate()

            runtime.crash(StackOverflowError(BROKEN))

            runtime.ends.shouldBeEmpty()
        }

        test("are ignored by default") {
            val runtime = RecordingRuntime()
            quickShutdown().install(runtime)

            runtime.uncaughtHandler() shouldBe null
        }
    }

    context("uninstalling") {
        test("removes the hook and gives the previous handler back, once") {
            val runtime = RecordingRuntime()
            val previous = Thread.UncaughtExceptionHandler { _, _ -> }
            runtime.installUncaughtHandler(previous)
            val installation = quickShutdown().install(runtime) { fatalErrors = FatalErrorPolicy.Exit() }

            installation.uninstall() shouldBe true
            installation.uninstall() shouldBe false
            runtime.hookNames shouldBe emptySet()
            runtime.uncaughtHandler() shouldBe previous
            installation.isInstalled shouldBe false
        }

        test("keeps a handler installed later") {
            val runtime = RecordingRuntime()
            val installation = quickShutdown().install(runtime) { fatalErrors = FatalErrorPolicy.Exit() }
            val later = Thread.UncaughtExceptionHandler { _, _ -> }
            runtime.installUncaughtHandler(later)

            installation.uninstall() shouldBe true
            runtime.uncaughtHandler() shouldBe later
        }

        test("removes only the hook when no handler was installed") {
            val runtime = RecordingRuntime()
            val installation = quickShutdown().install(runtime)

            installation.uninstall() shouldBe true
            runtime.hookNames shouldBe emptySet()
        }

        test("is refused once the shutdown started") {
            val runtime = RecordingRuntime()
            val installation = quickShutdown().install(runtime)
            installation.shutdown.initiate()

            installation.uninstall() shouldBe false
        }
    }

    context("the real JVM") {
        test("takes an installation by default and gives it back") {
            val installation = quickShutdown().install()

            installation.isInstalled shouldBe true
            installation.uninstall() shouldBe true
        }
    }

    context("declaration") {
        test("refuses an escalation without a positive span") {
            shouldThrow<ValidationException> {
                quickShutdown().install(RecordingRuntime()) { escalation = Escalation.Halt(after = Duration.ZERO) }
            }
        }

        test("warns when nothing triggers the shutdown") {
            val builder = ProcessInstallationBuilder()
            builder.hasExitHook = false

            builder.validate().warnings.map { warning -> warning.code } shouldBe listOf(
                ShutdownViolationCodes.NO_TRIGGER,
            )
        }

        test("shows itself") {
            val installation = quickShutdown().install(RecordingRuntime())

            installation.toString() shouldBe "ProcessInstallation(Shutdown(\"application\", Running), " +
                "ProcessInstallationConfiguration(hasExitHook=true, fatalErrors=Ignore, " +
                "escalation=Halt(after=10s, exitCode=124)), installed=true)"
            FatalErrorPolicy.Exit().toString() shouldBe "Exit"
            Escalation.None.toString() shouldBe "None"
            Escalation.Halt() shouldBe Escalation.Halt()
            Escalation.Halt().hashCode() shouldBe Escalation.Halt().hashCode()
            Escalation.Halt().equals(TASK) shouldBe false
            Escalation.Halt(after = 1.seconds) shouldNotBe Escalation.Halt()
            Escalation.Halt(exitCode = ExitCode.FAILURE) shouldNotBe Escalation.Halt()
        }
    }
})
