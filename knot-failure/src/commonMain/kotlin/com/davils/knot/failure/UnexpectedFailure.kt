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

package com.davils.knot.failure

import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.text.SafeText
import kotlin.time.Instant

/**
 * One failure that no code handled, as a failure handler took it in.
 *
 * @property id the unique id of the failure.
 * @property cause the failure.
 * @property origin where it happened.
 * @property severity how bad it is.
 * @property fingerprint what makes it the same as other failures.
 * @property at when it was taken in.
 * @property thread the name of the thread it was taken in on the JVM, `null` on the `wasmJs` target.
 * @property suppressed how many failures of the same fingerprint the storm guard suppressed before this one.
 * @since 1.0.0
 */
public class UnexpectedFailure(
    public val id: FailureId,
    public val cause: Throwable,
    public val origin: FailureOrigin,
    public val severity: FailureSeverity,
    public val fingerprint: FailureFingerprint,
    public val at: Instant,
    public val thread: String?,
    public val suppressed: Int,
) {
    /**
     * Tells whether [other] is the failure with the same id.
     *
     * @param other the object to compare with.
     * @return `true` if [other] has the same id.
     * @since 1.0.0
     */
    override fun equals(other: Any?): Boolean = other is UnexpectedFailure && id == other.id

    /**
     * Returns the hash code of the id.
     *
     * @return the hash code.
     * @since 1.0.0
     */
    override fun hashCode(): Int = id.hashCode()

    /**
     * Returns the id, the type of the cause, the origin and the severity, never the message of the cause.
     *
     * @return the failure, such as `UnexpectedFailure(0195…, IOException from thread "main", Recoverable)`.
     * @since 1.0.0
     */
    override fun toString(): String =
        "UnexpectedFailure($id, ${typeNameOf(cause)} from $origin, $severity, fingerprint=$fingerprint, " +
            "suppressed=$suppressed, thread=${thread?.let(SafeText::quoted) ?: NONE})"

    private companion object {
        const val NONE: String = "none"
    }
}
