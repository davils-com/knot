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

import com.davils.knot.shutdown.task.PhaseBuilder
import com.davils.knot.shutdown.task.ShutdownTaskBuilder
import com.davils.knot.shutdown.task.ShutdownTaskConfiguration
import com.davils.knot.shutdown.task.ShutdownTaskName
import com.davils.kore.core.annotation.DavilsDsl
import com.davils.kore.core.dsl.ValidatingBuilder
import com.davils.kore.core.validation.Validatable

/**
 * Declares the tasks of a shutdown phase by phase, for [shutdown] and [Shutdown.register].
 *
 * A task name is unique within the shutdown; a name declared twice is reported under `tasks["<name>"]`. At most
 * [ShutdownLimits.MAX_TASKS] tasks are kept; more are reported with [ShutdownViolationCodes.TOO_MANY_TASKS].
 *
 * @param T what the builder builds.
 * @since 1.0.0
 */
@DavilsDsl
public abstract class ShutdownPlanBuilder<T : Validatable> internal constructor() : ValidatingBuilder<T>() {
    internal val tasks: MutableMap<String, ShutdownTaskConfiguration> = linkedMapOf()
    internal var hasTooManyTasks: Boolean = false

    /**
     * Declares tasks of [phase].
     *
     * @param phase the phase of the tasks.
     * @param block declares the tasks.
     * @throws IllegalStateException if the builder was evaluated already.
     * @since 1.0.0
     */
    public fun phase(phase: ShutdownPhase, block: PhaseBuilder.() -> Unit) {
        checkOpen()
        val builder = PhaseBuilder(phase, ::declare)
        builder.block()
    }

    /**
     * Declares tasks of [ShutdownPhase.Announce].
     *
     * @param block declares the tasks.
     * @throws IllegalStateException if the builder was evaluated already.
     * @since 1.0.0
     */
    public fun announce(block: PhaseBuilder.() -> Unit) {
        phase(ShutdownPhase.Announce, block)
    }

    /**
     * Declares tasks of [ShutdownPhase.Drain].
     *
     * @param block declares the tasks.
     * @throws IllegalStateException if the builder was evaluated already.
     * @since 1.0.0
     */
    public fun drain(block: PhaseBuilder.() -> Unit) {
        phase(ShutdownPhase.Drain, block)
    }

    /**
     * Declares tasks of [ShutdownPhase.Stop].
     *
     * @param block declares the tasks.
     * @throws IllegalStateException if the builder was evaluated already.
     * @since 1.0.0
     */
    public fun stop(block: PhaseBuilder.() -> Unit) {
        phase(ShutdownPhase.Stop, block)
    }

    /**
     * Declares tasks of [ShutdownPhase.Release].
     *
     * @param block declares the tasks.
     * @throws IllegalStateException if the builder was evaluated already.
     * @since 1.0.0
     */
    public fun release(block: PhaseBuilder.() -> Unit) {
        phase(ShutdownPhase.Release, block)
    }

    /**
     * Declares tasks of [ShutdownPhase.Flush].
     *
     * @param block declares the tasks.
     * @throws IllegalStateException if the builder was evaluated already.
     * @since 1.0.0
     */
    public fun flush(block: PhaseBuilder.() -> Unit) {
        phase(ShutdownPhase.Flush, block)
    }

    private fun declare(phase: ShutdownPhase, name: String, block: ShutdownTaskBuilder.() -> Unit) {
        checkOpen()
        hasTooManyTasks = hasTooManyTasks || tasks.size >= ShutdownLimits.MAX_TASKS
        if (hasTooManyTasks) return
        val builder = ShutdownTaskBuilder(phase, ShutdownTaskName(name))
        collect(target = tasks, key = name, builder = builder, property = ::tasks, block = block)
    }
}
