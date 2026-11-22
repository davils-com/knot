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

package com.davils.knot.failure.fixture

import com.davils.knot.failure.FailureFingerprint
import com.davils.knot.failure.FailureId
import com.davils.knot.failure.FailureOrigin
import com.davils.knot.failure.FailureSeverity
import com.davils.knot.failure.UnexpectedFailure

const val FAILURE_ID: String = "0195a1b2-c3d4-7e5f-8a9b-0c1d2e3f4a5b"

fun unexpected(
    cause: Throwable = failure(),
    origin: FailureOrigin = FailureOrigin.Thread("main"),
    thread: String? = "main",
): UnexpectedFailure = UnexpectedFailure(
    id = FailureId(FAILURE_ID),
    cause = cause,
    origin = origin,
    severity = FailureSeverity.Fatal,
    fingerprint = FailureFingerprint("00000000000000ff"),
    at = STARTED_AT,
    thread = thread,
    suppressed = 2,
)
