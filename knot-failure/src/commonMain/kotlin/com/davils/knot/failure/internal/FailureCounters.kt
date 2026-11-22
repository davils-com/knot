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

@file:OptIn(ExperimentalAtomicApi::class)

package com.davils.knot.failure.internal

import com.davils.knot.failure.FailureMetrics
import kotlin.concurrent.atomics.AtomicLong
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.concurrent.atomics.incrementAndFetch

internal class FailureCounters {
    private val reported: AtomicLong = AtomicLong(0L)
    private val suppressed: AtomicLong = AtomicLong(0L)
    private val ignored: AtomicLong = AtomicLong(0L)
    private val written: AtomicLong = AtomicLong(0L)
    private val sinkFailures: AtomicLong = AtomicLong(0L)
    private val rejectedEvents: AtomicLong = AtomicLong(0L)

    fun reported() {
        reported.incrementAndFetch()
    }

    fun suppressed() {
        suppressed.incrementAndFetch()
    }

    fun ignored() {
        ignored.incrementAndFetch()
    }

    fun written(isWritten: Boolean) {
        val counter = written.takeIf { isWritten } ?: sinkFailures
        counter.incrementAndFetch()
    }

    fun rejectedEvent() {
        rejectedEvents.incrementAndFetch()
    }

    fun snapshot(recent: Int): FailureMetrics = FailureMetrics(
        reported = reported.load(),
        suppressed = suppressed.load(),
        ignored = ignored.load(),
        written = written.load(),
        sinkFailures = sinkFailures.load(),
        recent = recent,
        rejectedEvents = rejectedEvents.load(),
    )
}
