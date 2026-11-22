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

import com.davils.knot.shutdown.ShutdownReason
import com.davils.knot.shutdown.exit.ExitCode
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.collection.immutableList
import kotlin.time.Duration
import kotlin.time.Instant

/**
 * How a shutdown ended: every phase that had tasks, every task and the exit code it chose.
 *
 * @property reason why the shutdown ran.
 * @property startedAt when it started, by the clock of the shutdown.
 * @property elapsed how long it ran.
 * @param phases the reports of the phases that had tasks, in their order.
 * @property exitCode the exit code it chose, see [ExitCodes][com.davils.knot.shutdown.exit.ExitCodes].
 * @since 1.0.0
 */
public class ShutdownReport(
    public val reason: ShutdownReason,
    public val startedAt: Instant,
    public val elapsed: Duration,
    phases: List<PhaseReport>,
    public val exitCode: ExitCode,
) {
    /**
     * The reports of the phases that had tasks, in their order.
     *
     * @since 1.0.0
     */
    public val phases: List<PhaseReport> = phases.immutableList()

    /**
     * The reports of every task, phase by phase.
     *
     * @since 1.0.0
     */
    public val tasks: List<TaskReport> get() = phases.flatMap { phase -> phase.tasks }

    /**
     * Whether every task completed.
     *
     * @since 1.0.0
     */
    public val isClean: Boolean get() = phases.all { phase -> phase.isClean }

    private val facts: List<Any> get() = listOf(reason, startedAt, elapsed, phases, exitCode)

    /**
     * Tells whether [other] reports the same shutdown.
     *
     * @param other the object to compare with.
     * @return `true` if [other] is equal.
     * @since 1.0.0
     */
    override fun equals(other: Any?): Boolean = other is ShutdownReport && facts == other.facts

    /**
     * Returns a hash code consistent with [equals].
     *
     * @return the hash code.
     * @since 1.0.0
     */
    override fun hashCode(): Int = facts.hashCode()

    /**
     * Returns the reason, the duration, the exit code and the phases.
     *
     * @return the report.
     * @since 1.0.0
     */
    override fun toString(): String = "ShutdownReport($reason in $elapsed, exitCode=$exitCode, phases=$phases)"
}
