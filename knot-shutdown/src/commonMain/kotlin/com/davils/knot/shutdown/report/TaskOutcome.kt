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

package com.davils.knot.shutdown.report

import com.davils.knot.shutdown.typeNameOf
import kotlin.time.Duration

/**
 * How a task of a shutdown ended.
 *
 * @since 1.0.0
 */
public sealed interface TaskOutcome {
    /**
     * Whether the task completed.
     *
     * @since 1.0.0
     */
    public val isCompleted: Boolean get() = this is Completed

    /**
     * The action returned.
     *
     * @property elapsed how long it ran.
     * @since 1.0.0
     */
    public class Completed(public val elapsed: Duration) : TaskOutcome {
        /**
         * Tells whether [other] completed in the same time.
         *
         * @param other the object to compare with.
         * @return `true` if [other] is equal.
         * @since 1.0.0
         */
        override fun equals(other: Any?): Boolean = other is Completed && elapsed == other.elapsed

        /**
         * Returns a hash code consistent with [equals].
         *
         * @return the hash code.
         * @since 1.0.0
         */
        override fun hashCode(): Int = elapsed.hashCode()

        /**
         * Returns the outcome.
         *
         * @return the outcome, such as `Completed(12ms)`.
         * @since 1.0.0
         */
        override fun toString(): String = "Completed($elapsed)"
    }

    /**
     * The action threw.
     *
     * @property cause what it threw.
     * @property elapsed how long it ran.
     * @since 1.0.0
     */
    public class Failed(public val cause: Throwable, public val elapsed: Duration) : TaskOutcome {
        /**
         * Tells whether [other] failed with the same cause in the same time.
         *
         * @param other the object to compare with.
         * @return `true` if [other] is equal.
         * @since 1.0.0
         */
        override fun equals(other: Any?): Boolean = other is Failed && cause == other.cause && elapsed == other.elapsed

        /**
         * Returns a hash code consistent with [equals].
         *
         * @return the hash code.
         * @since 1.0.0
         */
        override fun hashCode(): Int = listOf(cause, elapsed).hashCode()

        /**
         * Returns the type of the cause, never its message, which may hold secrets.
         *
         * @return the outcome, such as `Failed(IOException after 3ms)`.
         * @since 1.0.0
         */
        override fun toString(): String = "Failed(${typeNameOf(cause)} after $elapsed)"
    }

    /**
     * The action ran out of its budget and ended once it was cancelled.
     *
     * @property timeout the budget it had.
     * @since 1.0.0
     */
    public class TimedOut(public val timeout: Duration) : TaskOutcome {
        /**
         * Tells whether [other] timed out after the same budget.
         *
         * @param other the object to compare with.
         * @return `true` if [other] is equal.
         * @since 1.0.0
         */
        override fun equals(other: Any?): Boolean = other is TimedOut && timeout == other.timeout

        /**
         * Returns a hash code consistent with [equals].
         *
         * @return the hash code.
         * @since 1.0.0
         */
        override fun hashCode(): Int = timeout.hashCode()

        /**
         * Returns the outcome.
         *
         * @return the outcome, such as `TimedOut(5s)`.
         * @since 1.0.0
         */
        override fun toString(): String = "TimedOut($timeout)"
    }

    /**
     * The action ran out of its budget and did not end within the cancellation grace after it was cancelled; it may
     * still run in the background.
     *
     * @property timeout the budget it had.
     * @since 1.0.0
     */
    public class Abandoned(public val timeout: Duration) : TaskOutcome {
        /**
         * Tells whether [other] was abandoned after the same budget.
         *
         * @param other the object to compare with.
         * @return `true` if [other] is equal.
         * @since 1.0.0
         */
        override fun equals(other: Any?): Boolean = other is Abandoned && timeout == other.timeout

        /**
         * Returns a hash code consistent with [equals].
         *
         * @return the hash code.
         * @since 1.0.0
         */
        override fun hashCode(): Int = timeout.hashCode()

        /**
         * Returns the outcome.
         *
         * @return the outcome, such as `Abandoned(5s)`.
         * @since 1.0.0
         */
        override fun toString(): String = "Abandoned($timeout)"
    }

    /**
     * The grace period of the shutdown was over before the task could start, so its action never ran.
     *
     * @since 1.0.0
     */
    public object DeadlineExceeded : TaskOutcome {
        /**
         * Returns the name of the outcome.
         *
         * @return `DeadlineExceeded`.
         * @since 1.0.0
         */
        override fun toString(): String = "DeadlineExceeded"
    }
}
