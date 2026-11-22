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

import com.davils.knot.failure.FailureFingerprint
import com.davils.knot.failure.FailureId
import com.davils.knot.failure.FailureOrigin
import com.davils.knot.failure.FailureSeverity
import com.davils.knot.failure.UnexpectedFailure
import kotlin.time.Instant

private const val MESSAGE: String = "the test failure"
private const val THREAD: String = "test"

/**
 * Ready failures for tests of sinks and renderers.
 *
 * @since 1.0.0
 */
public object TestFailures {
    /**
     * The time every test failure happened, unless it says otherwise.
     *
     * @since 1.0.0
     */
    public val AT: Instant = Instant.parse("2026-11-22T12:00:00Z")

    /**
     * The origin every test failure has, unless it says otherwise.
     *
     * @since 1.0.0
     */
    public val ORIGIN: FailureOrigin = FailureOrigin.Thread("test")

    /**
     * The id every test failure has, unless it says otherwise.
     *
     * @since 1.0.0
     */
    public const val ID: String = "01936d2a-0000-7000-8000-000000000001"

    /**
     * Returns a failure with the facts of the parameters.
     *
     * @param cause what failed; an `IllegalStateException` by default.
     * @param severity how bad it is; recoverable by default.
     * @param origin where it happened.
     * @param id its id.
     * @param at when it happened.
     * @param suppressed how many failures of its fingerprint were suppressed before.
     * @return the failure, with the fingerprint of [cause] and the thread `test`.
     * @since 1.0.0
     */
    public fun failure(
        cause: Throwable = IllegalStateException(MESSAGE),
        severity: FailureSeverity = FailureSeverity.Recoverable,
        origin: FailureOrigin = ORIGIN,
        id: String = ID,
        at: Instant = AT,
        suppressed: Int = 0,
    ): UnexpectedFailure = UnexpectedFailure(
        id = FailureId(id),
        cause = cause,
        origin = origin,
        severity = severity,
        fingerprint = FailureFingerprint.of(cause),
        at = at,
        thread = THREAD,
        suppressed = suppressed,
    )
}
