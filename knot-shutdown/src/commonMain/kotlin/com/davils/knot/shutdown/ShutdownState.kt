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

import com.davils.knot.shutdown.report.ShutdownReport

/**
 * The state of a [Shutdown]: running, shutting down in a phase, or terminated with a report.
 *
 * @since 1.0.0
 */
public sealed interface ShutdownState {
    /**
     * The shutdown has not started.
     *
     * @since 1.0.0
     */
    public object Running : ShutdownState {
        /**
         * Returns the name of the state.
         *
         * @return `Running`.
         * @since 1.0.0
         */
        override fun toString(): String = "Running"
    }

    /**
     * The shutdown runs the tasks of [phase].
     *
     * @property reason why the shutdown runs.
     * @property phase the phase it runs.
     * @since 1.0.0
     */
    public class ShuttingDown(public val reason: ShutdownReason, public val phase: ShutdownPhase) : ShutdownState {
        /**
         * Tells whether [other] is the same phase for the same reason.
         *
         * @param other the object to compare with.
         * @return `true` if [other] is equal.
         * @since 1.0.0
         */
        override fun equals(other: Any?): Boolean =
            other is ShuttingDown && reason == other.reason && phase == other.phase

        /**
         * Returns a hash code consistent with [equals].
         *
         * @return the hash code.
         * @since 1.0.0
         */
        override fun hashCode(): Int = listOf(reason, phase).hashCode()

        /**
         * Returns the phase and the reason.
         *
         * @return the state, such as `ShuttingDown(Drain, reason=ProcessExit)`.
         * @since 1.0.0
         */
        override fun toString(): String = "ShuttingDown($phase, reason=$reason)"
    }

    /**
     * The shutdown ended.
     *
     * @property report how it ended.
     * @since 1.0.0
     */
    public class Terminated(public val report: ShutdownReport) : ShutdownState {
        /**
         * Tells whether [other] ended with the same report.
         *
         * @param other the object to compare with.
         * @return `true` if [other] is equal.
         * @since 1.0.0
         */
        override fun equals(other: Any?): Boolean = other is Terminated && report == other.report

        /**
         * Returns a hash code consistent with [equals].
         *
         * @return the hash code.
         * @since 1.0.0
         */
        override fun hashCode(): Int = report.hashCode()

        /**
         * Returns the exit code of the report.
         *
         * @return the state, such as `Terminated(exitCode=0)`.
         * @since 1.0.0
         */
        override fun toString(): String = "Terminated(exitCode=${report.exitCode})"
    }
}
