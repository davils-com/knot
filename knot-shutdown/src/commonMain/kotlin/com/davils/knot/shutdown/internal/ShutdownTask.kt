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

package com.davils.knot.shutdown.internal

import com.davils.knot.shutdown.ShutdownPhase
import com.davils.knot.shutdown.task.ShutdownContext
import com.davils.knot.shutdown.task.ShutdownTaskConfiguration
import com.davils.knot.shutdown.task.ShutdownTaskName
import kotlin.time.Duration

internal class ShutdownTask(
    val name: ShutdownTaskName,
    val phase: ShutdownPhase,
    val timeout: Duration,
    val after: Set<ShutdownTaskName>,
    val action: suspend ShutdownContext.() -> Unit,
) {
    val node: GraphNode get() = GraphNode(name = name, phase = phase, after = after)

    companion object {
        fun of(task: ShutdownTaskConfiguration, defaultTimeout: Duration): ShutdownTask = ShutdownTask(
            name = task.name,
            phase = task.phase,
            timeout = task.timeout ?: defaultTimeout,
            after = task.after,
            action = task.action,
        )
    }
}
