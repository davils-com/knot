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

import com.davils.knot.shutdown.exit.ExitCodes
import com.davils.kollect.core.MeterRegistry
import com.davils.kollect.core.trace.Tracer
import com.davils.konfetti.core.EventBus
import com.davils.kore.core.annotation.DavilsDsl
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

private const val SUBJECT: String = "shutdown"

/**
 * Declares a shutdown, for [shutdown]: its name, its budgets, its telemetry and its tasks.
 *
 * @since 1.0.0
 */
@DavilsDsl
public class ShutdownBuilder internal constructor() : ShutdownPlanBuilder<ShutdownConfiguration>() {
    override val subject: String get() = SUBJECT

    /**
     * The name of the shutdown, which events, meters and the JVM hook carry; `application` by default. It starts
     * with an ASCII letter or digit, holds letters, digits, `.`, `_`, `:` and `-` and is at most
     * [ShutdownLimits.MAX_NAME_LENGTH] long.
     *
     * @since 1.0.0
     */
    public var name: String = DEFAULT_NAME

    /**
     * How long the whole shutdown may take; a task that would start later never runs. 30 seconds by default, above
     * zero and at most [ShutdownLimits.MAX_GRACE_PERIOD].
     *
     * @since 1.0.0
     */
    public var gracePeriod: Duration = DEFAULT_GRACE_PERIOD

    /**
     * How long a task may run unless it declares its own timeout; 5 seconds by default, above zero and finite.
     *
     * @since 1.0.0
     */
    public var taskTimeout: Duration = DEFAULT_TASK_TIMEOUT

    /**
     * How long a cancelled task is waited for before it is abandoned; 1 second by default, above zero and finite.
     *
     * @since 1.0.0
     */
    public var cancellationGrace: Duration = DEFAULT_CANCELLATION_GRACE

    /**
     * The exit codes the shutdown chooses.
     *
     * @since 1.0.0
     */
    public var exitCodes: ExitCodes = ExitCodes()

    /**
     * The event bus whose topic [ShutdownEvent.TOPIC] receives the events, or `null` for none.
     *
     * @since 1.0.0
     */
    public var events: EventBus? = null

    /**
     * The registry that receives the meters of the shutdown, or `null` for none.
     *
     * @since 1.0.0
     */
    public var meters: MeterRegistry? = null

    /**
     * The tracer that records a span for the shutdown and for each task; none by default.
     *
     * @since 1.0.0
     */
    public var tracer: Tracer = Tracer.None

    /**
     * The clock that dates the report.
     *
     * @since 1.0.0
     */
    public var clock: Clock = Clock.System

    /**
     * The source of the time that budgets and durations are measured with.
     *
     * @since 1.0.0
     */
    public var timeSource: TimeSource.WithComparableMarks = TimeSource.Monotonic

    /**
     * How many events [Shutdown.events] keeps for a slow collector before it drops the oldest; 64 by default, between
     * 1 and [ShutdownLimits.MAX_EVENT_CAPACITY].
     *
     * @since 1.0.0
     */
    public var eventCapacity: Int = DEFAULT_EVENT_CAPACITY

    override fun assemble(): ShutdownConfiguration = ShutdownConfiguration(this)

    /**
     * The defaults of a shutdown.
     *
     * @since 1.0.0
     */
    public companion object {
        /**
         * The default [name].
         *
         * @since 1.0.0
         */
        public const val DEFAULT_NAME: String = "application"

        /**
         * The default [eventCapacity].
         *
         * @since 1.0.0
         */
        public const val DEFAULT_EVENT_CAPACITY: Int = 64

        /**
         * The default [gracePeriod].
         *
         * @since 1.0.0
         */
        public val DEFAULT_GRACE_PERIOD: Duration = 30.seconds

        /**
         * The default [taskTimeout].
         *
         * @since 1.0.0
         */
        public val DEFAULT_TASK_TIMEOUT: Duration = 5.seconds

        /**
         * The default [cancellationGrace].
         *
         * @since 1.0.0
         */
        public val DEFAULT_CANCELLATION_GRACE: Duration = 1.seconds
    }
}
