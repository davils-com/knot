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

package com.davils.knot.shutdown.task

import com.davils.knot.shutdown.ShutdownLimits
import com.davils.knot.shutdown.ShutdownPhase
import com.davils.knot.shutdown.ShutdownViolationCodes
import com.davils.knot.shutdown.isValidName
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.collection.immutableSet
import com.davils.kore.core.validation.Validatable
import com.davils.kore.core.validation.ValidationScope
import kotlin.time.Duration

private const val DEFAULT_TIMEOUT: String = "default"
private const val NAME_MESSAGE: String = "must start with an ASCII letter or digit, hold only letters, digits, " +
    "'.', '_', ':' and '-' and be at most ${ShutdownLimits.MAX_NAME_LENGTH} characters long"
private const val TIMEOUT_MESSAGE: String = "must be above zero and finite"
private const val ACTION_MESSAGE: String = "declares no action"
private const val DEPENDENCIES_MESSAGE: String = "must not wait on more than ${ShutdownLimits.MAX_DEPENDENCIES} tasks"

private val noAction: suspend ShutdownContext.() -> Unit = {}

/**
 * The declaration of one task of a shutdown.
 *
 * @since 1.0.0
 */
public class ShutdownTaskConfiguration internal constructor(builder: ShutdownTaskBuilder) : Validatable {
    /**
     * The name of the task.
     *
     * @since 1.0.0
     */
    public val name: ShutdownTaskName = builder.name

    /**
     * The phase it runs in.
     *
     * @since 1.0.0
     */
    public val phase: ShutdownPhase = builder.phase

    /**
     * How long its action may run, or `null` for the task timeout of the shutdown.
     *
     * @since 1.0.0
     */
    public val timeout: Duration? = builder.timeout

    /**
     * The tasks of the same phase it waits for.
     *
     * @since 1.0.0
     */
    public val after: Set<ShutdownTaskName> = builder.dependencies.immutableSet()

    internal val hasAction: Boolean = builder.body != null
    internal val action: suspend ShutdownContext.() -> Unit = builder.body ?: noAction

    /**
     * Checks the name, the timeout, the dependencies and the action of the task.
     *
     * @since 1.0.0
     */
    override fun ValidationScope.validate() {
        if (!isValidName(name.value)) fail(::name, NAME_MESSAGE, ShutdownViolationCodes.INVALID_TASK_NAME)
        if (!isValidTimeout(timeout)) fail(::timeout, TIMEOUT_MESSAGE, ShutdownViolationCodes.INVALID_TIMEOUT)
        if (after.size > ShutdownLimits.MAX_DEPENDENCIES) {
            fail(::after, DEPENDENCIES_MESSAGE, ShutdownViolationCodes.TOO_MANY_DEPENDENCIES)
        }
        if (!hasAction) fail(ACTION_MESSAGE, ShutdownViolationCodes.MISSING_ACTION)
    }

    /**
     * Returns the name, the phase, the dependencies and the timeout of the task.
     *
     * @return the task, such as `ShutdownTaskConfiguration(database in Release, after=[pool], timeout=default)`.
     * @since 1.0.0
     */
    override fun toString(): String =
        "ShutdownTaskConfiguration($name in $phase, after=$after, timeout=${timeout ?: DEFAULT_TIMEOUT})"
}

private fun isValidTimeout(timeout: Duration?): Boolean {
    if (timeout == null) return true
    return timeout.isPositive() && timeout.isFinite()
}
