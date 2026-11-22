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

import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.text.SafeText

private const val FOREIGN_MESSAGE: String = "The setting is not declared by the schema these settings were loaded from"
private const val ABSENT_MESSAGE: String = "The setting is neither set nor defaulted"

/**
 * The values of the settings of an [EnvironmentSchema], read and checked by [EnvironmentSchema.load].
 *
 * The values were read once, when the settings were loaded, and never change. The settings are safe to share between
 * threads. The `toString` shows the names of the settings and `****` in place of every value.
 *
 * @since 1.0.0
 */
public class EnvironmentSettings internal constructor(
    private val schema: EnvironmentSchema,
    private val values: Map<Setting<*>, Any>,
) {
    /**
     * Returns the value of [setting]: the converted value of its variable or its default.
     *
     * @param T the type of the value.
     * @param setting a setting of the schema these settings were loaded from.
     * @return the value, or `null` if the variable is not set and the setting has no default.
     * @throws IllegalArgumentException if [setting] is not declared by that schema.
     * @since 1.0.0
     */
    public operator fun <T : Any> get(setting: Setting<T>): T? {
        require(schema.declares(setting)) { FOREIGN_MESSAGE }
        @Suppress("UNCHECKED_CAST")
        return values[setting] as T?
    }

    /**
     * Returns the value of [setting] and refuses to go on without one; a required setting or one with a default
     * always has one after a successful [EnvironmentSchema.load].
     *
     * @param T the type of the value.
     * @param setting a setting of the schema these settings were loaded from.
     * @return the value.
     * @throws IllegalArgumentException if [setting] is not declared by that schema.
     * @throws IllegalStateException if the setting has no value.
     * @since 1.0.0
     */
    public fun <T : Any> require(setting: Setting<T>): T = checkNotNull(get(setting)) { ABSENT_MESSAGE }

    /**
     * Returns the names of the settings that have a value, escaped, with `****` in place of every value.
     *
     * @return the masked settings, such as `{HOST=****, PORT=****}`.
     * @since 1.0.0
     */
    override fun toString(): String {
        val names = values.keys.map { setting -> setting.name }.sorted()
        return names.joinToString(prefix = "{", postfix = "}") { name -> "${SafeText.text(name)}=${SafeText.REDACTED}" }
    }
}
