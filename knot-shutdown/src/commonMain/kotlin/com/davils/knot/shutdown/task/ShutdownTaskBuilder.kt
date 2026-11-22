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
import com.davils.kore.core.annotation.DavilsDsl
import com.davils.kore.core.dsl.ValidatingBuilder
import kotlin.time.Duration

private const val SUBJECT: String = "shutdown task"

/**
 * Declares one task of a shutdown, for [PhaseBuilder.task].
 *
 * @property phase the phase the task runs in.
 * @property name the name of the task.
 * @since 1.0.0
 */
@DavilsDsl
public class ShutdownTaskBuilder internal constructor(
    public val phase: ShutdownPhase,
    public val name: ShutdownTaskName,
) : ValidatingBuilder<ShutdownTaskConfiguration>() {
    internal val dependencies: MutableSet<ShutdownTaskName> = linkedSetOf()
    internal var body: (suspend ShutdownContext.() -> Unit)? = null

    override val subject: String get() = SUBJECT

    /**
     * How long the action may run before it is cancelled, or `null` for the task timeout of the shutdown; above zero.
     *
     * @since 1.0.0
     */
    public var timeout: Duration? = null

    /**
     * Runs the task only after the tasks [tasks] of the same phase have ended, however they ended. A task of an earlier
     * phase has always ended; one of a later phase is refused.
     *
     * @param tasks the names of the tasks to wait for.
     * @since 1.0.0
     */
    public fun after(vararg tasks: String) {
        dependencies += tasks.map(::ShutdownTaskName)
    }

    /**
     * Sets what the task does. The action runs once, on the scope of the shutdown, and is cancelled when its budget
     * is over; a failure of it is reported and never stops the shutdown.
     *
     * @param block the action.
     * @since 1.0.0
     */
    public fun action(block: suspend ShutdownContext.() -> Unit) {
        body = block
    }

    override fun assemble(): ShutdownTaskConfiguration = ShutdownTaskConfiguration(this)
}
