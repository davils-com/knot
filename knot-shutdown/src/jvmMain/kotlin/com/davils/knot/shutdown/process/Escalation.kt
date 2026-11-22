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
import kotlin.time.Duration.Companion.seconds

/**
 * What a [ProcessInstallation] does when a shutdown overruns its grace period.
 *
 * @since 1.0.0
 */
public sealed interface Escalation {
    /**
     * Waits for the shutdown however long it takes.
     *
     * @since 1.0.0
     */
    public object None : Escalation {
        /**
         * Returns the name of the escalation.
         *
         * @return `None`.
         * @since 1.0.0
         */
        override fun toString(): String = "None"
    }

    /**
     * Halts the process with [exitCode] when the shutdown has not ended [after] its grace period; the default.
     *
     * @property after how long after the grace period the watchdog halts; 10 seconds by default, above zero and
     * finite.
     * @property exitCode the exit code of the halt; [ExitCode.TIMED_OUT] by default.
     * @since 1.0.0
     */
    public class Halt(public val after: Duration = DEFAULT_AFTER, public val exitCode: ExitCode = ExitCode.TIMED_OUT) :
        Escalation {
        /**
         * Tells whether [other] halts after the same span with the same code.
         *
         * @param other the object to compare with.
         * @return `true` if [other] is equal.
         * @since 1.0.0
         */
        override fun equals(other: Any?): Boolean = other is Halt && after == other.after && exitCode == other.exitCode

        /**
         * Returns a hash code consistent with [equals].
         *
         * @return the hash code.
         * @since 1.0.0
         */
        override fun hashCode(): Int = listOf(after, exitCode).hashCode()

        /**
         * Returns the escalation.
         *
         * @return the escalation, such as `Halt(after=10s, exitCode=124)`.
         * @since 1.0.0
         */
        override fun toString(): String = "Halt(after=$after, exitCode=$exitCode)"

        private companion object {
            val DEFAULT_AFTER: Duration = 10.seconds
        }
    }
}
