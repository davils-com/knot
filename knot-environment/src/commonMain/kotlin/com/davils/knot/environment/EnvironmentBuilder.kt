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

import com.davils.kore.core.annotation.DavilsDsl
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.collection.immutableList
import com.davils.kore.core.dsl.ValidatingBuilder

private const val BUILDER_SUBJECT: String = "environment"
private const val MAX_KEPT_SCOPES: Int = 2

/**
 * Declares an environment on top of a base, for [environment].
 *
 * Every declaration is checked once the block has returned, and every problem is reported in one
 * [ValidationException][com.davils.kore.core.validation.report.ValidationException] with the codes of
 * [EnvironmentViolationCodes]; no violation contains the value of a variable. Declarations beyond
 * [EnvironmentLimits] are not kept but reported once. A builder is meant for one thread and one block; a declaration
 * after the builder was evaluated throws an [IllegalStateException].
 *
 * @since 1.0.0
 */
@DavilsDsl
public class EnvironmentBuilder internal constructor(private val base: Environment) :
    ValidatingBuilder<LayeredEnvironment>() {
    private val layers: MutableList<Environment> = mutableListOf()
    private val variables: MutableList<Pair<String, String>> = mutableListOf()
    private val removals: MutableList<String> = mutableListOf()
    private val scopes: MutableList<String> = mutableListOf()
    private var hasTooManyLayers: Boolean = false
    private var hasTooManyChanges: Boolean = false

    override val subject: String get() = BUILDER_SUBJECT

    /**
     * Stacks [environment] on the base and on every layer declared before it, so its variables win over theirs.
     *
     * The layer is read live, at every read of the built environment. More than [EnvironmentLimits.MAX_LAYERS] layers
     * are reported with [EnvironmentViolationCodes.TOO_MANY_LAYERS].
     *
     * @param environment the environment to stack.
     * @throws IllegalStateException if the builder was evaluated already.
     * @since 1.0.0
     */
    public fun layer(environment: Environment) {
        checkOpen()
        if (!hasRoomForLayer()) return
        layers += environment
    }

    /**
     * Stacks an environment that [block] declares on an empty base, like [layer] with an environment.
     *
     * The problems of [block] are reported under `layers[n]`, where `n` counts the layers declared with a block.
     *
     * @param block declares the variables of the layer.
     * @throws IllegalStateException if the builder was evaluated already.
     * @since 1.0.0
     */
    public fun layer(block: EnvironmentBuilder.() -> Unit) {
        checkOpen()
        if (!hasRoomForLayer()) return
        collect(
            target = layers,
            builder = EnvironmentBuilder(Environment.EMPTY),
            property = ::layers,
            block = block,
        )
    }

    /**
     * Sets the variable [name] to [value], over the base and every layer.
     *
     * The name must not be blank, contain `=` or the NUL character or be longer than
     * [Limits.MAX_NAME_LENGTH][com.davils.kore.core.Limits.MAX_NAME_LENGTH]; the value must not contain the NUL
     * character or be longer than [Limits.MAX_ARGUMENT_LENGTH][com.davils.kore.core.Limits.MAX_ARGUMENT_LENGTH]. A
     * variable must not be set twice or be both set and removed.
     *
     * @param name the name of the variable.
     * @param value the value of the variable.
     * @throws IllegalStateException if the builder was evaluated already.
     * @since 1.0.0
     */
    public fun variable(name: String, value: String) {
        checkOpen()
        if (!hasRoomForChange()) return
        variables += name to value
    }

    /**
     * Hides the variable [name] of the base and of every layer, so that it reads as not set.
     *
     * The name follows the rules of [variable]. A variable must not be removed twice or be both removed and set.
     *
     * @param name the name of the variable.
     * @throws IllegalStateException if the builder was evaluated already.
     * @since 1.0.0
     */
    public fun remove(name: String) {
        checkOpen()
        if (!hasRoomForChange()) return
        removals += name
    }

    /**
     * Restricts the environment to the variables whose names start with [prefix], under their names without it, so
     * that `variable("HOME")` reads `KNOT_HOME` for the prefix `KNOT_`.
     *
     * The scope applies to the whole environment, the overrides and removals included, which are therefore declared
     * with the prefix. A name that is nothing but the prefix is not listed in [Environment.names]. The prefix follows
     * the rules of the names of [variable], and a scope must be declared at most once.
     *
     * @param prefix the prefix of the names to keep.
     * @throws IllegalStateException if the builder was evaluated already.
     * @since 1.0.0
     */
    public fun scope(prefix: String) {
        checkOpen()
        if (scopes.size >= MAX_KEPT_SCOPES) return
        scopes += prefix
    }

    override fun assemble(): LayeredEnvironment = LayeredEnvironment(
        Declarations(
            base = base,
            layers = layers.immutableList(),
            variables = variables.immutableList(),
            removals = removals.immutableList(),
            scopes = scopes.immutableList(),
            hasTooManyLayers = hasTooManyLayers,
            hasTooManyChanges = hasTooManyChanges,
        ),
    )

    private fun hasRoomForLayer(): Boolean {
        hasTooManyLayers = hasTooManyLayers || layers.size >= EnvironmentLimits.MAX_LAYERS
        return !hasTooManyLayers
    }

    private fun hasRoomForChange(): Boolean {
        val changes = variables.size + removals.size
        hasTooManyChanges = hasTooManyChanges || changes >= EnvironmentLimits.MAX_CHANGES
        return !hasTooManyChanges
    }
}
