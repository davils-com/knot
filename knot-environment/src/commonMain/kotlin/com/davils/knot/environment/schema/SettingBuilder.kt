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

package com.davils.knot.environment.schema

import com.davils.kore.core.annotation.DavilsDsl

/**
 * Declares how a [Setting] is read, for the declaring functions of [EnvironmentSchema].
 *
 * @param T the type of the value of the setting.
 * @since 1.0.0
 */
@DavilsDsl
public class SettingBuilder<T : Any> internal constructor() {
    internal val rules: MutableList<SettingRule<T>> = mutableListOf()
    internal var isRequired: Boolean = false
    internal var isSecret: Boolean = false

    /**
     * The value when the variable is not set; `null` for none. A setting with a default is never missing.
     *
     * @since 1.0.0
     */
    public var default: T? = null

    /**
     * What the setting is for, as [EnvironmentSchema.describe] shows it; empty by default.
     *
     * @since 1.0.0
     */
    public var description: String = ""

    /**
     * Makes the setting required: loading fails with `MISSING_REQUIRED` of
     * [EnvironmentViolationCodes][com.davils.knot.environment.EnvironmentViolationCodes] when the variable is not set
     * and the setting has no [default].
     *
     * @since 1.0.0
     */
    public fun required() {
        isRequired = true
    }

    /**
     * Marks the setting as a secret, so that [EnvironmentSchema.describe] never shows its default.
     *
     * The value of a setting never appears in a message or a `toString` anyway; this protects the default that the
     * schema declares.
     *
     * @since 1.0.0
     */
    public fun secret() {
        isSecret = true
    }

    /**
     * Refuses every value for which [accepts] is `false`, with `REJECTED_VALUE` of
     * [EnvironmentViolationCodes][com.davils.knot.environment.EnvironmentViolationCodes] and [message], such as
     * `accept("must be a port") { port -> port in 1..65535 }`. Rules apply to read values, not to the [default], in
     * the order they are declared; the first that refuses is reported.
     *
     * @param message what the value must be, shown escaped and never with the value.
     * @param accepts tells whether a value is accepted; it is contained, and a failure counts as a refusal.
     * @since 1.0.0
     */
    public fun accept(message: String, accepts: (T) -> Boolean) {
        rules += SettingRule(message, accepts)
    }
}
