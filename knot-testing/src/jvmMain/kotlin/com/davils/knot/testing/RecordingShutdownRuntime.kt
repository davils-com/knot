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

package com.davils.knot.testing

import com.davils.knot.shutdown.exit.ExitCode
import com.davils.knot.shutdown.process.ShutdownRuntime
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.time.Duration

/**
 * A [ShutdownRuntime] that records instead of touching the JVM, for tests of process installations and of handlers of
 * uncaught exceptions.
 *
 * Hooks and timers are kept and run only by [runHooks] and [fireTimers]; [exit] and [halt] record the code and throw
 * [ProcessExit] and [ProcessHalt]. It is safe to use from every thread.
 *
 * @since 1.0.0
 */
public class RecordingShutdownRuntime : ShutdownRuntime {
    private val lock: ReentrantLock = ReentrantLock()
    private val hooks: MutableMap<String, () -> Unit> = linkedMapOf()
    private val timers: MutableMap<String, Pair<Duration, () -> Unit>> = linkedMapOf()
    private val exitCodes: MutableList<ExitCode> = mutableListOf()
    private val haltCodes: MutableList<ExitCode> = mutableListOf()
    private val uncaught: MutableList<Throwable> = mutableListOf()
    private var handler: Thread.UncaughtExceptionHandler? = null

    /**
     * The names of the hooks that are installed.
     *
     * @since 1.0.0
     */
    public val hookNames: Set<String> get() = lock.withLock { hooks.keys.toSet() }

    /**
     * The delays of the timers that are scheduled.
     *
     * @since 1.0.0
     */
    public val timerDelays: List<Duration> get() = lock.withLock { timers.values.map { (delay, _) -> delay } }

    /**
     * The codes of every call of [exit].
     *
     * @since 1.0.0
     */
    public val exits: List<ExitCode> get() = lock.withLock { exitCodes.toList() }

    /**
     * The codes of every call of [halt].
     *
     * @since 1.0.0
     */
    public val halts: List<ExitCode> get() = lock.withLock { haltCodes.toList() }

    /**
     * The exceptions reported because no handler took them.
     *
     * @since 1.0.0
     */
    public val reported: List<Throwable> get() = lock.withLock { uncaught.toList() }

    /**
     * Keeps [action] under [name] until [runHooks] or the returned handle closes.
     *
     * @param name the name of the hook.
     * @param action what the hook runs.
     * @return the handle that removes the hook.
     * @since 1.0.0
     */
    override fun addHook(name: String, action: () -> Unit): AutoCloseable {
        lock.withLock { hooks[name] = action }
        return AutoCloseable { lock.withLock { hooks -= name } }
    }

    /**
     * Returns the handler of uncaught exceptions that is installed.
     *
     * @return the handler, or `null`.
     * @since 1.0.0
     */
    override fun uncaughtHandler(): Thread.UncaughtExceptionHandler? = lock.withLock { handler }

    /**
     * Installs [handler] as the handler of uncaught exceptions.
     *
     * @param handler the handler, or `null` for none.
     * @since 1.0.0
     */
    override fun installUncaughtHandler(handler: Thread.UncaughtExceptionHandler?) {
        lock.withLock { this.handler = handler }
    }

    /**
     * Records [cause] in [reported].
     *
     * @param thread the thread it ended.
     * @param cause the exception.
     * @since 1.0.0
     */
    override fun reportUncaught(thread: Thread, cause: Throwable) {
        lock.withLock { uncaught += cause }
    }

    /**
     * Keeps [action] under [name] until [fireTimers] or the returned handle closes.
     *
     * @param name the name of the timer.
     * @param after the delay, recorded in [timerDelays].
     * @param action what the timer runs.
     * @return the handle that cancels the timer.
     * @since 1.0.0
     */
    override fun schedule(name: String, after: Duration, action: () -> Unit): AutoCloseable {
        lock.withLock { timers[name] = after to action }
        return AutoCloseable { lock.withLock { timers -= name } }
    }

    /**
     * Records [code] in [exits] and throws.
     *
     * @param code the exit code.
     * @return never.
     * @throws ProcessExit always.
     * @since 1.0.0
     */
    override fun exit(code: ExitCode): Nothing {
        lock.withLock { exitCodes += code }
        throw ProcessExit(code)
    }

    /**
     * Records [code] in [halts] and throws.
     *
     * @param code the exit code.
     * @return never.
     * @throws ProcessHalt always.
     * @since 1.0.0
     */
    override fun halt(code: ExitCode): Nothing {
        lock.withLock { haltCodes += code }
        throw ProcessHalt(code)
    }

    /**
     * Runs every hook on the calling thread, as the JVM does when it shuts down.
     *
     * @since 1.0.0
     */
    public fun runHooks() {
        val running = lock.withLock { hooks.values.toList() }
        running.forEach { hook -> hook() }
    }

    /**
     * Runs every timer on the calling thread, as if its delay had passed.
     *
     * @since 1.0.0
     */
    public fun fireTimers() {
        val firing = lock.withLock { timers.values.map { (_, action) -> action } }
        firing.forEach { action -> action() }
    }

    /**
     * Hands [cause] to the installed handler of uncaught exceptions, as the JVM does when [thread] ends with it, or
     * reports it when no handler is installed.
     *
     * @param cause the exception.
     * @param thread the thread it ends; the calling thread by default.
     * @since 1.0.0
     */
    public fun crash(cause: Throwable, thread: Thread = Thread.currentThread()) {
        val current = uncaughtHandler()
        if (current == null) return reportUncaught(thread, cause)
        current.uncaughtException(thread, cause)
    }

    /**
     * Returns what was recorded.
     *
     * @return the runtime.
     * @since 1.0.0
     */
    override fun toString(): String =
        "RecordingShutdownRuntime(hooks=${hookNames.size}, timers=${timerDelays.size}, exits=$exits, halts=$halts)"
}
