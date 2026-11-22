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

import com.davils.knot.shutdown.internal.ShutdownCoordinator
import com.davils.knot.shutdown.internal.ShutdownTask
import com.davils.knot.shutdown.internal.ShutdownTelemetry
import com.davils.knot.shutdown.internal.TaskRegistry
import com.davils.knot.shutdown.report.ShutdownReport
import com.davils.kollect.core.health.Health
import com.davils.kollect.core.health.HealthIndicator
import com.davils.kollect.core.health.ReasonCode
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.text.SafeText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * The coordinated shutdown of an application: its tasks run phase by phase, within their budgets, exactly once.
 *
 * [initiate] starts the shutdown and waits for its report; any later call waits for the same report. Every task of a
 * phase runs at the same time as the others, after the tasks of the same phase it waits on; a phase starts when the
 * one before it has ended. A task that throws, runs out of its budget or never ends is reported and never stops the
 * shutdown. The shutdown reports its progress through [state], [events], the topic [ShutdownEvent.TOPIC] of its event
 * bus, its meters and its spans; it never logs.
 *
 * A shutdown created by [shutdown] touches nothing outside itself. On the JVM,
 * [install][com.davils.knot.shutdown.process.install] adds the shutdown hook, the fatal error trigger and the watchdog
 * that run and enforce it when the process ends; on the `wasmJs` target, which has no such hooks, the application
 * calls [initiate] itself, for instance when a browser page is hidden or a Node.js process receives a signal.
 *
 * A shutdown is safe to use from every thread and coroutine. It is a [HealthIndicator]: up while it runs, down once it
 * started.
 *
 * @property configuration the declaration of the shutdown.
 * @since 1.0.0
 */
public class Shutdown internal constructor(public val configuration: ShutdownConfiguration, scope: CoroutineScope) :
    HealthIndicator {
    internal val telemetry: ShutdownTelemetry = ShutdownTelemetry(configuration)
    private val registry: TaskRegistry = TaskRegistry(
        initial = configuration.tasks.map { task -> ShutdownTask.of(task, configuration.taskTimeout) },
        defaultTimeout = configuration.taskTimeout,
        refused = telemetry::refused,
    )
    internal val coordinator: ShutdownCoordinator = ShutdownCoordinator(
        configuration = configuration,
        registry = registry,
        telemetry = telemetry,
        scope = scope,
    )

    init {
        telemetry.registered(registry.size)
    }

    /**
     * The state of the shutdown; it changes from [ShutdownState.Running] through one [ShutdownState.ShuttingDown] per
     * phase to [ShutdownState.Terminated].
     *
     * @since 1.0.0
     */
    public val state: StateFlow<ShutdownState> get() = coordinator.state

    /**
     * The events of the shutdown, for collectors in the process; a slow collector loses the oldest events beyond
     * [ShutdownBuilder.eventCapacity].
     *
     * @since 1.0.0
     */
    public val eventFlow: Flow<ShutdownEvent> get() = telemetry.events

    /**
     * Whether the shutdown has started.
     *
     * @since 1.0.0
     */
    public val isStarted: Boolean get() = coordinator.isStarted

    /**
     * Starts the shutdown, unless it has started already, and waits for its report.
     *
     * The shutdown runs on the scope it was created with, not on the caller's coroutine, so cancelling the caller
     * stops the wait but never the shutdown.
     *
     * @param reason why the shutdown runs; [ShutdownReason.Requested] by default. A later call keeps the reason of
     * the first.
     * @return the report of the shutdown.
     * @since 1.0.0
     */
    public suspend fun initiate(reason: ShutdownReason = ShutdownReason.Requested()): ShutdownReport =
        coordinator.initiate(reason)

    /**
     * Waits until the shutdown has ended, without starting it.
     *
     * @return the report of the shutdown.
     * @since 1.0.0
     */
    public suspend fun awaitTermination(): ShutdownReport = coordinator.awaitTermination()

    /**
     * Adds the tasks that [block] declares to the shutdown, such as those of a component that starts later.
     *
     * The tasks may wait on every task the shutdown holds already. A registration is refused once the shutdown has
     * started, and when it breaks a rule, such as a name another task has or a shutdown with more than
     * [ShutdownLimits.MAX_TASKS] tasks.
     *
     * @param block declares the tasks.
     * @return the registration, or the refusal and why.
     * @since 1.0.0
     */
    public fun register(block: ShutdownRegistrationBuilder.() -> Unit): ShutdownRegistration {
        val registration = registry.register(block)
        telemetry.registered(registry.size)
        return registration
    }

    /**
     * Reads the counters of the shutdown.
     *
     * @return a snapshot of the counters.
     * @since 1.0.0
     */
    public fun metrics(): ShutdownMetrics = telemetry.metrics(registry.size)

    /**
     * Tells whether the shutdown has started: up while it has not, down with [ReasonCode.StateMatched] once it has.
     *
     * @return the health of the shutdown.
     * @since 1.0.0
     */
    override suspend fun check(): Health {
        if (isStarted) return Health.down(ReasonCode.StateMatched)
        return Health.Up
    }

    /**
     * Returns the name and the state of the shutdown, the name escaped.
     *
     * @return the shutdown, such as `Shutdown("application", Running)`.
     * @since 1.0.0
     */
    override fun toString(): String = "Shutdown(${SafeText.quoted(configuration.name)}, ${state.value})"
}
