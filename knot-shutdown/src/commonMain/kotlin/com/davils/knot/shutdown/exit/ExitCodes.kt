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

import com.davils.knot.shutdown.ShutdownReason

/**
 * The exit codes a shutdown reports in its [ShutdownReport][com.davils.knot.shutdown.report.ShutdownReport].
 *
 * A shutdown that a fatal error started ends with [fatal]; a clean one with the exit code of its
 * [ShutdownReason.Requested] or [completed]; every other with [failed].
 *
 * @property completed the code of a clean shutdown; [ExitCode.SUCCESS] by default.
 * @property failed the code of a shutdown with a task that failed or did not end in time; [ExitCode.FAILURE] by
 * default.
 * @property fatal the code of a shutdown started by a fatal error; [ExitCode.SOFTWARE] by default.
 * @since 1.0.0
 */
public class ExitCodes(
    public val completed: ExitCode = ExitCode.SUCCESS,
    public val failed: ExitCode = ExitCode.FAILURE,
    public val fatal: ExitCode = ExitCode.SOFTWARE,
) {
    /**
     * Chooses the exit code of a shutdown for [reason].
     *
     * @param reason why the shutdown ran.
     * @param isClean whether every task completed.
     * @return the exit code.
     * @since 1.0.0
     */
    public fun codeOf(reason: ShutdownReason, isClean: Boolean): ExitCode {
        if (reason is ShutdownReason.FatalError) return fatal
        val requested = (reason as? ShutdownReason.Requested)?.exitCode ?: completed
        return requested.takeIf { isClean } ?: failed
    }

    /**
     * Tells whether [other] chooses the same codes.
     *
     * @param other the object to compare with.
     * @return `true` if [other] is equal.
     * @since 1.0.0
     */
    override fun equals(other: Any?): Boolean =
        other is ExitCodes && completed == other.completed && failed == other.failed && fatal == other.fatal

    /**
     * Returns a hash code consistent with [equals].
     *
     * @return the hash code.
     * @since 1.0.0
     */
    override fun hashCode(): Int = listOf(completed, failed, fatal).hashCode()

    /**
     * Returns the codes.
     *
     * @return the codes, such as `ExitCodes(completed=0, failed=1, fatal=70)`.
     * @since 1.0.0
     */
    override fun toString(): String = "ExitCodes(completed=$completed, failed=$failed, fatal=$fatal)"
}
