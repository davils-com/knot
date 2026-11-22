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

import com.davils.knot.shutdown.report.TaskOutcome
import com.davils.knot.shutdown.report.TaskReport
import com.davils.knot.shutdown.task.ShutdownContext
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.ComparableTimeMark
import kotlin.time.Duration

internal class TaskRun(private val task: ShutdownTask, private val run: RunContext) {
    suspend fun conclude(): TaskReport {
        val span = run.telemetry.startTask(task.name, task.phase)
        val report = TaskReport(name = task.name, phase = task.phase, outcome = outcome())
        run.telemetry.concluded(report, span)
        return report
    }

    private suspend fun outcome(): TaskOutcome {
        val budget = minOf(a = task.timeout, b = run.remaining())
        if (!budget.isPositive()) return TaskOutcome.DeadlineExceeded
        return within(budget)
    }

    private suspend fun within(budget: Duration): TaskOutcome {
        val mark = run.timeSource.markNow()
        val context = ShutdownContext(
            reason = run.reason,
            phase = task.phase,
            task = task.name,
            started = mark,
            budget = budget,
        )
        val ended = CompletableDeferred<Throwable?>()
        val work = run.workers.async { task.action(context) }
        work.invokeOnCompletion { cause -> ended.complete(cause) }
        val finished = withTimeoutOrNull(budget) { Finished(ended.await()) } ?: return expired(work, budget)
        return settled(finished.cause, mark)
    }

    private suspend fun expired(work: Job, budget: Duration): TaskOutcome {
        work.cancel()
        val joined = withTimeoutOrNull(run.cancellationGrace) { work.join() }
        if (joined == null) return TaskOutcome.Abandoned(budget)
        return TaskOutcome.TimedOut(budget)
    }

    private fun settled(cause: Throwable?, mark: ComparableTimeMark): TaskOutcome {
        val elapsed = mark.elapsedNow()
        if (cause == null) return TaskOutcome.Completed(elapsed)
        return TaskOutcome.Failed(cause, elapsed)
    }

    private data class Finished(val cause: Throwable?)
}
