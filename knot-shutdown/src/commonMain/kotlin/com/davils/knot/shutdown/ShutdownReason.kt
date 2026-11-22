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

import com.davils.knot.shutdown.exit.ExitCode
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.text.SafeText

/**
 * Why a shutdown runs.
 *
 * @since 1.0.0
 */
public sealed interface ShutdownReason {
    /**
     * The application asked for the shutdown.
     *
     * @property source who asked, such as `admin-endpoint`, shown escaped; `null` if not told.
     * @property exitCode the exit code of a clean shutdown, or `null` for the one of
     * [ExitCodes][com.davils.knot.shutdown.exit.ExitCodes].
     * @since 1.0.0
     */
    public class Requested(public val source: String? = null, public val exitCode: ExitCode? = null) :
        ShutdownReason {
        /**
         * Tells whether [other] is a request from the same source with the same exit code.
         *
         * @param other the object to compare with.
         * @return `true` if [other] is equal.
         * @since 1.0.0
         */
        override fun equals(other: Any?): Boolean =
            other is Requested && source == other.source && exitCode == other.exitCode

        /**
         * Returns a hash code consistent with [equals].
         *
         * @return the hash code.
         * @since 1.0.0
         */
        override fun hashCode(): Int = listOf(source, exitCode).hashCode()

        /**
         * Returns the request, its source escaped.
         *
         * @return the request, such as `Requested(source="admin", exitCode=0)`.
         * @since 1.0.0
         */
        override fun toString(): String {
            val shownSource = source?.let(SafeText::quoted) ?: NONE
            val shownCode = exitCode?.toString() ?: NONE
            return "Requested(source=$shownSource, exitCode=$shownCode)"
        }
    }

    /**
     * The process is ending, such as through `System.exit`, the end of `main` or a signal such as `SIGTERM`, and runs
     * the shutdown from its shutdown hook. Only the JVM installation reports it.
     *
     * @since 1.0.0
     */
    public object ProcessExit : ShutdownReason {
        /**
         * Returns the name of the reason.
         *
         * @return `ProcessExit`.
         * @since 1.0.0
         */
        override fun toString(): String = "ProcessExit"
    }

    /**
     * A fatal error that nothing handled started the shutdown.
     *
     * @property cause the error.
     * @property origin where it happened, such as the name of a thread, shown escaped.
     * @since 1.0.0
     */
    public class FatalError(public val cause: Throwable, public val origin: String) : ShutdownReason {
        /**
         * Tells whether [other] is the same error from the same origin.
         *
         * @param other the object to compare with.
         * @return `true` if [other] is equal.
         * @since 1.0.0
         */
        override fun equals(other: Any?): Boolean =
            other is FatalError && cause == other.cause && origin == other.origin

        /**
         * Returns a hash code consistent with [equals].
         *
         * @return the hash code.
         * @since 1.0.0
         */
        override fun hashCode(): Int = listOf(cause, origin).hashCode()

        /**
         * Returns the type of the error and its origin, never the message of the error, which may hold secrets.
         *
         * @return the reason, such as `FatalError(OutOfMemoryError in "main")`.
         * @since 1.0.0
         */
        override fun toString(): String = "FatalError(${typeNameOf(cause)} in ${SafeText.quoted(origin)})"
    }

    private companion object {
        const val NONE: String = "none"
    }
}
