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

import com.davils.knot.shutdown.ShutdownConfiguration
import com.davils.knot.shutdown.ShutdownPhase
import com.davils.knot.shutdown.ShutdownReason
import com.davils.knot.shutdown.report.PhaseReport
import com.davils.knot.shutdown.report.ShutdownReport
import kotlinx.coroutines.CoroutineScope

internal class ShutdownSequence(
    private val configuration: ShutdownConfiguration,
    private val telemetry: ShutdownTelemetry,
    private val workers: CoroutineScope,
    private val entering: (ShutdownReason, ShutdownPhase) -> Unit,
) {
    suspend fun run(reason: ShutdownReason, tasks: List<ShutdownTask>): ShutdownReport {
        val startedAt = configuration.clock.now()
        val mark = configuration.timeSource.markNow()
        val span = telemetry.initiated(reason, tasks.size)
        val run = RunContext(
            reason = reason,
            started = mark,
            gracePeriod = configuration.gracePeriod,
            workers = workers,
            timeSource = configuration.timeSource,
            cancellationGrace = configuration.cancellationGrace,
            telemetry = telemetry,
        )
        val phases = ShutdownPhase.entries.mapNotNull { phase -> phaseOf(phase, tasks, run) }
        val isClean = phases.all { phase -> phase.isClean }
        val report = ShutdownReport(
            reason = reason,
            startedAt = startedAt,
            elapsed = mark.elapsedNow(),
            phases = phases,
            exitCode = configuration.exitCodes.codeOf(reason, isClean),
        )
        telemetry.terminated(report, span)
        return report
    }

    private suspend fun phaseOf(phase: ShutdownPhase, tasks: List<ShutdownTask>, run: RunContext): PhaseReport? {
        val members = tasks.filter { task -> task.phase == phase }
        if (members.isEmpty()) return null
        entering(run.reason, phase)
        return PhaseRun(phase, members, run).conclude()
    }
}
