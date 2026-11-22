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
import com.davils.kore.core.collection.immutableSet
import com.davils.kore.core.text.SafeText
import com.davils.kore.core.validation.Validatable
import com.davils.kore.core.validation.ValidationScope

/**
 * An environment composed of a base, layers on top of it, changes and rules, as [environment] declares it.
 *
 * A variable is read in this order: a filter or a removal hides it; an override wins over every layer; a later layer
 * wins over an earlier one; the base comes next and a default last; a variable that none of them sets falls back to
 * its aliases. With a scope, only the variables whose names start with its prefix are seen, under their names without
 * the prefix. With an expansion, the references in a value are replaced when it is read. The base and the layers are
 * read live at every call unless the environment is a snapshot; the overrides and defaults are fixed when the
 * environment is built. The environment is safe to share between threads as long as its base and layers are.
 *
 * Two environments are equal only when they are the same object, because the variables they read may change. The
 * `toString` shows the names of the variables, escaped, and `****` in place of every value.
 *
 * @since 1.0.0
 */
public class LayeredEnvironment internal constructor(private val declarations: Declarations) :
    Environment,
    Validatable {
    private val options: Options = declarations.options
    private val isCaseInsensitive: Boolean = options.isCaseInsensitive
    private val base: Environment = sourceOf(declarations.base)
    private val layers: List<Environment> = sourcesOf(declarations.layers)
    private val variables: Map<String, String> = keyed(declarations.changes.variables)
    private val removals: Set<String> = declarations.changes.removals
        .map(::keyOf)
        .immutableSet()
    private val defaults: Map<String, String> = keyed(declarations.changes.defaults)
    private val aliases: Map<String, List<String>> = keyed(declarations.rules.aliases)
    private val filters: List<NameFilter> = declarations.rules.filters
    private val required: List<String> = declarations.rules.required
    private val scope: String? = options.scopes.firstOrNull()
    private val hasInclusions: Boolean = filters.any { filter -> filter.isInclusion }

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
            val scoped = unscoped.filter { name -> name.length > prefix.length && hasPrefix(name, prefix) }
            return scoped.map { name -> name.substring(prefix.length) }.immutableSet()
        }

    /**
     * Returns the value of the variable [name] after the scope, the filters, the removals, the overrides, the layers,
     * the base, the defaults and the aliases, with its references expanded if the environment expands them.
     *
     * @param name the name of the variable, without the prefix of the scope.
     * @return the value, or `null` if the variable is not set, was removed or is filtered out.
     * @throws EnvironmentException if the environment expands references and the value holds one that cannot be
     * expanded; the message names the variable and never the value.
     * @since 1.0.0
     */
    override fun variable(name: String): String? {
        val unscoped = scope.orEmpty() + name
        val raw = resolved(unscoped)
        val onMissing = options.expansion
        if (raw == null || onMissing == null) return raw
        return Expansion(variable = name, onMissing = onMissing, lookup = ::resolved).of(raw)
    }

    /**
     * Checks the declaration of the environment; [environment] calls it before it returns the environment.
     *
     * @since 1.0.0
     */
    override fun ValidationScope.validate() {
        with(DeclarationCheck(declarations)) { check() }
        validateRequired()
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

    private fun ValidationScope.validateRequired() {
        val missing = required.filter { name -> resolved(name) == null }.associateWith { MISSING_MESSAGE }
        each(::required, missing) { message -> fail(message, EnvironmentViolationCodes.MISSING_REQUIRED) }
    }

    private fun resolved(name: String): String? {
        if (!isShown(name)) return null
        return ownVariable(name) ?: aliasedVariable(name)
    }

    private fun aliasedVariable(name: String): String? {
        val fallbacks = aliases[keyOf(name)].orEmpty()
        return fallbacks.firstNotNullOfOrNull { fallback -> ownVariable(fallback)?.takeIf { isShown(fallback) } }
    }

    private fun ownVariable(name: String): String? {
        val key = keyOf(name)
        if (key in removals) return null
        val declared = variables[key] ?: layers.firstNotNullOfOrNull { layer -> read(layer, name) }
        return declared ?: read(base, name) ?: defaults[key]
    }

    private fun read(source: Environment, name: String): String? {
        if (isCaseInsensitive) return source.foldedVariable(name)
        return source.variable(name)
    }

    private fun isShown(name: String): Boolean {
        val isIncluded = !hasInclusions || filters.any { filter -> filter.isInclusion && matches(filter, name) }
        val isExcluded = filters.any { filter -> !filter.isInclusion && matches(filter, name) }
        return isIncluded && !isExcluded
    }

    private fun matches(filter: NameFilter, name: String): Boolean = filter.matches(name, ::keyOf)

    private fun hasPrefix(name: String, prefix: String): Boolean = keyOf(name).startsWith(keyOf(prefix))

    private fun maskedEntry(name: String): String = "${SafeText.text(name)}=${SafeText.REDACTED}"

    private fun unscopedNames(): Set<String> {
        val collected = LinkedHashMap<String, String>()
        val declared = declarations.changes.variables.map { (name, _) -> name }
        val layered = layers.flatMap { layer -> layer.names }
        val defaulted = declarations.changes.defaults.map { (name, _) -> name }
        val aliased = declarations.rules.aliases.map { (name, _) -> name }
        (declared + layered + base.names + defaulted + aliased).forEach { name ->
            collected.getOrPut(keyOf(name)) { name }
        }
        return collected.values.filterTo(LinkedHashSet()) { name -> resolved(name) != null }
    }

    private fun keyOf(name: String): String {
        if (isCaseInsensitive) return foldedName(name)
        return name
    }

    private fun <V> keyed(pairs: List<Pair<String, V>>): Map<String, V> {
        val keyed = LinkedHashMap<String, V>()
        pairs.forEach { (name, value) -> keyed.getOrPut(keyOf(name)) { value } }
        return keyed
    }

    private fun sourcesOf(environments: List<Environment>): List<Environment> {
        val latestFirst = environments.asReversed()
        return latestFirst.map(::sourceOf).immutableList()
    }

    private fun sourceOf(environment: Environment): Environment {
        if (options.isSnapshot) return SnapshotEnvironment(environment)
        return environment
    }

    private companion object {
        const val MISSING_MESSAGE: String = "is required but not set"
    }
}
