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

import com.davils.knot.shutdown.RegistrationRefusal
import com.davils.knot.shutdown.ShutdownRegistration
import com.davils.knot.shutdown.ShutdownRegistrationBuilder
import com.davils.knot.shutdown.task.ShutdownTaskName
import kotlin.time.Duration

internal class TaskRegistry(
    initial: List<ShutdownTask>,
    private val defaultTimeout: Duration,
    private val refused: (RegistrationRefusal) -> Unit,
) {
    private val guard: Guard = platformGuard()
    private var tasks: Map<ShutdownTaskName, ShutdownTask> = initial.associateBy { task -> task.name }
    private var isSealed: Boolean = false

    val size: Int get() = guard.locked { tasks.size }

    fun register(block: ShutdownRegistrationBuilder.() -> Unit): ShutdownRegistration {
        val builder = ShutdownRegistrationBuilder(::nodes)
        builder.block()
        val registration = guard.locked { admit(builder) }
        if (registration is ShutdownRegistration.Refused) refused(registration.refusal)
        return registration
    }

    fun seal(): List<ShutdownTask> = guard.locked {
        isSealed = true
        tasks.values.toList()
    }

    private fun admit(builder: ShutdownRegistrationBuilder): ShutdownRegistration {
        if (isSealed) return ShutdownRegistration.Refused(RegistrationRefusal.ShuttingDown)
        return validated(builder)
    }

    private fun validated(builder: ShutdownRegistrationBuilder): ShutdownRegistration {
        val report = builder.validate()
        if (!report.isValid) return ShutdownRegistration.Refused(RegistrationRefusal.Invalid(report))
        return accepted(builder)
    }

    private fun accepted(builder: ShutdownRegistrationBuilder): ShutdownRegistration {
        val plan = builder.build()
        val added = plan.tasks.map { task -> ShutdownTask.of(task, defaultTimeout) }
        tasks = tasks + added.associateBy { task -> task.name }
        val names = added.map { task -> task.name }.toSet()
        return ShutdownRegistration.Registered(names, ::release)
    }

    private fun release(names: Set<ShutdownTaskName>): Boolean = guard.locked {
        val present = names intersect tasks.keys
        val remaining = tasks - present
        val isNeeded = remaining.values.any { task -> task.after.any { dependency -> dependency in present } }
        val isReleased = !isSealed && present.isNotEmpty() && !isNeeded
        if (isReleased) tasks = remaining
        isReleased
    }

    private fun nodes(): Collection<GraphNode> = guard.locked { tasks.values.map { task -> task.node } }
}
