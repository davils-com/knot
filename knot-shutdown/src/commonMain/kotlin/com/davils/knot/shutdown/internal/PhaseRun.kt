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

import com.davils.knot.shutdown.ShutdownPhase
import com.davils.knot.shutdown.report.PhaseReport
import com.davils.knot.shutdown.report.TaskReport
import com.davils.knot.shutdown.task.ShutdownTaskName
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

internal class PhaseRun(
    private val phase: ShutdownPhase,
    private val tasks: List<ShutdownTask>,
    private val run: RunContext,
) {
    suspend fun conclude(): PhaseReport {
        val mark = run.timeSource.markNow()
        run.telemetry.phaseStarted(phase, tasks.map { task -> task.name })
        val finished = tasks.associate { task -> task.name to CompletableDeferred<Unit>() }
        val reports = coroutineScope {
            val runs = tasks.map { task -> async { concludeAfter(task, finished) } }
            runs.awaitAll()
        }
        val report = PhaseReport(phase = phase, tasks = reports, elapsed = mark.elapsedNow())
        run.telemetry.phaseCompleted(report)
        return report
    }

    private suspend fun concludeAfter(
        task: ShutdownTask,
        finished: Map<ShutdownTaskName, CompletableDeferred<Unit>>,
    ): TaskReport {
        val awaited = task.after.mapNotNull { dependency -> finished[dependency] }
        awaited.forEach { dependency -> dependency.await() }
        val report = TaskRun(task, run).conclude()
        finished.getValue(task.name).complete(Unit)
        return report
    }
}
