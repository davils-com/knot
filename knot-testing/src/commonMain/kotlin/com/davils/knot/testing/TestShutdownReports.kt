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

package com.davils.knot.testing

import com.davils.knot.shutdown.ShutdownPhase
import com.davils.knot.shutdown.ShutdownReason
import com.davils.knot.shutdown.exit.ExitCode
import com.davils.knot.shutdown.report.PhaseReport
import com.davils.knot.shutdown.report.ShutdownReport
import com.davils.knot.shutdown.report.TaskOutcome
import com.davils.knot.shutdown.report.TaskReport
import com.davils.knot.shutdown.task.ShutdownTaskName
import kotlin.time.Duration
import kotlin.time.Instant

/**
 * Ready reports of shutdowns, for tests of code that reads them.
 *
 * @since 1.0.0
 */
public object TestShutdownReports {
    /**
     * The time every test shutdown started, unless it says otherwise.
     *
     * @since 1.0.0
     */
    public val STARTED_AT: Instant = Instant.parse("2026-11-22T12:00:00Z")

    /**
     * Returns the report of one task.
     *
     * @param name the name of the task.
     * @param phase its phase; [ShutdownPhase.Release] by default.
     * @param outcome how it ended; completed at once by default.
     * @return the report.
     * @since 1.0.0
     */
    public fun task(
        name: String,
        phase: ShutdownPhase = ShutdownPhase.Release,
        outcome: TaskOutcome = TaskOutcome.Completed(Duration.ZERO),
    ): TaskReport = TaskReport(ShutdownTaskName(name), phase, outcome)

    /**
     * Returns the report of a phase with [tasks].
     *
     * @param phase the phase.
     * @param tasks the reports of its tasks.
     * @return the report, which took no time.
     * @since 1.0.0
     */
    public fun phase(phase: ShutdownPhase, vararg tasks: TaskReport): PhaseReport =
        PhaseReport(phase, tasks.toList(), Duration.ZERO)

    /**
     * Returns the report of a shutdown with [phases].
     *
     * @param phases the reports of its phases.
     * @param reason why it ran; requested by default.
     * @param exitCode the exit code it chose; [ExitCode.SUCCESS] by default.
     * @return the report, which started at [STARTED_AT] and took no time.
     * @since 1.0.0
     */
    public fun report(
        vararg phases: PhaseReport,
        reason: ShutdownReason = ShutdownReason.Requested(),
        exitCode: ExitCode = ExitCode.SUCCESS,
    ): ShutdownReport = ShutdownReport(
        reason = reason,
        startedAt = STARTED_AT,
        elapsed = Duration.ZERO,
        phases = phases.toList(),
        exitCode = exitCode,
    )
}
