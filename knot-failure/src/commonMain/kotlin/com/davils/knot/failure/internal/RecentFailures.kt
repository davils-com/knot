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
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Instant

internal class RecentFailures(private val window: Duration, private val clock: Clock) {
    private val guard: Guard = platformGuard()
    private val times: ArrayDeque<Instant> = ArrayDeque()

    fun record(at: Instant) {
        guard.locked {
            times.addLast(at)
            if (times.size > FailureLimits.MAX_RECENT) times.removeFirst()
        }
    }

    fun count(): Int = guard.locked {
        val horizon = clock.now() - window
        while (times.firstOrNull()?.let { first -> first <= horizon } == true) times.removeFirst()
        times.size
    }
}
