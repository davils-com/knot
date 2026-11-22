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

package com.davils.knot.environment.schema

import com.davils.knot.environment.Environment
import com.davils.knot.environment.EnvironmentException
import com.davils.knot.environment.EnvironmentViolationCodes
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.failure.contained
import com.davils.kore.core.text.SafeText
import com.davils.kore.core.validation.Validatable
import com.davils.kore.core.validation.ValidationScope
import com.davils.kore.core.validation.violation.ViolationCode

private const val MISSING_MESSAGE: String = "is required but not set"
private const val UNKNOWN_MESSAGE: String = "is not declared by the schema"
private const val TOO_MANY_MESSAGE: String = "must not declare more than the settings the limit allows"
private const val DUPLICATE_MESSAGE: String = "is declared more than once"
private const val EXPANSION_MESSAGE: String = "cannot be read from the environment"

internal class SettingsCheck(
    private val schema: EnvironmentSchema,
    private val environment: Environment,
) : Validatable {
    private val settings: Nothing? = null
    private val problems: MutableMap<String, Pair<ViolationCode, String>> = linkedMapOf()
    val values: MutableMap<Setting<*>, Any> = linkedMapOf()

    override fun ValidationScope.validate() {
        if (schema.hasTooManySettings) fail(::settings, TOO_MANY_MESSAGE, EnvironmentViolationCodes.INVALID_SCHEMA)
        schema.duplicates.forEach { name ->
            problems[name] =
                EnvironmentViolationCodes.INVALID_SCHEMA to DUPLICATE_MESSAGE
        }
        schema.settings.forEach { setting -> read(setting) }
        if (schema.isStrict) unknownNames().forEach { name -> problems[name] = UNKNOWN to UNKNOWN_MESSAGE }
        each(::settings, problems) { (code, message) -> fail(message, code) }
    }

    private fun <T : Any> read(setting: Setting<T>) {
        val read = contained { environment.variable(setting.name) }
        val failure = read.exceptionOrNull()
        if (failure != null) return problem(setting, codeOf(failure), EXPANSION_MESSAGE)
        store(setting, read.getOrNull())
    }

    private fun <T : Any> store(setting: Setting<T>, text: String?) {
        if (text == null) return storeDefault(setting)
        converted(setting, text)?.let { value -> values[setting] = value }
    }

    private fun storeDefault(setting: Setting<*>) {
        val default = setting.default
        if (default == null) return missing(setting)
        values[setting] = default
    }

    private fun <T : Any> converted(setting: Setting<T>, raw: String): T? {
        val converted = contained { setting.converter.convert(raw) }.getOrNull()
        val refusal = refusalOf(setting, converted)
        if (refusal != null) problem(setting, refusal.first, refusal.second)
        return converted.takeIf { refusal == null }
    }

    private fun <T : Any> refusalOf(setting: Setting<T>, converted: T?): Pair<ViolationCode, String>? {
        if (converted == null) return EnvironmentViolationCodes.UNREADABLE_VALUE to unreadableMessage(setting)
        val refusal = setting.rules.firstOrNull { rule -> !isAccepted(rule, converted) }
        return refusal?.let { rule -> EnvironmentViolationCodes.REJECTED_VALUE to SafeText.text(rule.message) }
    }

    private fun <T : Any> isAccepted(rule: SettingRule<T>, value: T): Boolean {
        val accepted = contained { rule.accepts(value) }
        return accepted.getOrDefault(false)
    }

    private fun unreadableMessage(setting: Setting<*>): String {
        val typeName = SafeText.quoted(setting.typeName)
        return "cannot be read as $typeName"
    }

    private fun codeOf(failure: Throwable): ViolationCode {
        if (failure is EnvironmentException) return failure.code
        return EnvironmentViolationCodes.UNREADABLE_VALUE
    }

    private fun missing(setting: Setting<*>) {
        if (setting.isRequired) problem(setting, EnvironmentViolationCodes.MISSING_REQUIRED, MISSING_MESSAGE)
    }

    private fun problem(setting: Setting<*>, code: ViolationCode, message: String) {
        problems.getOrPut(setting.name) { code to message }
    }

    private fun unknownNames(): List<String> {
        val declared = schema.settings.map { setting -> setting.name }.toSet()
        val prefix = schema.prefix
        val names = environment.names
        val unknown = names.filter { name -> name.startsWith(prefix) && name !in declared }
        return unknown.sorted()
    }

    private companion object {
        val UNKNOWN: ViolationCode = EnvironmentViolationCodes.UNKNOWN_VARIABLE
    }
}
