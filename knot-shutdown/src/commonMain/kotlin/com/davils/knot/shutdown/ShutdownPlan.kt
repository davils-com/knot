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

package com.davils.knot.shutdown

import com.davils.knot.shutdown.internal.GraphNode
import com.davils.knot.shutdown.internal.checkGraph
import com.davils.knot.shutdown.task.ShutdownTaskConfiguration
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.collection.immutableList
import com.davils.kore.core.validation.Validatable
import com.davils.kore.core.validation.ValidationScope

private const val EMPTY_MESSAGE: String = "declares no task"
private const val TOO_MANY_TASKS_MESSAGE: String =
    "would let the shutdown hold more than ${ShutdownLimits.MAX_TASKS} tasks"

/**
 * The tasks of one registration, as [Shutdown.register] declares them.
 *
 * @since 1.0.0
 */
public class ShutdownPlan internal constructor(builder: ShutdownRegistrationBuilder) : Validatable {
    /**
     * The declared tasks, in the order of their declaration.
     *
     * @since 1.0.0
     */
    public val tasks: List<ShutdownTaskConfiguration> = builder.tasks.values.immutableList()

    private val registered: Collection<GraphNode> = builder.registered()
    private val hasTooManyTasks: Boolean = builder.hasTooManyTasks

    /**
     * Checks the tasks against each other and against the tasks the shutdown holds already.
     *
     * @since 1.0.0
     */
    override fun ValidationScope.validate() {
        if (tasks.isEmpty() && !hasTooManyTasks) fail(EMPTY_MESSAGE, ShutdownViolationCodes.EMPTY_REGISTRATION)
        val isTooMany = hasTooManyTasks || registered.size + tasks.size > ShutdownLimits.MAX_TASKS
        if (isTooMany) fail(::tasks, TOO_MANY_TASKS_MESSAGE, ShutdownViolationCodes.TOO_MANY_TASKS)
        checkGraph(registered = registered, declared = tasks, property = ::tasks)
    }

    /**
     * Returns the number of tasks.
     *
     * @return the plan.
     * @since 1.0.0
     */
    override fun toString(): String = "ShutdownPlan(tasks=${tasks.size})"
}
