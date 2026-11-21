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

package com.davils.knot.environment

import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.collection.immutableList
import com.davils.kore.core.collection.immutableMap
import com.davils.kore.core.collection.immutableSet
import com.davils.kore.core.text.SafeText
import com.davils.kore.core.validation.Validatable
import com.davils.kore.core.validation.ValidationScope
import kotlin.reflect.KProperty

private const val TOO_MANY_LAYERS_MESSAGE: String =
    "must not stack more than ${EnvironmentLimits.MAX_LAYERS} layers"
private const val TOO_MANY_CHANGES_MESSAGE: String =
    "must not declare more than ${EnvironmentLimits.MAX_CHANGES} overrides and removals"
private const val DUPLICATE_MESSAGE: String = "is declared more than once"
private const val CONFLICT_MESSAGE: String = "is both overridden and removed"
private const val REPEATED_SCOPE_MESSAGE: String = "is declared more than once"

/**
 * An environment composed of a base, layers on top of it and changes, as [environment] declares it.
 *
 * A variable is read in this order: a removed variable is never set; an override wins over every layer; a later layer
 * wins over an earlier one; the base comes last. With a scope, only the variables whose names start with its prefix
 * are seen, under their names without the prefix. The base and the layers are read live at every call, the overrides
 * are fixed when the environment is built. The environment is safe to share between threads as long as its base and
 * layers are.
 *
 * Two environments are equal only when they are the same object, because the variables they read may change. The
 * `toString` shows the names of the variables, escaped, and `****` in place of every value.
 *
 * @since 1.0.0
 */
public class LayeredEnvironment internal constructor(
    private val declarations: Declarations,
) : Environment, Validatable {
    private val base: Environment = declarations.base
    private val layers: List<Environment> = declarations.layers.asReversed().immutableList()
    private val variables: Map<String, String> = declarations.variables.toMap().immutableMap()
    private val removals: Set<String> = declarations.removals.immutableSet()
    private val scope: String? = declarations.scopes.firstOrNull()

    /**
     * The names of all variables that [variable] reads, as an immutable snapshot of the base and the layers taken
     * when the property is read.
     *
     * @since 1.0.0
     */
    override val names: Set<String>
        get() {
            val unscoped = unscopedNames()
            val prefix = scope ?: return unscoped.immutableSet()
            val scoped = unscoped.filter { name -> name.length > prefix.length && name.startsWith(prefix) }
            return scoped.map { name -> name.removePrefix(prefix) }.immutableSet()
        }

    /**
     * Returns the value of the variable [name] after the scope, the removals, the overrides and the layers.
     *
     * @param name the name of the variable, compared exactly, without the prefix of the scope.
     * @return the value, or `null` if the variable is not set or was removed.
     * @since 1.0.0
     */
    override fun variable(name: String): String? {
        val prefix = scope ?: return unscopedVariable(name)
        return unscopedVariable(prefix + name)
    }

    /**
     * Checks the declaration of the environment; [environment] calls it before it returns the environment.
     *
     * @since 1.0.0
     */
    override fun ValidationScope.validate() {
        if (declarations.hasTooManyLayers) {
            fail(::layers, TOO_MANY_LAYERS_MESSAGE, EnvironmentViolationCodes.TOO_MANY_LAYERS)
        }
        if (declarations.hasTooManyChanges) {
            fail(::variables, TOO_MANY_CHANGES_MESSAGE, EnvironmentViolationCodes.TOO_MANY_CHANGES)
        }
        validateOverrides()
        validateRemovals()
        validateScope()
    }

    /**
     * Returns the names of the variables, escaped and in alphabetical order, with `****` in place of every value.
     *
     * @return the masked variables, such as `{HOME=****, PATH=****}`.
     * @since 1.0.0
     */
    override fun toString(): String {
        val sorted = names.sorted()
        return sorted.joinToString(prefix = "{", postfix = "}") { name -> maskedEntry(name) }
    }

    private fun unscopedVariable(name: String): String? {
        if (name in removals) return null
        val layered = variables[name] ?: layers.firstNotNullOfOrNull { layer -> layer.variable(name) }
        return layered ?: base.variable(name)
    }

    private fun maskedEntry(name: String): String = "${SafeText.text(name)}=${SafeText.REDACTED}"

    private fun unscopedNames(): Set<String> {
        val collected = LinkedHashSet(base.names)
        layers.forEach { layer -> collected += layer.names }
        collected += variables.keys
        collected -= removals
        return collected
    }

    private fun ValidationScope.validateOverrides() {
        val valid = validNames(::variables, declarations.variables.map { (name, _) -> name })
        val valueRefusals = declarations.variables
            .filter { (name, _) -> name in valid }
            .mapNotNull { (name, value) -> valueRefusal(value)?.let { refusal -> name to refusal } }
        each(::variables, valueRefusals.toMap()) { refusal -> fail(refusal.message, refusal.code) }
        failDuplicates(::variables, valid)
    }

    private fun ValidationScope.validateRemovals() {
        val valid = validNames(::removals, declarations.removals)
        failDuplicates(::removals, valid)
        val overridden = declarations.variables.map { (name, _) -> name }.toSet()
        val conflicts = valid.filter { name -> name in overridden }.associateWith { CONFLICT_MESSAGE }
        each(::removals, conflicts) { message -> fail(message, EnvironmentViolationCodes.CONFLICTING_CHANGE) }
    }

    private fun ValidationScope.validateScope() {
        if (declarations.scopes.size > 1) {
            fail(::scope, REPEATED_SCOPE_MESSAGE, EnvironmentViolationCodes.REPEATED_SCOPE)
        }
        val prefix = scope ?: return
        val refusal = nameRefusal(prefix) ?: return
        fail(::scope, refusal.message, refusal.code)
    }

    private fun ValidationScope.validNames(property: KProperty<*>, names: List<String>): List<String> {
        val valid = mutableListOf<String>()
        names.forEach { name ->
            val refusal = nameRefusal(name)
            if (refusal == null) {
                valid += name
                return@forEach
            }
            fail(property, refusal.message, refusal.code)
        }
        return valid
    }

    private fun ValidationScope.failDuplicates(property: KProperty<*>, names: List<String>) {
        val counts = names.groupingBy { name -> name }.eachCount()
        val duplicates = counts.filterValues { count -> count > 1 }.mapValues { DUPLICATE_MESSAGE }
        each(property, duplicates) { message -> fail(message, EnvironmentViolationCodes.DUPLICATE_CHANGE) }
    }
}
