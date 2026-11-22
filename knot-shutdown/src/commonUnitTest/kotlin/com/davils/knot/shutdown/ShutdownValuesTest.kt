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

package com.davils.knot.shutdown

import com.davils.knot.shutdown.exit.ExitCode
import com.davils.knot.shutdown.exit.ExitCodes
import com.davils.knot.shutdown.fixture.STARTED_AT
import com.davils.knot.shutdown.report.PhaseReport
import com.davils.knot.shutdown.report.ShutdownReport
import com.davils.knot.shutdown.report.TaskOutcome
import com.davils.knot.shutdown.report.TaskReport
import com.davils.knot.shutdown.task.ShutdownTaskName
import com.davils.kore.testing.HostileFragments
import com.davils.kore.testing.isSafeToShow
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldNotContain
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

private const val SECRET: String = "s3cr3t-message"
private const val TASK: String = "database"
private const val HIGHEST_CODE: Int = 255
private const val SOURCE: String = "a"
private const val OTHER_SOURCE: String = "b"

private val completed: TaskReport =
    TaskReport(ShutdownTaskName(TASK), ShutdownPhase.Release, TaskOutcome.Completed(4.milliseconds))
private val phase: PhaseReport = PhaseReport(ShutdownPhase.Release, listOf(completed), 4.milliseconds)
private val report: ShutdownReport = ShutdownReport(
    reason = ShutdownReason.ProcessExit,
    startedAt = STARTED_AT,
    elapsed = 4.milliseconds,
    phases = listOf(phase),
    exitCode = ExitCode.SUCCESS,
)

class ShutdownValuesTest : FunSpec({
    context("exit codes") {
        test("lie between 0 and 255") {
            ExitCode(HIGHEST_CODE).value shouldBe HIGHEST_CODE
            shouldThrow<IllegalArgumentException> { ExitCode(-1) }
            shouldThrow<IllegalArgumentException> { ExitCode(HIGHEST_CODE + 1) }
            ExitCode.TIMED_OUT.toString() shouldBe "124"
        }

        test("exit codes are values") {
            ExitCodes() shouldBe ExitCodes()
            ExitCodes().hashCode() shouldBe ExitCodes().hashCode()
            ExitCodes() shouldNotBe ExitCodes(fatal = ExitCode.FAILURE)
            ExitCodes().equals(TASK) shouldBe false
            ExitCodes().toString() shouldBe "ExitCodes(completed=0, failed=1, fatal=70)"
        }
    }

    context("reasons") {
        test("show a source escaped and never the message of a fatal error") {
            val fatal = ShutdownReason.FatalError(IllegalStateException(SECRET), "main")

            fatal.toString() shouldBe "FatalError(IllegalStateException in \"main\")"
            fatal.toString() shouldNotContain SECRET
            HostileFragments.all.forEach { fragment ->
                isSafeToShow(ShutdownReason.Requested(source = fragment).toString()) shouldBe true
                isSafeToShow(
                    ShutdownReason.FatalError(IllegalStateException(SECRET), fragment).toString(),
                ) shouldBe true
            }
        }

        test("reasons are values") {
            val cause = IllegalStateException(SECRET)

            ShutdownReason.Requested(
                SOURCE,
                ExitCode.SUCCESS,
            ) shouldBe ShutdownReason.Requested(SOURCE, ExitCode.SUCCESS)
            ShutdownReason.Requested(SOURCE).hashCode() shouldBe ShutdownReason.Requested(SOURCE).hashCode()
            ShutdownReason.Requested(SOURCE) shouldNotBe ShutdownReason.Requested(OTHER_SOURCE)
            ShutdownReason.FatalError(cause, SOURCE) shouldBe ShutdownReason.FatalError(cause, SOURCE)
            val fatal = ShutdownReason.FatalError(cause, SOURCE)
            fatal.hashCode() shouldBe ShutdownReason.FatalError(cause, SOURCE).hashCode()
            ShutdownReason.FatalError(cause, SOURCE) shouldNotBe ShutdownReason.FatalError(cause, OTHER_SOURCE)
            ShutdownReason.ProcessExit.toString() shouldBe "ProcessExit"
        }
    }

    context("outcomes") {
        test("tell whether the task completed and never show a message") {
            val failed = TaskOutcome.Failed(IllegalStateException(SECRET), 1.seconds)

            TaskOutcome.Completed(1.seconds).isCompleted shouldBe true
            failed.isCompleted shouldBe false
            failed.toString() shouldBe "Failed(IllegalStateException after 1s)"
            TaskOutcome.TimedOut(1.seconds).toString() shouldBe "TimedOut(1s)"
            TaskOutcome.Abandoned(1.seconds).toString() shouldBe "Abandoned(1s)"
            TaskOutcome.DeadlineExceeded.toString() shouldBe "DeadlineExceeded"
            TaskOutcome.Completed(1.seconds).toString() shouldBe "Completed(1s)"
        }

        test("outcomes are values") {
            val cause = IllegalStateException(SECRET)
            val outcomes = listOf(
                TaskOutcome.Completed(1.seconds) to TaskOutcome.Completed(2.seconds),
                TaskOutcome.Failed(cause, 1.seconds) to TaskOutcome.Failed(cause, 2.seconds),
                TaskOutcome.TimedOut(1.seconds) to TaskOutcome.TimedOut(2.seconds),
                TaskOutcome.Abandoned(1.seconds) to TaskOutcome.Abandoned(2.seconds),
            )

            outcomes.forEach { (one, other) ->
                one shouldBe one
                one.hashCode() shouldNotBe other.hashCode()
                one shouldNotBe other
                one.equals(TASK) shouldBe false
            }
        }
    }

    context("reports") {
        test("are values that show what happened") {
            val again = ShutdownReport(
                reason = ShutdownReason.ProcessExit,
                startedAt = STARTED_AT,
                elapsed = 4.milliseconds,
                phases = listOf(PhaseReport(ShutdownPhase.Release, listOf(completed), 4.milliseconds)),
                exitCode = ExitCode.SUCCESS,
            )

            report shouldBe again
            report.hashCode() shouldBe again.hashCode()
            phase.hashCode() shouldBe again.phases.single().hashCode()
            completed.hashCode() shouldBe again.tasks.single().hashCode()
            report.equals(TASK) shouldBe false
            phase.equals(TASK) shouldBe false
            completed.equals(TASK) shouldBe false
            report.tasks shouldBe listOf(completed)
            completed.toString() shouldBe "TaskReport(database in Release: Completed(4ms))"
            phase.toString() shouldBe "PhaseReport(Release in 4ms: [TaskReport(database in Release: Completed(4ms))])"
            report.toString() shouldBe "ShutdownReport(ProcessExit in 4ms, exitCode=0, phases=[$phase])"
        }
    }

    context("states") {
        test("are values that show where the shutdown is") {
            val shuttingDown = ShutdownState.ShuttingDown(ShutdownReason.ProcessExit, ShutdownPhase.Drain)

            shuttingDown shouldBe ShutdownState.ShuttingDown(ShutdownReason.ProcessExit, ShutdownPhase.Drain)
            shuttingDown.hashCode() shouldBe
                ShutdownState.ShuttingDown(ShutdownReason.ProcessExit, ShutdownPhase.Drain).hashCode()
            shuttingDown.equals(TASK) shouldBe false
            shuttingDown.toString() shouldBe "ShuttingDown(Drain, reason=ProcessExit)"
            ShutdownState.Terminated(report).hashCode() shouldBe ShutdownState.Terminated(report).hashCode()
            ShutdownState.Terminated(report).equals(TASK) shouldBe false
            ShutdownState.Terminated(report).toString() shouldBe "Terminated(exitCode=0)"
            ShutdownState.Running.toString() shouldBe "Running"
        }
    }

    context("events") {
        test("show their facts") {
            val events = listOf(
                ShutdownEvent.Initiated(TASK, ShutdownReason.ProcessExit, 1),
                ShutdownEvent.PhaseStarted(TASK, ShutdownPhase.Release, listOf(TASK)),
                ShutdownEvent.TaskConcluded(TASK, completed),
                ShutdownEvent.PhaseCompleted(TASK, phase),
                ShutdownEvent.Terminated(TASK, report),
                ShutdownEvent.Escalated(TASK, ExitCode.TIMED_OUT, 1.seconds),
            )

            events.map { event -> event.toString() } shouldBe listOf(
                "Initiated(database, reason=ProcessExit, tasks=1)",
                "PhaseStarted(database, Release, tasks=1)",
                "TaskConcluded(database, TaskReport(database in Release: Completed(4ms)))",
                "PhaseCompleted(database, Release, clean=true)",
                "Terminated(database, exitCode=0, clean=true)",
                "Escalated(database, exitCode=124, after=1s)",
            )
        }
    }

    context("refusals") {
        test("show why") {
            RegistrationRefusal.ShuttingDown.toString() shouldBe "ShuttingDown"
        }
    }
})
