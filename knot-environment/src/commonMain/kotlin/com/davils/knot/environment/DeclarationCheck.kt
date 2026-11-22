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

package com.davils.knot.environment

import com.davils.kore.core.validation.ValidationScope
import com.davils.kore.core.validation.violation.ViolationCode
import kotlin.reflect.KProperty

private const val TOO_MANY_LAYERS_MESSAGE: String =
    "must not stack more than ${EnvironmentLimits.MAX_LAYERS} layers"
private const val TOO_MANY_CHANGES_MESSAGE: String =
    "must not declare more than ${EnvironmentLimits.MAX_CHANGES} overrides, removals and defaults"
private const val TOO_MANY_RULES_MESSAGE: String =
    "must not declare more than ${EnvironmentLimits.MAX_RULES} aliases, filters and required variables"
private const val DUPLICATE_MESSAGE: String = "is declared more than once"
private const val OVERRIDE_CONFLICT_MESSAGE: String = "is both overridden and removed"
private const val DEFAULT_CONFLICT_MESSAGE: String = "is both defaulted and removed"
private const val REPEATED_SCOPE_MESSAGE: String = "is declared more than once"
private const val SELF_ALIAS_MESSAGE: String = "must not name itself as a fallback"
private const val FALLBACK_COUNT_MESSAGE: String =
    "must list between 1 and ${EnvironmentLimits.MAX_FALLBACKS} fallbacks"
private const val GRAMMAR_MESSAGE: String =
    "a name must start with an ASCII letter or an underscore and hold only ASCII letters, digits and underscores"

internal class DeclarationCheck(private val declarations: Declarations) {
    private val layers: Nothing? = null
    private val variables: Nothing? = null
    private val removals: Nothing? = null
    private val defaults: Nothing? = null
    private val aliases: Nothing? = null
    private val filters: Nothing? = null
    private val required: Nothing? = null
    private val scope: Nothing? = null
    private val changes: Changes = declarations.changes
    private val rules: Rules = declarations.rules
    private val grammar: NameGrammar = declarations.options.grammar
    private val fold: (String) -> String = foldingOf(declarations.options.isCaseInsensitive)

    fun ValidationScope.check() {
        checkOverflows()
        checkDotenv()
        checkOverrides()
        checkRemovals()
        checkDefaults()
        checkAliases()
        validNames(::filters, rules.filters.map { filter -> filter.text })
        failDuplicates(::required, validNames(::required, rules.required))
        checkScope()
    }

    private fun ValidationScope.checkOverflows() {
        val overflows = declarations.overflows
        if (overflows.hasTooManyLayers) {
            fail(
                ::layers,
                TOO_MANY_LAYERS_MESSAGE,
                EnvironmentViolationCodes.TOO_MANY_LAYERS,
            )
        }
        if (overflows.hasTooManyChanges) {
            fail(::variables, TOO_MANY_CHANGES_MESSAGE, EnvironmentViolationCodes.TOO_MANY_CHANGES)
        }
        if (overflows.hasTooManyRules) fail(::aliases, TOO_MANY_RULES_MESSAGE, EnvironmentViolationCodes.TOO_MANY_RULES)
    }

    private fun ValidationScope.checkDotenv() {
        val (tooLong, unreadable) = declarations.dotenvProblems.partition { problem -> problem.isTooLong }
        tooLong.forEach { problem -> fail(::layers, problem.reason, EnvironmentViolationCodes.TOO_LONG) }
        unreadable.forEach { problem ->
            fail(::layers, "line ${problem.line} ${problem.reason}", EnvironmentViolationCodes.INVALID_DOTENV)
        }
    }

    private fun ValidationScope.checkOverrides() {
        val valid = checkedValues(::variables, changes.variables)
        failDuplicates(::variables, valid)
    }

    private fun ValidationScope.checkDefaults() {
        val valid = checkedValues(::defaults, changes.defaults)
        failDuplicates(::defaults, valid)
    }

    private fun ValidationScope.checkRemovals() {
        val valid = validNames(::removals, changes.removals)
        failDuplicates(::removals, valid)
        val overridden = changes.variables.map { (name, _) -> fold(name) }.toSet()
        val defaulted = changes.defaults.map { (name, _) -> fold(name) }.toSet()
        val conflicts = valid.mapNotNull { name ->
            conflictOf(
                name,
                overridden,
                defaulted,
            )?.let { message -> name to message }
        }
        each(::removals, conflicts.toMap()) { message -> fail(message, EnvironmentViolationCodes.CONFLICTING_CHANGE) }
    }

    private fun conflictOf(name: String, overridden: Set<String>, defaulted: Set<String>): String? {
        if (fold(name) in overridden) return OVERRIDE_CONFLICT_MESSAGE
        return DEFAULT_CONFLICT_MESSAGE.takeIf { fold(name) in defaulted }
    }

    private fun ValidationScope.checkAliases() {
        val names = validNames(::aliases, rules.aliases.map { (name, _) -> name })
        failDuplicates(::aliases, names)
        val valid = rules.aliases.filter { (name, _) -> name in names }
        valid.forEach { (_, fallbacks) -> validNames(::aliases, fallbacks) }
        val refusals = valid.mapNotNull { (name, fallbacks) ->
            aliasRefusal(
                name,
                fallbacks,
            )?.let { message -> name to message }
        }
        each(::aliases, refusals.toMap()) { message -> fail(message, EnvironmentViolationCodes.INVALID_ALIAS) }
    }

    private fun aliasRefusal(name: String, fallbacks: List<String>): String? {
        if (fallbacks.isEmpty() || fallbacks.size > EnvironmentLimits.MAX_FALLBACKS) return FALLBACK_COUNT_MESSAGE
        return SELF_ALIAS_MESSAGE.takeIf { fallbacks.any { fallback -> fold(fallback) == fold(name) } }
    }

    private fun ValidationScope.checkScope() {
        val scopes = declarations.options.scopes
        if (scopes.size > 1) fail(::scope, REPEATED_SCOPE_MESSAGE, EnvironmentViolationCodes.REPEATED_SCOPE)
        val prefix = scopes.firstOrNull() ?: return
        val refusal = nameRefusal(prefix) ?: return
        fail(::scope, refusal.message, refusal.code)
    }

    private fun ValidationScope.checkedValues(property: KProperty<*>, pairs: List<Pair<String, String>>): List<String> {
        val valid = validNames(property, pairs.map { (name, _) -> name })
        val validSet = valid.toSet()
        val refusals = pairs
            .filter { (name, _) -> name in validSet }
            .mapNotNull { (name, value) -> valueRefusal(value)?.let { refusal -> name to refusal } }
        each(property, refusals.toMap()) { refusal -> fail(refusal.message, refusal.code) }
        return valid
    }

    private fun ValidationScope.validNames(property: KProperty<*>, names: List<String>): List<String> {
        val valid = mutableListOf<String>()
        names.forEach { name ->
            val refusal = refusalOf(name)
            if (refusal == null) valid += name
            refusal?.let { (message, code) -> fail(property, message, code) }
        }
        return valid
    }

    private fun refusalOf(name: String): Pair<String, ViolationCode>? {
        val refusal = nameRefusal(name)
        if (refusal != null) return refusal.message to refusal.code
        return (GRAMMAR_MESSAGE to EnvironmentViolationCodes.INVALID_NAME).takeUnless { grammar.accepts(name) }
    }

    private fun ValidationScope.failDuplicates(property: KProperty<*>, names: List<String>) {
        val counts = names.groupingBy(fold).eachCount()
        val duplicated = names.filter { name -> counts.getValue(fold(name)) > 1 }.distinctBy(fold)
        val duplicates = duplicated.associateWith { DUPLICATE_MESSAGE }
        each(property, duplicates) { message -> fail(message, EnvironmentViolationCodes.DUPLICATE_CHANGE) }
    }
}

private fun foldingOf(isCaseInsensitive: Boolean): (String) -> String {
    if (isCaseInsensitive) return ::foldedName
    return { name -> name }
}
