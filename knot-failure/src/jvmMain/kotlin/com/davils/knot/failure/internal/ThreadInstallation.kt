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

package com.davils.knot.failure.internal

import com.davils.knot.shutdown.process.ShutdownRuntime
import com.davils.kore.core.failure.contained
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.ExperimentalAtomicApi

internal class ThreadInstallation(
    private val runtime: ShutdownRuntime,
    private val handle: (Thread, Throwable) -> Unit,
) {
    fun install(): AutoCloseable {
        val previous = runtime.uncaughtHandler()
        val own = Thread.UncaughtExceptionHandler { thread, cause ->
            contained { handle(thread, cause) }
            delegate(previous, thread, cause)
        }
        runtime.installUncaughtHandler(own)
        val closed = AtomicBoolean(false)
        return AutoCloseable {
            val isFirst = closed.compareAndSet(expectedValue = false, newValue = true)
            if (isFirst && runtime.uncaughtHandler() === own) runtime.installUncaughtHandler(previous)
        }
    }

    private fun delegate(previous: Thread.UncaughtExceptionHandler?, thread: Thread, cause: Throwable) {
        if (previous == null) return runtime.reportUncaught(thread, cause)
        contained { previous.uncaughtException(thread, cause) }
    }
}
