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

@file:OptIn(ExperimentalAtomicApi::class, DavilsInternal::class)

package com.davils.knot.shutdown.internal

import com.davils.knot.shutdown.ShutdownConfiguration
import com.davils.knot.shutdown.ShutdownPhase
import com.davils.knot.shutdown.ShutdownReason
import com.davils.knot.shutdown.ShutdownState
import com.davils.knot.shutdown.report.ShutdownReport
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.concurrency.Guard
import com.davils.kore.core.concurrency.guard
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.ExperimentalAtomicApi

private const val COORDINATOR: String = "knot-shutdown"
private const val WORKERS: String = "knot-shutdown-task"

internal class ShutdownCoordinator(
    private val configuration: ShutdownConfiguration,
    private val registry: TaskRegistry,
    private val telemetry: ShutdownTelemetry,
    private val scope: CoroutineScope,
) {
    private val mutableState = MutableStateFlow<ShutdownState>(ShutdownState.Running)
    private val result = CompletableDeferred<ShutdownReport>()
    private val started = AtomicBoolean(false)
    private val guard: Guard = guard()
    private val observers: MutableList<(ShutdownReason) -> AutoCloseable?> = mutableListOf()

    val state: StateFlow<ShutdownState> = mutableState.asStateFlow()

    val isStarted: Boolean get() = started.load()

    suspend fun initiate(reason: ShutdownReason): ShutdownReport {
        start(reason)
        return result.await()
    }

    suspend fun awaitTermination(): ShutdownReport = result.await()

    fun whenStarted(observer: (ShutdownReason) -> AutoCloseable?) {
        guard.locked { observers += observer }
    }

    private fun start(reason: ShutdownReason) {
        if (!started.compareAndSet(expectedValue = false, newValue = true)) return
        val tasks = registry.seal()
        mutableState.value = ShutdownState.ShuttingDown(reason, ShutdownPhase.entries.first())
        val listening = guard.locked { observers.toList() }
        val resources = RunResources(parent = scope.coroutineContext[Job], armed = listening.mapNotNull { it(reason) })
        val coordinator = CoroutineScope(scope.coroutineContext + resources.coordinator + CoroutineName(COORDINATOR))
        val workers = CoroutineScope(scope.coroutineContext + resources.workers + CoroutineName(WORKERS))
        val sequence = ShutdownSequence(configuration, telemetry, workers) { running, phase ->
            mutableState.value = ShutdownState.ShuttingDown(running, phase)
        }
        val running = coordinator.async { finished(sequence.run(reason, tasks), resources) }
        running.invokeOnCompletion { cause -> failed(cause, resources) }
    }

    private fun finished(report: ShutdownReport, resources: RunResources): ShutdownReport {
        resources.release()
        mutableState.value = ShutdownState.Terminated(report)
        result.complete(report)
        return report
    }

    private fun failed(cause: Throwable?, resources: RunResources) {
        val failure = cause ?: return
        resources.release()
        result.completeExceptionally(failure)
    }
}
