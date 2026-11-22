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

import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours

/**
 * The upper bounds that a shutdown and its tasks are held to.
 *
 * A declaration beyond a limit is reported and refused, so a runaway registration can neither exhaust the memory of a
 * process nor make its shutdown take forever. Raising a limit is a compatible change, lowering one a breaking change.
 *
 * @since 1.0.0
 */
public object ShutdownLimits {
    /**
     * The most tasks one shutdown holds, registered ones included.
     *
     * @since 1.0.0
     */
    public const val MAX_TASKS: Int = 1_024

    /**
     * The most tasks one task waits on.
     *
     * @since 1.0.0
     */
    public const val MAX_DEPENDENCIES: Int = 64

    /**
     * The longest name of a shutdown or a task: an ASCII letter or digit first, then letters, digits, `.`, `_`, `:`
     * and `-`.
     *
     * @since 1.0.0
     */
    public const val MAX_NAME_LENGTH: Int = 128

    /**
     * The most events the [Shutdown.events] flow keeps for a slow collector before it drops the oldest.
     *
     * @since 1.0.0
     */
    public const val MAX_EVENT_CAPACITY: Int = 4_096

    /**
     * The longest grace period of a shutdown.
     *
     * @since 1.0.0
     */
    public val MAX_GRACE_PERIOD: Duration = 1.hours
}
