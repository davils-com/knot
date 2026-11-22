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

/**
 * A snapshot of the counters of a shutdown, read by [Shutdown.metrics].
 *
 * @property registeredTasks the tasks the shutdown holds.
 * @property completed the tasks that completed.
 * @property failed the tasks whose action threw.
 * @property timedOut the tasks that ran out of their budget.
 * @property abandoned the tasks that did not end after they were cancelled.
 * @property deadlineExceeded the tasks that never ran because the grace period was over.
 * @property refusedRegistrations the registrations that were refused.
 * @property escalations the times the watchdog halted the process.
 * @property rejectedEvents the events the event bus did not take.
 * @since 1.0.0
 */
public class ShutdownMetrics internal constructor(
    public val registeredTasks: Int,
    public val completed: Long,
    public val failed: Long,
    public val timedOut: Long,
    public val abandoned: Long,
    public val deadlineExceeded: Long,
    public val refusedRegistrations: Long,
    public val escalations: Long,
    public val rejectedEvents: Long,
) {
    /**
     * Returns every counter.
     *
     * @return the counters.
     * @since 1.0.0
     */
    override fun toString(): String =
        "ShutdownMetrics(registeredTasks=$registeredTasks, completed=$completed, failed=$failed, " +
            "timedOut=$timedOut, abandoned=$abandoned, deadlineExceeded=$deadlineExceeded, " +
            "refusedRegistrations=$refusedRegistrations, escalations=$escalations, rejectedEvents=$rejectedEvents)"
}
