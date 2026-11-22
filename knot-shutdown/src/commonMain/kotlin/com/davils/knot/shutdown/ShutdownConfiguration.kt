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

import com.davils.knot.shutdown.exit.ExitCodes
import com.davils.knot.shutdown.internal.checkGraph
import com.davils.knot.shutdown.task.ShutdownTaskConfiguration
import com.davils.kollect.core.MeterRegistry
import com.davils.kollect.core.trace.Tracer
import com.davils.konfetti.core.EventBus
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.collection.immutableList
import com.davils.kore.core.text.SafeText
import com.davils.kore.core.validation.Validatable
import com.davils.kore.core.validation.ValidationScope
import com.davils.kore.core.validation.violation.ViolationCode
import kotlin.reflect.KProperty
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.TimeSource

private const val NAME_MESSAGE: String = "must start with an ASCII letter or digit, hold only letters, digits, " +
    "'.', '_', ':' and '-' and be at most ${ShutdownLimits.MAX_NAME_LENGTH} characters long"
private const val SPAN_MESSAGE: String = "must be above zero and finite"
private const val GRACE_MESSAGE: String = "must be above zero and at most one hour"
private const val CAPACITY_MESSAGE: String = "must lie between 1 and ${ShutdownLimits.MAX_EVENT_CAPACITY}"
private const val TOO_MANY_TASKS_MESSAGE: String = "must not declare more than ${ShutdownLimits.MAX_TASKS} tasks"
private const val EXCEEDS_GRACE_MESSAGE: String = "is longer than the grace period, so tasks are cut short"

/**
 * The declaration of a shutdown, as [shutdown] built and checked it.
 *
 * @since 1.0.0
 */
public class ShutdownConfiguration internal constructor(builder: ShutdownBuilder) : Validatable {
    /**
     * See [ShutdownBuilder.name].
     *
     * @since 1.0.0
     */
    public val name: String = builder.name

    /**
     * See [ShutdownBuilder.gracePeriod].
     *
     * @since 1.0.0
     */
    public val gracePeriod: Duration = builder.gracePeriod

    /**
     * See [ShutdownBuilder.taskTimeout].
     *
     * @since 1.0.0
     */
    public val taskTimeout: Duration = builder.taskTimeout

    /**
     * See [ShutdownBuilder.cancellationGrace].
     *
     * @since 1.0.0
     */
    public val cancellationGrace: Duration = builder.cancellationGrace

    /**
     * See [ShutdownBuilder.exitCodes].
     *
     * @since 1.0.0
     */
    public val exitCodes: ExitCodes = builder.exitCodes

    /**
     * See [ShutdownBuilder.events].
     *
     * @since 1.0.0
     */
    public val events: EventBus? = builder.events

    /**
     * See [ShutdownBuilder.meters].
     *
     * @since 1.0.0
     */
    public val meters: MeterRegistry? = builder.meters

    /**
     * See [ShutdownBuilder.tracer].
     *
     * @since 1.0.0
     */
    public val tracer: Tracer = builder.tracer

    /**
     * See [ShutdownBuilder.clock].
     *
     * @since 1.0.0
     */
    public val clock: Clock = builder.clock

    /**
     * See [ShutdownBuilder.timeSource].
     *
     * @since 1.0.0
     */
    public val timeSource: TimeSource.WithComparableMarks = builder.timeSource

    /**
     * See [ShutdownBuilder.eventCapacity].
     *
     * @since 1.0.0
     */
    public val eventCapacity: Int = builder.eventCapacity

    /**
     * The tasks declared with the shutdown, in the order of their declaration.
     *
     * @since 1.0.0
     */
    public val tasks: List<ShutdownTaskConfiguration> = builder.tasks.values.immutableList()

    private val hasTooManyTasks: Boolean = builder.hasTooManyTasks

    /**
     * Checks the name, the budgets, the capacity and the graph of the tasks.
     *
     * @since 1.0.0
     */
    override fun ValidationScope.validate() {
        if (!isValidName(name)) fail(::name, NAME_MESSAGE, ShutdownViolationCodes.INVALID_NAME)
        checkGrace()
        checkSpan(::taskTimeout, taskTimeout, ShutdownViolationCodes.INVALID_TIMEOUT)
        checkSpan(::cancellationGrace, cancellationGrace, ShutdownViolationCodes.INVALID_CANCELLATION_GRACE)
        if (eventCapacity !in 1..ShutdownLimits.MAX_EVENT_CAPACITY) {
            fail(::eventCapacity, CAPACITY_MESSAGE, ShutdownViolationCodes.INVALID_EVENT_CAPACITY)
        }
        if (hasTooManyTasks) fail(::tasks, TOO_MANY_TASKS_MESSAGE, ShutdownViolationCodes.TOO_MANY_TASKS)
        checkBudgets()
        checkGraph(registered = emptyList(), declared = tasks, property = ::tasks)
    }

    /**
     * Returns the name, the budgets and the number of tasks, the name escaped.
     *
     * @return the configuration.
     * @since 1.0.0
     */
    override fun toString(): String =
        "ShutdownConfiguration(name=${SafeText.quoted(name)}, gracePeriod=$gracePeriod, taskTimeout=$taskTimeout, " +
            "tasks=${tasks.size})"

    private fun ValidationScope.checkGrace() {
        val isValid = gracePeriod.isPositive() && gracePeriod <= ShutdownLimits.MAX_GRACE_PERIOD
        if (!isValid) fail(::gracePeriod, GRACE_MESSAGE, ShutdownViolationCodes.INVALID_GRACE_PERIOD)
    }

    private fun ValidationScope.checkSpan(property: KProperty<*>, span: Duration, code: ViolationCode) {
        if (span.isPositive() && span.isFinite()) return
        fail(property, SPAN_MESSAGE, code)
    }

    private fun ValidationScope.checkBudgets() {
        if (taskTimeout > gracePeriod) {
            warn(::taskTimeout, EXCEEDS_GRACE_MESSAGE, ShutdownViolationCodes.TIMEOUT_EXCEEDS_GRACE)
        }
        val exceeding = tasks.filter { task -> task.timeout.orZero() > gracePeriod }
        each(::tasks, exceeding.associate { task -> task.name.value to task }) {
            warn(EXCEEDS_GRACE_MESSAGE, ShutdownViolationCodes.TIMEOUT_EXCEEDS_GRACE)
        }
    }
}

private fun Duration?.orZero(): Duration = this ?: Duration.ZERO
