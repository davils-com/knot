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

package com.davils.knot.failure.internal

import com.davils.knot.failure.FailureFingerprint
import com.davils.knot.failure.FailureLimits
import com.davils.knot.failure.fixture.STARTED_AT
import com.davils.knot.failure.storm.FailureStormConfiguration
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

private const val WIDTH: Int = 16

private val first: FailureFingerprint = FailureFingerprint("0000000000000001")
private val second: FailureFingerprint = FailureFingerprint("0000000000000002")

private fun guard(maxReports: Int = 2): StormGuard = StormGuard(
    FailureStormConfiguration(window = 1.minutes, maxReports = maxReports),
)

class StormGuardTest : FunSpec({
    test("admits failures of a fingerprint up to the limit of a window") {
        val guard = guard()

        guard.admit(first, STARTED_AT, isForced = false).isAdmitted shouldBe true
        guard.admit(first, STARTED_AT, isForced = false).isAdmitted shouldBe true
        guard.admit(first, STARTED_AT, isForced = false).isAdmitted shouldBe false
        guard.admit(second, STARTED_AT, isForced = false).isAdmitted shouldBe true
    }

    test("admits a forced failure beyond the limit") {
        val guard = guard(maxReports = 1)
        guard.admit(first, STARTED_AT, isForced = false)

        guard.admit(first, STARTED_AT, isForced = true).isAdmitted shouldBe true
    }

    test("carries the suppressed count into the next window") {
        val guard = guard(maxReports = 1)
        guard.admit(first, STARTED_AT, isForced = false)
        guard.admit(first, STARTED_AT + 1.seconds, isForced = false)
        guard.admit(first, STARTED_AT + 2.seconds, isForced = false)

        guard.admit(
            first,
            STARTED_AT + 2.minutes,
            isForced = false,
        ) shouldBe StormVerdict(isAdmitted = true, suppressed = 2)
    }

    test("forgets the oldest windows beyond the limit of fingerprints") {
        val guard = guard()
        repeat(FailureLimits.MAX_TRACKED_FINGERPRINTS + 1) { index ->
            guard.admit(FailureFingerprint(index.toString().padStart(WIDTH, '0')), STARTED_AT, isForced = false)
        }

        guard.tracked shouldBe FailureLimits.MAX_TRACKED_FINGERPRINTS
    }

    test("drops expired windows without suppressed failures first") {
        val guard = guard()
        repeat(FailureLimits.MAX_TRACKED_FINGERPRINTS) { index ->
            guard.admit(FailureFingerprint(index.toString().padStart(WIDTH, '0')), STARTED_AT, isForced = false)
        }

        guard.admit(FailureFingerprint("ffffffffffffffff"), STARTED_AT + 2.minutes, isForced = false)

        guard.tracked shouldBe 1
    }
})
