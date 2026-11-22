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

@file:OptIn(ExperimentalAtomicApi::class)

package com.davils.knot.shutdown.process

import com.davils.knot.shutdown.Shutdown
import com.davils.knot.shutdown.ShutdownReason
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.ExperimentalAtomicApi

private const val HOOK_PREFIX: String = "knot-shutdown-"

/**
 * A shutdown bound to the JVM by [install]: its shutdown hook, its fatal error trigger and its watchdog.
 *
 * The hook runs the shutdown with [ShutdownReason.ProcessExit] when the JVM shuts down. The trigger runs it with
 * [ShutdownReason.FatalError] and ends the process when an exception that no thread handled is fatal. The watchdog
 * halts the process when the shutdown overruns its grace period by the span of its [Escalation]. An installation is
 * safe to use from every thread.
 *
 * @property shutdown the installed shutdown.
 * @property configuration the declaration of the installation.
 * @since 1.0.0
 */
public class ProcessInstallation internal constructor(
    public val shutdown: Shutdown,
    public val configuration: ProcessInstallationConfiguration,
    private val runtime: ShutdownRuntime,
) {
    private val installed: AtomicBoolean = AtomicBoolean(true)
    private val hook: AutoCloseable? = hookOf()
    private val handler: FatalErrorTrigger? = handlerOf()

    init {
        val watchdog = Watchdog(shutdown, runtime, configuration.escalation)
        shutdown.coordinator.whenStarted { watchdog.arm() }
    }

    /**
     * Whether the installation is still bound to the JVM.
     *
     * @since 1.0.0
     */
    public val isInstalled: Boolean get() = installed.load()

    /**
     * Runs the shutdown, unless it has started, waits for its report and ends the process with its exit code.
     *
     * The wait cannot be cancelled: once called, the process ends. The shutdown hook sees that the shutdown has ended
     * and returns at once.
     *
     * @param reason why the shutdown runs; [ShutdownReason.Requested] by default.
     * @return never.
     * @since 1.0.0
     */
    public suspend fun exit(reason: ShutdownReason = ShutdownReason.Requested()): Nothing = withContext(
        NonCancellable,
    ) {
        val report = shutdown.initiate(reason)
        runtime.exit(report.exitCode)
    }

    /**
     * Unbinds the shutdown from the JVM: removes the hook and gives the handler of uncaught exceptions back, if it is
     * still the one of this installation. The watchdog of a shutdown that started stays armed.
     *
     * @return `true` the first time, before the shutdown started; `false` otherwise.
     * @since 1.0.0
     */
    public fun uninstall(): Boolean {
        val isUninstalled = !shutdown.isStarted && installed.compareAndSet(expectedValue = true, newValue = false)
        if (isUninstalled) release()
        return isUninstalled
    }

    /**
     * Returns the shutdown and the declaration.
     *
     * @return the installation.
     * @since 1.0.0
     */
    override fun toString(): String = "ProcessInstallation($shutdown, $configuration, installed=$isInstalled)"

    private fun release() {
        hook?.close()
        val own = handler
        if (own != null && runtime.uncaughtHandler() === own) runtime.installUncaughtHandler(own.previous)
    }

    private fun hookOf(): AutoCloseable? {
        if (!configuration.hasExitHook) return null
        return runtime.addHook(HOOK_PREFIX + shutdown.configuration.name) {
            runBlocking { shutdown.initiate(ShutdownReason.ProcessExit) }
        }
    }

    private fun handlerOf(): FatalErrorTrigger? {
        val policy = configuration.fatalErrors as? FatalErrorPolicy.Exit ?: return null
        val trigger = FatalErrorTrigger(
            installation = this,
            runtime = runtime,
            policy = policy,
            previous = runtime.uncaughtHandler(),
        )
        runtime.installUncaughtHandler(trigger)
        return trigger
    }
}
