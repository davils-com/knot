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

package com.davils.knot.shutdown.exit

import kotlin.jvm.JvmInline

private const val LOWEST: Int = 0
private const val HIGHEST: Int = 255
private const val SUCCESS_CODE: Int = 0
private const val FAILURE_CODE: Int = 1
private const val SOFTWARE_CODE: Int = 70
private const val TIMED_OUT_CODE: Int = 124

/**
 * The status a process ends with, between 0 and 255 as every operating system keeps it.
 *
 * @property value the status.
 * @throws IllegalArgumentException if [value] lies outside 0 to 255.
 * @since 1.0.0
 */
@JvmInline
public value class ExitCode(public val value: Int) {
    init {
        require(value in LOWEST..HIGHEST) { "An exit code lies between $LOWEST and $HIGHEST, but was $value" }
    }

    /**
     * Returns the [value] as text.
     *
     * @return the status, such as `0`.
     * @since 1.0.0
     */
    override fun toString(): String = value.toString()

    /**
     * The exit codes a shutdown chooses by default.
     *
     * @since 1.0.0
     */
    public companion object {
        /**
         * `0`: the process ended as planned.
         *
         * @since 1.0.0
         */
        public val SUCCESS: ExitCode = ExitCode(SUCCESS_CODE)

        /**
         * `1`: a task of the shutdown failed or did not end in time.
         *
         * @since 1.0.0
         */
        public val FAILURE: ExitCode = ExitCode(FAILURE_CODE)

        /**
         * `70`, `EX_SOFTWARE` of BSD's `sysexits.h`: the process ended on a fatal error.
         *
         * @since 1.0.0
         */
        public val SOFTWARE: ExitCode = ExitCode(SOFTWARE_CODE)

        /**
         * `124`, as `timeout` of GNU coreutils reports it: the shutdown overran its budget and was halted.
         *
         * @since 1.0.0
         */
        public val TIMED_OUT: ExitCode = ExitCode(TIMED_OUT_CODE)
    }
}
