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
 * A variable is read in this order: a filter or a removal hides it; an override wins over every layer; a later layer
 * wins over an earlier one; the base comes next and a default last. A variable that none of them sets falls back to
 * its aliases. A scope then restricts the result to a prefix, and an expansion replaces the references in the value.
 *
 * Every declaration is checked once the block has returned, and every problem is reported in one
 * [ValidationException][com.davils.kore.core.validation.report.ValidationException] with the codes of
 * [EnvironmentViolationCodes]; no violation contains the value of a variable. Declarations beyond
 * [EnvironmentLimits] are not kept but reported once. Nothing is read from or written to a file: a dotenv text is
 * handed over as a string. A builder is meant for one thread and one block; a declaration after the builder was
 * evaluated throws an [IllegalStateException].
 *
 * @since 1.0.0
 */
@DavilsDsl
public class EnvironmentBuilder internal constructor(private val base: Environment) :
    ValidatingBuilder<LayeredEnvironment>() {
    private val layers: MutableList<Environment> = mutableListOf()
    private val variables: MutableList<Pair<String, String>> = mutableListOf()
    private val removals: MutableList<String> = mutableListOf()
    private val defaults: MutableList<Pair<String, String>> = mutableListOf()
    private val aliases: MutableList<Pair<String, List<String>>> = mutableListOf()
    private val filters: MutableList<NameFilter> = mutableListOf()
    private val required: MutableList<String> = mutableListOf()
    private val scopes: MutableList<String> = mutableListOf()
    private val dotenvProblems: MutableList<DotenvProblem> = mutableListOf()
    private var namePrefix: String = ""
    private var isCaseInsensitive: Boolean = false
    private var expansion: MissingReference? = null
    private var isSnapshot: Boolean = false
    private var hasTooManyLayers: Boolean = false
    private var hasTooManyChanges: Boolean = false
    private var hasTooManyRules: Boolean = false

    /**
     * The grammar every declared name must follow; [NameGrammar.Lenient] by default.
     *
     * It applies to the names of overrides, removals, defaults, aliases, filters and required variables, not to the
     * variables of the base and the layers.
     *
     * @throws IllegalStateException if it is set after the builder was evaluated.
     * @since 1.0.0
     */
    public var grammar: NameGrammar = NameGrammar.Lenient
        set(value) {
            checkOpen()
            field = value
        }

    override val subject: String get() = BUILDER_SUBJECT

    /**
     * Stacks [environment] on the base and on every layer declared before it, so its variables win over theirs.
     *
     * The layer is read live, at every read of the built environment, unless [snapshot] is declared. More than
     * [EnvironmentLimits.MAX_LAYERS] layers are reported with [EnvironmentViolationCodes.TOO_MANY_LAYERS].
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
     * The problems of [block] are reported under `layers[n]`, where `n` counts the layers declared with a block, a
     * map or a dotenv text.
     *
     * @param block declares the variables of the layer.
     * @throws IllegalStateException if the builder was evaluated already.
     * @since 1.0.0
     */
    public fun layer(block: EnvironmentBuilder.() -> Unit) {
        checkOpen()
        if (!hasRoomForLayer()) return
        collect(target = layers, builder = EnvironmentBuilder(Environment.EMPTY), property = ::layers, block = block)
    }

    /**
     * Stacks the variables of [variables] as a layer, like [layer] with a block that declares each of them.
     *
     * @param variables the names and values of the layer, which follow the rules of [variable].
     * @throws IllegalStateException if the builder was evaluated already.
     * @since 1.0.0
     */
    public fun layer(variables: Map<String, String>) {
        layer { variables(variables) }
    }

    /**
     * Stacks the variables of the dotenv text [text] as a layer.
     *
     * Every line is blank, a comment that starts with `#`, or an assignment `NAME=value`, optionally after `export `.
     * A value is unquoted, where a ` #` starts a comment and surrounding whitespace is dropped; single-quoted, which is
     * taken literally; or double-quoted, where `\n`, `\r`, `\t`, `\"`, `\\` and `\$` are escapes. Quoted values may
     * span lines. A name assigned twice keeps its last value. References such as `${NAME}` are kept as they are
     * unless [expand] is declared. A line that cannot be read is reported with
     * [EnvironmentViolationCodes.INVALID_DOTENV] and its number, never its text; a text longer than
     * [EnvironmentLimits.MAX_DOTENV_LENGTH] or [EnvironmentLimits.MAX_DOTENV_LINES] is not read but reported with
     * [EnvironmentViolationCodes.TOO_LONG]. Knot never opens a file: the caller reads the text.
     *
     * @param text the dotenv text.
     * @throws IllegalStateException if the builder was evaluated already.
     * @since 1.0.0
     */
    public fun dotenv(text: String) {
        checkOpen()
        if (!hasRoomForLayer()) return
        val parsed = Dotenv.parse(text)
        dotenvProblems += parsed.problems
        layer(parsed.variables)
    }

    /**
     * Sets the variable [name] to [value], over the base and every layer.
     *
     * The name must not be blank, contain `=` or the NUL character, break the [grammar] or be longer than
     * [Limits.MAX_NAME_LENGTH][com.davils.kore.core.Limits.MAX_NAME_LENGTH]; the value must not contain the NUL
     * character or be longer than [Limits.MAX_ARGUMENT_LENGTH][com.davils.kore.core.Limits.MAX_ARGUMENT_LENGTH]. A
     * variable must not be set twice or be both set and removed. Inside [prefixed], the prefix is put in front of
     * [name].
     *
     * @param name the name of the variable.
     * @param value the value of the variable.
     * @throws IllegalStateException if the builder was evaluated already.
     * @since 1.0.0
     */
    public fun variable(name: String, value: String) {
        checkOpen()
        if (!hasRoomForChange()) return
        variables += prefixedName(name) to value
    }

    /**
     * Sets every variable of [variables], like [variable] for each entry.
     *
     * @param variables the names and values to set.
     * @throws IllegalStateException if the builder was evaluated already.
     * @since 1.0.0
     */
    public fun variables(variables: Map<String, String>) {
        variables.forEach { (name, value) -> variable(name, value) }
    }

    /**
     * Hides the variables [names] of the base, of every layer and of the defaults, so that they read as not set.
     *
     * Each name follows the rules of [variable]. A variable must not be removed twice or be both removed and set.
     *
     * @param names the names of the variables.
     * @throws IllegalStateException if the builder was evaluated already.
     * @since 1.0.0
     */
    public fun remove(vararg names: String) {
        checkOpen()
        names.forEach { name ->
            if (!hasRoomForChange()) return
            removals += prefixedName(name)
        }
    }

    /**
     * Sets the variable [name] to [value] unless the base or a layer sets it: a default comes last.
     *
     * The name and the value follow the rules of [variable]. A variable must not be defaulted twice or be both
     * defaulted and removed.
     *
     * @param name the name of the variable.
     * @param value the value it has when nothing else sets it.
     * @throws IllegalStateException if the builder was evaluated already.
     * @since 1.0.0
     */
    public fun default(name: String, value: String) {
        checkOpen()
        if (!hasRoomForChange()) return
        defaults += prefixedName(name) to value
    }

    /**
     * Declares a default for every entry of [defaults], like [default] for each.
     *
     * @param defaults the names and default values.
     * @throws IllegalStateException if the builder was evaluated already.
     * @since 1.0.0
     */
    public fun defaults(defaults: Map<String, String>) {
        defaults.forEach { (name, value) -> default(name, value) }
    }

    /**
     * Reads the variable [name] from [fallbacks], in their order, when nothing sets [name] itself, such as a
     * deprecated name that still works.
     *
     * A fallback is read like any other variable but never through another alias, so aliases cannot form a cycle.
     * The alias and its fallbacks follow the rules of [variable]; an alias lists between one and
     * [EnvironmentLimits.MAX_FALLBACKS] fallbacks and never itself, see [EnvironmentViolationCodes.INVALID_ALIAS].
     *
     * @param name the name that is read.
     * @param fallbacks the names it falls back to.
     * @throws IllegalStateException if the builder was evaluated already.
     * @since 1.0.0
     */
    public fun alias(name: String, vararg fallbacks: String) {
        checkOpen()
        if (!hasRoomForRule()) return
        aliases += prefixedName(name) to fallbacks.map(::prefixedName).immutableList()
    }

    /**
     * Restricts the variables the environment shows, see [FilterBuilder].
     *
     * @param block declares the inclusions and exclusions.
     * @throws IllegalStateException if the builder was evaluated already.
     * @since 1.0.0
     */
    public fun filter(block: FilterBuilder.() -> Unit) {
        checkOpen()
        val builder = FilterBuilder { filter ->
            if (hasRoomForRule()) filters += filter.renamed(namePrefix)
        }
        builder.block()
    }

    /**
     * Requires the variables [names] to be set when the environment is built.
     *
     * A variable that is not set then is reported with [EnvironmentViolationCodes.MISSING_REQUIRED] under
     * `required["<name>"]`. The check reads the base and the layers once, when the environment is built; a variable
     * that a live layer drops later reads as not set afterwards.
     *
     * @param names the names of the required variables.
     * @throws IllegalStateException if the builder was evaluated already.
     * @since 1.0.0
     */
    public fun require(vararg names: String) {
        checkOpen()
        names.forEach { name ->
            if (!hasRoomForRule()) return
            required += prefixedName(name)
        }
    }

    /**
     * Restricts the environment to the variables whose names start with [prefix], under their names without it, so
     * that `variable("HOME")` reads `KNOT_HOME` for the prefix `KNOT_`.
     *
     * The scope applies to the whole environment, so every other declaration is written with the prefix, which
     * [prefixed] saves writing. A name that is nothing but the prefix is not listed in [Environment.names]. The
     * prefix follows the rules of the names of [variable], and a scope must be declared at most once.
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

    /**
     * Puts [prefix] in front of every name that [block] declares, so that `prefixed("KNOT_") { variable("HOME", …) }`
     * sets `KNOT_HOME`. Blocks nest, and their prefixes add up.
     *
     * @param prefix the prefix of the names declared in [block].
     * @param block the declarations.
     * @throws IllegalStateException if the builder was evaluated already.
     * @since 1.0.0
     */
    public fun prefixed(prefix: String, block: EnvironmentBuilder.() -> Unit) {
        checkOpen()
        val outer = namePrefix
        namePrefix = outer + prefix
        try {
            block()
        } finally {
            namePrefix = outer
        }
    }

    /**
     * Compares names without regard to ASCII case, as Windows does, so that `variable("path")` reads `Path`.
     *
     * When two sources spell a name differently, the one that wins by the order of [EnvironmentBuilder] gives the
     * value and the spelling in [Environment.names]. Declaring it twice has no further effect.
     *
     * @throws IllegalStateException if the builder was evaluated already.
     * @since 1.0.0
     */
    public fun ignoreCase() {
        checkOpen()
        isCaseInsensitive = true
    }

    /**
     * Replaces the references `${NAME}` and `${NAME:-default}` in every value that is read with the value of the
     * variable `NAME` of this environment, before the scope; `$$` stands for a single `$`.
     *
     * A referenced value is expanded in turn, at most [EnvironmentLimits.MAX_EXPANSION_DEPTH] references deep, and an
     * expanded value is at most [EnvironmentLimits.MAX_EXPANDED_LENGTH] characters long; a cycle, a deeper chain, a
     * longer value and a malformed reference make the read throw an [EnvironmentException]. A `$` that does not start
     * a reference is kept. Declaring it again replaces [onMissing].
     *
     * @param onMissing what a reference to a variable that is not set expands to; [MissingReference.Fail] by default.
     * @throws IllegalStateException if the builder was evaluated already.
     * @since 1.0.0
     */
    public fun expand(onMissing: MissingReference = MissingReference.Fail) {
        checkOpen()
        expansion = onMissing
    }

    /**
     * Reads the base and every layer once, when the environment is built, instead of at every read, so that the
     * environment never changes afterwards. Declaring it twice has no further effect.
     *
     * @throws IllegalStateException if the builder was evaluated already.
     * @since 1.0.0
     */
    public fun snapshot() {
        checkOpen()
        isSnapshot = true
    }

    override fun assemble(): LayeredEnvironment = LayeredEnvironment(
        Declarations(
            base = base,
            layers = layers.immutableList(),
            changes = Changes(
                variables = variables.immutableList(),
                removals = removals.immutableList(),
                defaults = defaults.immutableList(),
            ),
            rules = Rules(
                aliases = aliases.immutableList(),
                filters = filters.immutableList(),
                required = required.immutableList(),
            ),
            options = Options(
                scopes = scopes.immutableList(),
                isCaseInsensitive = isCaseInsensitive,
                expansion = expansion,
                isSnapshot = isSnapshot,
                grammar = grammar,
            ),
            overflows = Overflows(
                hasTooManyLayers = hasTooManyLayers,
                hasTooManyChanges = hasTooManyChanges,
                hasTooManyRules = hasTooManyRules,
            ),
            dotenvProblems = dotenvProblems.immutableList(),
        ),
    )

    private fun prefixedName(name: String): String = namePrefix + name

    private fun hasRoomForLayer(): Boolean {
        hasTooManyLayers = hasTooManyLayers || layers.size >= EnvironmentLimits.MAX_LAYERS
        return !hasTooManyLayers
    }

    private fun hasRoomForChange(): Boolean {
        val changes = variables.size + removals.size + defaults.size
        hasTooManyChanges = hasTooManyChanges || changes >= EnvironmentLimits.MAX_CHANGES
        return !hasTooManyChanges
    }

    private fun hasRoomForRule(): Boolean {
        val rules = aliases.size + filters.size + required.size
        hasTooManyRules = hasTooManyRules || rules >= EnvironmentLimits.MAX_RULES
        return !hasTooManyRules
    }
}
