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

package com.davils.knot.failure.internal

import com.davils.knot.failure.FailureFingerprint
import com.davils.knot.failure.FailureLimits
import com.davils.knot.failure.storm.FailureStormConfiguration
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.concurrency.Guard
import com.davils.kore.core.concurrency.guard
import kotlin.time.Instant

internal class StormGuard(private val storm: FailureStormConfiguration) {
    private val guard: Guard = guard()
    private val windows: LinkedHashMap<FailureFingerprint, StormWindow> = LinkedHashMap()

    val tracked: Int get() = guard.locked { windows.size }

    fun admit(fingerprint: FailureFingerprint, at: Instant, isForced: Boolean): StormVerdict =
        guard.locked { decided(fingerprint, at, isForced) }

    private fun decided(fingerprint: FailureFingerprint, at: Instant, isForced: Boolean): StormVerdict {
        val current = windows[fingerprint]?.takeIf { window -> isOpen(window, at) } ?: return opened(fingerprint, at)
        val isAdmitted = isForced || current.reported < storm.maxReports
        return counted(fingerprint, current, isAdmitted)
    }

    private fun opened(fingerprint: FailureFingerprint, at: Instant): StormVerdict {
        val carried = windows.remove(fingerprint)?.suppressed ?: 0
        if (windows.size >= FailureLimits.MAX_TRACKED_FINGERPRINTS) makeRoom(at)
        windows[fingerprint] = StormWindow(start = at, reported = 1, suppressed = 0)
        return StormVerdict(isAdmitted = true, suppressed = carried)
    }

    private fun makeRoom(at: Instant) {
        windows.entries.removeAll { (_, window) -> !isOpen(window, at) && window.suppressed == 0 }
        if (windows.size >= FailureLimits.MAX_TRACKED_FINGERPRINTS) windows.remove(windows.keys.first())
    }

    private fun counted(fingerprint: FailureFingerprint, current: StormWindow, isAdmitted: Boolean): StormVerdict {
        val next = current.copy(
            reported = current.reported + admittedCount(isAdmitted),
            suppressed = current.suppressed + admittedCount(!isAdmitted),
        )
        windows[fingerprint] = next
        return StormVerdict(isAdmitted = isAdmitted, suppressed = 0)
    }

    private fun admittedCount(isCounted: Boolean): Int {
        if (isCounted) return 1
        return 0
    }

    private fun isOpen(window: StormWindow, at: Instant): Boolean = at - window.start < storm.window
}
