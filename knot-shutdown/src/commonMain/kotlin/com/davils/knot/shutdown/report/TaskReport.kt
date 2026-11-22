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

package com.davils.knot.shutdown.report

import com.davils.knot.shutdown.ShutdownPhase
import com.davils.knot.shutdown.task.ShutdownTaskName

/**
 * How one task of a shutdown ended.
 *
 * @property name the name of the task.
 * @property phase its phase.
 * @property outcome how it ended.
 * @since 1.0.0
 */
public class TaskReport(
    public val name: ShutdownTaskName,
    public val phase: ShutdownPhase,
    public val outcome: TaskOutcome,
) {
    /**
     * Whether the task completed.
     *
     * @since 1.0.0
     */
    public val isCompleted: Boolean get() = outcome.isCompleted

    /**
     * Tells whether [other] reports the same task with the same outcome.
     *
     * @param other the object to compare with.
     * @return `true` if [other] is equal.
     * @since 1.0.0
     */
    override fun equals(other: Any?): Boolean =
        other is TaskReport && name == other.name && phase == other.phase && outcome == other.outcome

    /**
     * Returns a hash code consistent with [equals].
     *
     * @return the hash code.
     * @since 1.0.0
     */
    override fun hashCode(): Int = listOf(name, phase, outcome).hashCode()

    /**
     * Returns the task and its outcome.
     *
     * @return the report, such as `TaskReport(database in Release: Completed(4ms))`.
     * @since 1.0.0
     */
    override fun toString(): String = "TaskReport($name in $phase: $outcome)"
}
