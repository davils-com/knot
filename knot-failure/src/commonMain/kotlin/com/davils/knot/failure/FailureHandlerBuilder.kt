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

package com.davils.knot.failure

import com.davils.knot.failure.classify.FailureClassification
import com.davils.knot.failure.classify.FailureClassificationBuilder
import com.davils.knot.failure.sink.FailureSink
import com.davils.knot.failure.sink.FailureSinksBuilder
import com.davils.knot.failure.sink.SinkBuilder
import com.davils.knot.failure.sink.SinkConfiguration
import com.davils.knot.failure.storm.FailureStormBuilder
import com.davils.knot.failure.storm.FailureStormConfiguration
import com.davils.knot.shutdown.Shutdown
import com.davils.knot.shutdown.ShutdownReason
import com.davils.kollect.core.MeterRegistry
import com.davils.kollect.core.trace.Tracer
import com.davils.konfetti.core.EventBus
import com.davils.kore.core.annotation.DavilsDsl
import com.davils.kore.core.dsl.ValidatingBuilder
import com.davils.kore.core.identity.Uuid
import com.davils.kore.core.identity.generator.UuidGenerator
import com.davils.kore.core.identity.generator.UuidV7Generator
import kotlin.time.Clock

private const val SUBJECT: String = "failure handler"
private const val DEFAULT_EVENT_CAPACITY: Int = 64
private const val DEFAULT_DEGRADED_AT: Int = 5

/**
 * Declares a failure handler, for [failures]: its classification, its storm protection, its sinks, its telemetry and
 * what a fatal failure does.
 *
 * @since 1.0.0
 */
@DavilsDsl
public class FailureHandlerBuilder internal constructor() : ValidatingBuilder<FailureHandlerConfiguration>() {
    internal val sinks: MutableMap<String, SinkConfiguration> = linkedMapOf()
    internal var hasTooManySinks: Boolean = false
    internal var classification: FailureClassification = FailureClassificationBuilder().build()
    internal var storm: FailureStormConfiguration? = null
    internal var fatalAction: (suspend (ShutdownReason.FatalError) -> Unit)? = null

    override val subject: String get() = SUBJECT

    /**
     * The event bus whose topic [FailureEvent.TOPIC] receives the events, or `null` for none.
     *
     * @since 1.0.0
     */
    public var events: EventBus? = null

    /**
     * The registry that receives the meters of the handler, or `null` for none.
     *
     * @since 1.0.0
     */
    public var meters: MeterRegistry? = null

    /**
     * The tracer that records a span for every reported failure; none by default.
     *
     * @since 1.0.0
     */
    public var tracer: Tracer = Tracer.None

    /**
     * The clock that dates the failures.
     *
     * @since 1.0.0
     */
    public var clock: Clock = Clock.System

    /**
     * Where the ids of the failures come from; time-ordered version 7 UUIDs from secure randomness by default.
     *
     * @since 1.0.0
     */
    public var ids: UuidGenerator<Uuid> = UuidV7Generator

    /**
     * The shutdown that a fatal failure runs, with [ShutdownReason.FatalError], after its report was written; `null`
     * for none. On the JVM, `exitThrough` of a process installation ends the process instead.
     *
     * @since 1.0.0
     */
    public var shutdown: Shutdown? = null
        set(value) {
            field = value
            fatalAction = value?.let { target -> { reason -> target.initiate(reason) } }
        }

    /**
     * How many recent failures make the handler report itself as degraded; 5 by default, at least 1.
     *
     * @since 1.0.0
     */
    public var degradedAt: Int = DEFAULT_DEGRADED_AT

    /**
     * How many failures [FailureHandler.failures] keeps for a slow collector before it drops the oldest; 64 by
     * default, between 1 and [FailureLimits.MAX_EVENT_CAPACITY].
     *
     * @since 1.0.0
     */
    public var eventCapacity: Int = DEFAULT_EVENT_CAPACITY

    /**
     * Declares which failures are fatal and which are ignored.
     *
     * @param block declares the rules.
     * @throws IllegalStateException if the builder was evaluated already.
     * @since 1.0.0
     */
    public fun classify(block: FailureClassificationBuilder.() -> Unit) {
        checkOpen()
        val builder = FailureClassificationBuilder()
        builder.block()
        classification = builder.build()
    }

    /**
     * Declares the storm protection; a window of one minute with 10 reports by default.
     *
     * @param block declares the window and the reports.
     * @throws IllegalStateException if the builder was evaluated already.
     * @since 1.0.0
     */
    public fun storm(block: FailureStormBuilder.() -> Unit) {
        storm = nested(builder = FailureStormBuilder(), property = ::storm, block = block)
    }

    /**
     * Declares the sinks that receive the crash reports; without one, failures are reported through the flow, the
     * events and the meters only.
     *
     * @param block declares the sinks.
     * @throws IllegalStateException if the builder was evaluated already.
     * @since 1.0.0
     */
    public fun sinks(block: FailureSinksBuilder.() -> Unit) {
        checkOpen()
        val builder = FailureSinksBuilder(::declare)
        builder.block()
    }

    override fun assemble(): FailureHandlerConfiguration = FailureHandlerConfiguration(this)

    private fun declare(name: String, sink: FailureSink, block: SinkBuilder.() -> Unit) {
        hasTooManySinks = hasTooManySinks || sinks.size >= FailureLimits.MAX_SINKS
        if (hasTooManySinks) return
        collect(target = sinks, key = name, builder = SinkBuilder(name, sink), property = ::sinks, block = block)
    }
}
