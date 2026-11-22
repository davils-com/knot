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

package com.davils.knot.shutdown.process

import com.davils.knot.shutdown.Shutdown

private const val THREAD_PREFIX: String = "knot-shutdown-watchdog-"

internal class Watchdog(
    private val shutdown: Shutdown,
    private val runtime: ShutdownRuntime,
    private val escalation: Escalation,
) {
    fun arm(): AutoCloseable? {
        val halt = escalation as? Escalation.Halt ?: return null
        val after = shutdown.configuration.gracePeriod + halt.after
        return runtime.schedule(THREAD_PREFIX + shutdown.configuration.name, after) {
            shutdown.telemetry.escalated(halt.exitCode, after)
            runtime.halt(halt.exitCode)
        }
    }
}
