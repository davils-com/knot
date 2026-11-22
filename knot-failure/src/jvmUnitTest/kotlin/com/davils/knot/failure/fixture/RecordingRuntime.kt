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

package com.davils.knot.failure.fixture

import com.davils.knot.shutdown.exit.ExitCode
import com.davils.knot.shutdown.process.ShutdownRuntime
import kotlin.time.Duration

class RecordingRuntime : ShutdownRuntime {
    private val hooks: MutableMap<String, () -> Unit> = linkedMapOf()
    private val timers: MutableMap<String, Pair<Duration, () -> Unit>> = linkedMapOf()
    private var handler: Thread.UncaughtExceptionHandler? = null

    val hookNames: Set<String> get() = hooks.keys.toSet()
    val timerDelays: List<Duration> get() = timers.values.map { (delay, _) -> delay }
    val reported: MutableList<Throwable> = mutableListOf()
    val ends: MutableList<ProcessEnd> = mutableListOf()

    override fun addHook(name: String, action: () -> Unit): AutoCloseable {
        hooks[name] = action
        return AutoCloseable { hooks -= name }
    }

    override fun uncaughtHandler(): Thread.UncaughtExceptionHandler? = handler

    override fun installUncaughtHandler(handler: Thread.UncaughtExceptionHandler?) {
        this.handler = handler
    }

    override fun reportUncaught(thread: Thread, cause: Throwable) {
        reported += cause
    }

    override fun schedule(name: String, after: Duration, action: () -> Unit): AutoCloseable {
        timers[name] = after to action
        return AutoCloseable { timers -= name }
    }

    override fun exit(code: ExitCode): Nothing = end(ProcessEnd(code, isHalt = false))

    override fun halt(code: ExitCode): Nothing = end(ProcessEnd(code, isHalt = true))

    fun runHooks() {
        hooks.values.toList().forEach { hook -> hook() }
    }

    fun fireTimers() {
        timers.values.toList().forEach { (_, action) -> action() }
    }

    fun crash(cause: Throwable) {
        val thread = Thread.currentThread()
        val current = handler
        if (current == null) return reportUncaught(thread, cause)
        current.uncaughtException(thread, cause)
    }

    private fun end(signal: ProcessEnd): Nothing {
        ends += signal
        throw signal
    }
}
