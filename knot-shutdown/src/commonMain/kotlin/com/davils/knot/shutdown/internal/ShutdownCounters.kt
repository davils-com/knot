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

package com.davils.knot.shutdown.internal

import com.davils.knot.shutdown.ShutdownMetrics
import kotlin.concurrent.atomics.AtomicLong
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.concurrent.atomics.incrementAndFetch

internal class ShutdownCounters {
    private val outcomes: Map<OutcomeKind, AtomicLong> = OutcomeKind.entries.associateWith { AtomicLong(0L) }
    private val refusals: AtomicLong = AtomicLong(0L)
    private val escalations: AtomicLong = AtomicLong(0L)
    private val rejectedEvents: AtomicLong = AtomicLong(0L)

    fun outcome(kind: OutcomeKind) {
        outcomes.getValue(kind).incrementAndFetch()
    }

    fun refused() {
        refusals.incrementAndFetch()
    }

    fun escalated() {
        escalations.incrementAndFetch()
    }

    fun rejectedEvent() {
        rejectedEvents.incrementAndFetch()
    }

    fun snapshot(registeredTasks: Int): ShutdownMetrics = ShutdownMetrics(
        registeredTasks = registeredTasks,
        completed = countOf(OutcomeKind.Completed),
        failed = countOf(OutcomeKind.Failed),
        timedOut = countOf(OutcomeKind.TimedOut),
        abandoned = countOf(OutcomeKind.Abandoned),
        deadlineExceeded = countOf(OutcomeKind.DeadlineExceeded),
        refusedRegistrations = refusals.load(),
        escalations = escalations.load(),
        rejectedEvents = rejectedEvents.load(),
    )

    private fun countOf(kind: OutcomeKind): Long = outcomes.getValue(kind).load()
}
