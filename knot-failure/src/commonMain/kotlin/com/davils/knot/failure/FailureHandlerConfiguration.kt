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

package com.davils.knot.failure

import com.davils.knot.failure.classify.FailureClassification
import com.davils.knot.failure.sink.SinkConfiguration
import com.davils.knot.failure.storm.FailureStormBuilder
import com.davils.knot.failure.storm.FailureStormConfiguration
import com.davils.knot.shutdown.ShutdownReason
import com.davils.kollect.core.MeterRegistry
import com.davils.kollect.core.trace.Tracer
import com.davils.konfetti.core.EventBus
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.collection.immutableList
import com.davils.kore.core.identity.Uuid
import com.davils.kore.core.identity.generator.UuidGenerator
import com.davils.kore.core.validation.Validatable
import com.davils.kore.core.validation.ValidationScope
import kotlin.time.Clock

private const val CAPACITY_MESSAGE: String = "must lie between 1 and ${FailureLimits.MAX_EVENT_CAPACITY}"
private const val DEGRADED_MESSAGE: String = "must be at least 1"
private const val TOO_MANY_SINKS_MESSAGE: String = "must not declare more than ${FailureLimits.MAX_SINKS} sinks"

/**
 * The declaration of a failure handler, as [failures] built and checked it.
 *
 * @since 1.0.0
 */
public class FailureHandlerConfiguration internal constructor(builder: FailureHandlerBuilder) : Validatable {
    /**
     * See [FailureHandlerBuilder.events].
     *
     * @since 1.0.0
     */
    public val events: EventBus? = builder.events

    /**
     * See [FailureHandlerBuilder.meters].
     *
     * @since 1.0.0
     */
    public val meters: MeterRegistry? = builder.meters

    /**
     * See [FailureHandlerBuilder.tracer].
     *
     * @since 1.0.0
     */
    public val tracer: Tracer = builder.tracer

    /**
     * See [FailureHandlerBuilder.clock].
     *
     * @since 1.0.0
     */
    public val clock: Clock = builder.clock

    /**
     * See [FailureHandlerBuilder.ids].
     *
     * @since 1.0.0
     */
    public val ids: UuidGenerator<Uuid> = builder.ids

    /**
     * See [FailureHandlerBuilder.degradedAt].
     *
     * @since 1.0.0
     */
    public val degradedAt: Int = builder.degradedAt

    /**
     * See [FailureHandlerBuilder.eventCapacity].
     *
     * @since 1.0.0
     */
    public val eventCapacity: Int = builder.eventCapacity

    /**
     * The classification of failures.
     *
     * @since 1.0.0
     */
    public val classification: FailureClassification = builder.classification

    /**
     * The storm protection.
     *
     * @since 1.0.0
     */
    public val storm: FailureStormConfiguration = builder.storm ?: FailureStormBuilder().build()

    /**
     * The sinks, in the order of their declaration.
     *
     * @since 1.0.0
     */
    public val sinks: List<SinkConfiguration> = builder.sinks.values.immutableList()

    internal val fatalAction: (suspend (ShutdownReason.FatalError) -> Unit)? = builder.fatalAction
    private val hasTooManySinks: Boolean = builder.hasTooManySinks

    /**
     * Checks the capacity, the threshold of degradation and the number of sinks.
     *
     * @since 1.0.0
     */
    override fun ValidationScope.validate() {
        if (eventCapacity !in 1..FailureLimits.MAX_EVENT_CAPACITY) {
            fail(::eventCapacity, CAPACITY_MESSAGE, FailureViolationCodes.INVALID_LIMIT)
        }
        if (degradedAt < 1) fail(::degradedAt, DEGRADED_MESSAGE, FailureViolationCodes.INVALID_LIMIT)
        if (hasTooManySinks) fail(::sinks, TOO_MANY_SINKS_MESSAGE, FailureViolationCodes.TOO_MANY_SINKS)
    }

    /**
     * Returns the storm protection and the sinks.
     *
     * @return the configuration.
     * @since 1.0.0
     */
    override fun toString(): String =
        "FailureHandlerConfiguration(storm=$storm, sinks=${sinks.map { sink -> sink.name }}, $classification)"
}
