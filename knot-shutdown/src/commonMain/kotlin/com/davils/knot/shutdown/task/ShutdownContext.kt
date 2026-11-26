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

package com.davils.knot.shutdown.task

import com.davils.knot.shutdown.ShutdownPhase
import com.davils.knot.shutdown.ShutdownReason
import com.davils.kore.core.budget.Deadline
import kotlin.time.Duration

/**
 * What the action of a task knows about the shutdown it runs in.
 *
 * @property reason why the shutdown runs.
 * @property phase the phase of the task.
 * @property task the name of the task.
 * @since 1.0.0
 */
public class ShutdownContext internal constructor(
    public val reason: ShutdownReason,
    public val phase: ShutdownPhase,
    public val task: ShutdownTaskName,
    private val deadline: Deadline,
) {
    /**
     * The time left of the budget of the task, never negative; an action that waits uses it as its own timeout.
     *
     * @since 1.0.0
     */
    public val remaining: Duration get() = deadline.remaining

    /**
     * Returns the task, its phase and the reason.
     *
     * @return the context, such as `ShutdownContext(database in Release, reason=ProcessExit)`.
     * @since 1.0.0
     */
    override fun toString(): String = "ShutdownContext($task in $phase, reason=$reason)"
}
