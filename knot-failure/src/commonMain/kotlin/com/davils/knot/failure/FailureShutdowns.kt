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

package com.davils.knot.failure

import com.davils.knot.shutdown.ShutdownPlanBuilder
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

private const val DRAIN_SUFFIX: String = "-drain"
private const val CLOSE_SUFFIX: String = "-close"
private const val DEFAULT_NAME: String = "failures"

/**
 * Declares the tasks that let [handler] write its last reports in
 * [ShutdownPhase.Drain][com.davils.knot.shutdown.ShutdownPhase.Drain] and close it in
 * [ShutdownPhase.Release][com.davils.knot.shutdown.ShutdownPhase.Release].
 *
 * @param handler the failure handler.
 * @param drainTimeout how long the handler may write; 5 seconds by default.
 * @param name the prefix of the names of the two tasks, `<name>-drain` and `<name>-close`.
 * @since 1.0.0
 */
public fun ShutdownPlanBuilder<*>.failures(
    handler: FailureHandler,
    drainTimeout: Duration = 5.seconds,
    name: String = DEFAULT_NAME,
) {
    drain {
        task(name + DRAIN_SUFFIX) {
            timeout = drainTimeout
            action { handler.drain() }
        }
    }
    release {
        task(name + CLOSE_SUFFIX) { action { handler.close() } }
    }
}
