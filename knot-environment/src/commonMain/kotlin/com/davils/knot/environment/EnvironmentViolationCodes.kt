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
 * The codes that the validation of an [EnvironmentBuilder] reports.
 *
 * Every code is a stable name in the `knot.environment` namespace, so a client can react to it without parsing the
 * message of a violation. No violation ever contains the value of a variable. A refused name is never shown either;
 * the violation is reported under `variables`, `removals` or `scope` without it. A valid name appears only as the
 * key of a path, such as `variables["HOME"]`, escaped wherever it is shown.
 *
 * @since 1.0.0
 */
public object EnvironmentViolationCodes {
    /**
     * A name is blank or contains `=` or the NUL character; reported under `variables`, `removals` or `scope`.
     *
     * @since 1.0.0
     */
    public val INVALID_NAME: ViolationCode = ViolationCode("knot.environment.invalid-name")

    /**
     * A name is longer than [Limits.MAX_NAME_LENGTH][com.davils.kore.core.Limits.MAX_NAME_LENGTH], reported without
     * the name, or a value is longer than
     * [Limits.MAX_ARGUMENT_LENGTH][com.davils.kore.core.Limits.MAX_ARGUMENT_LENGTH], reported under the name of its
     * variable.
     *
     * @since 1.0.0
     */
    public val TOO_LONG: ViolationCode = ViolationCode("knot.environment.too-long")

    /**
     * A value contains the NUL character, which no operating system keeps in an environment; reported under
     * `variables["<name>"]`.
     *
     * @since 1.0.0
     */
    public val INVALID_VALUE: ViolationCode = ViolationCode("knot.environment.invalid-value")

    /**
     * A variable is overridden twice or removed twice; reported under `variables["<name>"]` or `removals["<name>"]`.
     *
     * @since 1.0.0
     */
    public val DUPLICATE_CHANGE: ViolationCode = ViolationCode("knot.environment.duplicate-change")

    /**
     * A variable is both overridden and removed; reported under `removals["<name>"]`.
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
     * More than [EnvironmentLimits.MAX_CHANGES] overrides and removals were declared; reported once under
     * `variables`.
     *
     * @since 1.0.0
     */
    public val TOO_MANY_CHANGES: ViolationCode = ViolationCode("knot.environment.too-many-changes")

    /**
     * A scope was declared more than once; reported under `scope`.
     *
     * @since 1.0.0
     */
    public val REPEATED_SCOPE: ViolationCode = ViolationCode("knot.environment.repeated-scope")
}
