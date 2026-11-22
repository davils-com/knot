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

import com.davils.knot.shutdown.ShutdownViolationCodes
import com.davils.knot.shutdown.task.ShutdownTaskConfiguration
import com.davils.knot.shutdown.task.ShutdownTaskName
import com.davils.kore.core.validation.ValidationScope
import kotlin.reflect.KProperty

private const val DUPLICATE_MESSAGE: String = "is already registered"
private const val UNKNOWN_MESSAGE: String = "waits on a task that is not declared"
private const val LATER_MESSAGE: String = "waits on a task of a later phase"
private const val CYCLE_MESSAGE: String = "is part of a dependency cycle or waits on one"

private val dependencies: KProperty<*> = ShutdownTaskConfiguration::after

internal fun ValidationScope.checkGraph(
    registered: Collection<GraphNode>,
    declared: List<ShutdownTaskConfiguration>,
    property: KProperty<*>,
) {
    val added = declared.associate { task -> task.name.value to nodeOf(task) }
    val known = registered.map { node -> node.name }.toSet()
    val all = (registered + added.values).associateBy { node -> node.name }
    val cyclic = DependencyCycles(all.values).members()
    each(property, added) { node ->
        if (node.name in known) fail(DUPLICATE_MESSAGE, ShutdownViolationCodes.DUPLICATE_TASK)
        node.after.forEach { dependency -> checkDependency(node, dependency, all) }
        if (node.name in cyclic) fail(CYCLE_MESSAGE, ShutdownViolationCodes.DEPENDENCY_CYCLE)
    }
}

internal fun nodeOf(task: ShutdownTaskConfiguration): GraphNode =
    GraphNode(name = task.name, phase = task.phase, after = task.after)

private fun ValidationScope.checkDependency(
    node: GraphNode,
    dependency: ShutdownTaskName,
    all: Map<ShutdownTaskName, GraphNode>,
) {
    val target = all[dependency]
    if (target == null) return fail(dependencies, UNKNOWN_MESSAGE, ShutdownViolationCodes.UNKNOWN_DEPENDENCY)
    if (target.phase > node.phase) fail(dependencies, LATER_MESSAGE, ShutdownViolationCodes.LATER_PHASE_DEPENDENCY)
}
