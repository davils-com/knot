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

@file:OptIn(DavilsInternal::class)

package com.davils.knot.shutdown.report

import com.davils.knot.shutdown.ShutdownPhase
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.collection.immutableList
import kotlin.time.Duration

/**
 * How one phase of a shutdown ended.
 *
 * @property phase the phase.
 * @param tasks the reports of its tasks, in the order of their declaration.
 * @property elapsed how long it ran.
 * @since 1.0.0
 */
public class PhaseReport(public val phase: ShutdownPhase, tasks: List<TaskReport>, public val elapsed: Duration) {
    /**
     * The reports of the tasks of the phase, in the order of their declaration.
     *
     * @since 1.0.0
     */
    public val tasks: List<TaskReport> = tasks.immutableList()

    /**
     * Whether every task of the phase completed.
     *
     * @since 1.0.0
     */
    public val isClean: Boolean get() = tasks.all { task -> task.isCompleted }

    /**
     * Tells whether [other] reports the same phase with the same tasks in the same time.
     *
     * @param other the object to compare with.
     * @return `true` if [other] is equal.
     * @since 1.0.0
     */
    override fun equals(other: Any?): Boolean =
        other is PhaseReport && phase == other.phase && tasks == other.tasks && elapsed == other.elapsed

    /**
     * Returns a hash code consistent with [equals].
     *
     * @return the hash code.
     * @since 1.0.0
     */
    override fun hashCode(): Int = listOf(phase, tasks, elapsed).hashCode()

    /**
     * Returns the phase and its tasks.
     *
     * @return the report.
     * @since 1.0.0
     */
    override fun toString(): String = "PhaseReport($phase in $elapsed: $tasks)"
}
