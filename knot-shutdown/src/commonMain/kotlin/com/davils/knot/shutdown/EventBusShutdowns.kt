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

package com.davils.knot.shutdown

import com.davils.konfetti.core.EventBus
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

private const val DRAIN_SUFFIX: String = "-drain"
private const val CLOSE_SUFFIX: String = "-close"
private const val DEFAULT_NAME: String = "event-bus"
private val drainMargin: Duration = 1.seconds

/**
 * Declares the tasks that drain the konfetti event bus [bus] in [ShutdownPhase.Drain] and close it in
 * [ShutdownPhase.Release].
 *
 * The drain task waits at most [drainTimeout] and never longer than the time its budget leaves, less a second for the
 * report of the bus.
 *
 * @param bus the event bus.
 * @param drainTimeout how long the bus may drain; 5 seconds by default.
 * @param name the prefix of the names of the two tasks, `<name>-drain` and `<name>-close`.
 * @since 1.0.0
 */
public fun ShutdownPlanBuilder<*>.eventBus(
    bus: EventBus,
    drainTimeout: Duration = 5.seconds,
    name: String = DEFAULT_NAME,
) {
    drain {
        task(name + DRAIN_SUFFIX) {
            timeout = drainTimeout + drainMargin
            action {
                val budget = minOf(a = drainTimeout, b = remaining - drainMargin)
                bus.drain(budget.coerceAtLeast(Duration.ZERO))
            }
        }
    }
    release {
        task(name + CLOSE_SUFFIX) { action { bus.close() } }
    }
}
