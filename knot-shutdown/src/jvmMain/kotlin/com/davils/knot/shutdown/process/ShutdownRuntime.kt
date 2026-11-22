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

package com.davils.knot.shutdown.process

import com.davils.knot.shutdown.exit.ExitCode
import kotlin.time.Duration

/**
 * The parts of the JVM a [ProcessInstallation] binds a shutdown to: the shutdown hooks, the default handler of
 * uncaught exceptions, a timer for the watchdog and the end of the process.
 *
 * [Jvm] is the real JVM; a test hands in a runtime that records instead, such as `RecordingShutdownRuntime` of
 * `knot-testing`. An implementation is safe to call from every thread.
 *
 * @since 1.0.0
 */
public interface ShutdownRuntime {
    /**
     * Adds a hook that runs [action] when the JVM shuts down.
     *
     * @param name the name of the thread of the hook.
     * @param action what the hook runs.
     * @return a handle whose `close` removes the hook again.
     * @since 1.0.0
     */
    public fun addHook(name: String, action: () -> Unit): AutoCloseable

    /**
     * Returns the default handler of uncaught exceptions.
     *
     * @return the handler, or `null` if there is none.
     * @since 1.0.0
     */
    public fun uncaughtHandler(): Thread.UncaughtExceptionHandler?

    /**
     * Sets the default handler of uncaught exceptions.
     *
     * @param handler the handler, or `null` for none.
     * @since 1.0.0
     */
    public fun installUncaughtHandler(handler: Thread.UncaughtExceptionHandler?)

    /**
     * Reports an exception that no handler took, as the JVM does without a default handler.
     *
     * @param thread the thread it ended.
     * @param cause the exception.
     * @since 1.0.0
     */
    public fun reportUncaught(thread: Thread, cause: Throwable)

    /**
     * Runs [action] once after [after], on a thread of its own.
     *
     * @param name the name of the thread.
     * @param after the delay.
     * @param action what runs.
     * @return a handle whose `close` cancels the action if it has not run.
     * @since 1.0.0
     */
    public fun schedule(name: String, after: Duration, action: () -> Unit): AutoCloseable

    /**
     * Ends the process with [code], running the shutdown hooks.
     *
     * @param code the exit code.
     * @return never.
     * @since 1.0.0
     */
    public fun exit(code: ExitCode): Nothing

    /**
     * Ends the process with [code] at once, without the shutdown hooks.
     *
     * @param code the exit code.
     * @return never.
     * @since 1.0.0
     */
    public fun halt(code: ExitCode): Nothing

    /**
     * The runtime of the real JVM.
     *
     * @since 1.0.0
     */
    public companion object {
        /**
         * The real JVM: `Runtime.addShutdownHook`, `Thread.setDefaultUncaughtExceptionHandler`, a daemon
         * `java.util.Timer` per scheduled action, `System.exit` and `Runtime.halt`.
         *
         * @since 1.0.0
         */
        public val Jvm: ShutdownRuntime = JvmShutdownRuntime
    }
}
