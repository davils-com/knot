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

import com.davils.knot.failure.FailureLimits
import com.davils.knot.failure.fixture.MovableClock
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class RecentFailuresTest : FunSpec({
    test("counts the failures within the window") {
        val clock = MovableClock()
        val recent = RecentFailures(window = 1.minutes, clock = clock)
        recent.record(clock.now())
        clock.advance(30.seconds)
        recent.record(clock.now())

        recent.count() shouldBe 2
        clock.advance(40.seconds)
        recent.count() shouldBe 1
    }

    test("keeps at most the limit of failures") {
        val clock = MovableClock()
        val recent = RecentFailures(window = 1.minutes, clock = clock)
        repeat(FailureLimits.MAX_RECENT + 1) { recent.record(clock.now()) }

        recent.count() shouldBe FailureLimits.MAX_RECENT
    }
})
