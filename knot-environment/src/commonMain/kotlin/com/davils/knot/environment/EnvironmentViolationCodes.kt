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

import com.davils.kore.core.validation.violation.ViolationCode

/**
 * The codes that the validation of an [EnvironmentBuilder], a read of an [Environment] and the loading of an
 * [EnvironmentSchema][com.davils.knot.environment.schema.EnvironmentSchema] report.
 *
 * Every code is a stable name in the `knot.environment` namespace, so a client can react to it without parsing the
 * message of a violation. No violation and no [EnvironmentException] ever contains the value of a variable. A refused
 * name is never shown either; the violation is reported under `variables`, `removals`, `defaults`, `aliases`,
 * `filters`, `required` or `scope` without it. A valid name appears only as the key of a path, such as
 * `variables["HOME"]`, escaped wherever it is shown.
 *
 * @since 1.0.0
 */
public object EnvironmentViolationCodes {
    /**
     * A name is blank, contains `=` or the NUL character, or breaks the declared
     * [NameGrammar][com.davils.knot.environment.NameGrammar].
     *
     * @since 1.0.0
     */
    public val INVALID_NAME: ViolationCode = ViolationCode("knot.environment.invalid-name")

    /**
     * A name is longer than [Limits.MAX_NAME_LENGTH][com.davils.kore.core.Limits.MAX_NAME_LENGTH], reported without
     * the name, a value is longer than [Limits.MAX_ARGUMENT_LENGTH][com.davils.kore.core.Limits.MAX_ARGUMENT_LENGTH],
     * reported under the name of its variable, or a dotenv text is longer than
     * [EnvironmentLimits.MAX_DOTENV_LENGTH] or [EnvironmentLimits.MAX_DOTENV_LINES].
     *
     * @since 1.0.0
     */
    public val TOO_LONG: ViolationCode = ViolationCode("knot.environment.too-long")

    /**
     * A value contains the NUL character, which no operating system keeps in an environment.
     *
     * @since 1.0.0
     */
    public val INVALID_VALUE: ViolationCode = ViolationCode("knot.environment.invalid-value")

    /**
     * A variable is overridden, removed, defaulted, aliased or required twice.
     *
     * @since 1.0.0
     */
    public val DUPLICATE_CHANGE: ViolationCode = ViolationCode("knot.environment.duplicate-change")

    /**
     * A variable is both overridden and removed, or both defaulted and removed; reported under `removals["<name>"]`.
     *
     * @since 1.0.0
     */
    public val CONFLICTING_CHANGE: ViolationCode = ViolationCode("knot.environment.conflicting-change")

    /**
     * More than [EnvironmentLimits.MAX_LAYERS] layers were declared; reported once under `layers`.
     *
     * @since 1.0.0
     */
    public val TOO_MANY_LAYERS: ViolationCode = ViolationCode("knot.environment.too-many-layers")

    /**
     * More than [EnvironmentLimits.MAX_CHANGES] changes were declared; reported once under `variables`.
     *
     * @since 1.0.0
     */
    public val TOO_MANY_CHANGES: ViolationCode = ViolationCode("knot.environment.too-many-changes")

    /**
     * More than [EnvironmentLimits.MAX_RULES] rules were declared; reported once under `aliases`.
     *
     * @since 1.0.0
     */
    public val TOO_MANY_RULES: ViolationCode = ViolationCode("knot.environment.too-many-rules")

    /**
     * A scope was declared more than once; reported under `scope`.
     *
     * @since 1.0.0
     */
    public val REPEATED_SCOPE: ViolationCode = ViolationCode("knot.environment.repeated-scope")

    /**
     * A line of a dotenv text cannot be read; reported under `layers` with the number of the line and never its text.
     *
     * @since 1.0.0
     */
    public val INVALID_DOTENV: ViolationCode = ViolationCode("knot.environment.invalid-dotenv")

    /**
     * An alias names itself as a fallback, lists no fallback or more than [EnvironmentLimits.MAX_FALLBACKS].
     *
     * @since 1.0.0
     */
    public val INVALID_ALIAS: ViolationCode = ViolationCode("knot.environment.invalid-alias")

    /**
     * A required variable or setting is not set.
     *
     * @since 1.0.0
     */
    public val MISSING_REQUIRED: ViolationCode = ViolationCode("knot.environment.missing-required")

    /**
     * The value of a variable cannot be converted to the type that is read from it.
     *
     * @since 1.0.0
     */
    public val UNREADABLE_VALUE: ViolationCode = ViolationCode("knot.environment.unreadable-value")

    /**
     * The value of a setting was converted but refused by a rule of the setting.
     *
     * @since 1.0.0
     */
    public val REJECTED_VALUE: ViolationCode = ViolationCode("knot.environment.rejected-value")

    /**
     * A strict [EnvironmentSchema][com.davils.knot.environment.schema.EnvironmentSchema] found a variable under its
     * prefix that it does not declare.
     *
     * @since 1.0.0
     */
    public val UNKNOWN_VARIABLE: ViolationCode = ViolationCode("knot.environment.unknown-variable")

    /**
     * More than [EnvironmentLimits.MAX_SETTINGS] settings were declared, or a setting was declared twice.
     *
     * @since 1.0.0
     */
    public val INVALID_SCHEMA: ViolationCode = ViolationCode("knot.environment.invalid-schema")

    /**
     * An expansion refers to a variable that is not set, while [MissingReference.Fail] is in effect.
     *
     * @since 1.0.0
     */
    public val UNRESOLVED_REFERENCE: ViolationCode = ViolationCode("knot.environment.unresolved-reference")

    /**
     * An expansion refers back to a variable it is expanding.
     *
     * @since 1.0.0
     */
    public val EXPANSION_CYCLE: ViolationCode = ViolationCode("knot.environment.expansion-cycle")

    /**
     * An expansion follows more than [EnvironmentLimits.MAX_EXPANSION_DEPTH] references or produces more than
     * [EnvironmentLimits.MAX_EXPANDED_LENGTH] characters.
     *
     * @since 1.0.0
     */
    public val EXPANSION_TOO_LARGE: ViolationCode = ViolationCode("knot.environment.expansion-too-large")

    /**
     * A value holds a reference that is not written as `${NAME}` or `${NAME:-default}`.
     *
     * @since 1.0.0
     */
    public val INVALID_REFERENCE: ViolationCode = ViolationCode("knot.environment.invalid-reference")
}
