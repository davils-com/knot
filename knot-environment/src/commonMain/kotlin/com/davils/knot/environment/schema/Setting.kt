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
import com.davils.kore.core.arguments.conversion.Converter
import com.davils.kore.core.collection.immutableList
import com.davils.kore.core.text.SafeText

/**
 * One environment variable that an [EnvironmentSchema] declares, typed, with its default and rules; a key into
 * [EnvironmentSettings].
 *
 * @param T the type of the value.
 * @property name the full name of the variable, the prefix of the schema included.
 * @param converter converts the text of the value.
 * @param builder the declaration of the default, the description and the rules.
 * @since 1.0.0
 */
public class Setting<T : Any> internal constructor(
    public val name: String,
    internal val converter: Converter<T>,
    builder: SettingBuilder<T>,
) {
    /**
     * The value when the variable is not set, or `null` for none.
     *
     * @since 1.0.0
     */
    public val default: T? = builder.default

    /**
     * What the setting is for.
     *
     * @since 1.0.0
     */
    public val description: String = builder.description

    /**
     * Whether loading fails when the variable is not set and there is no [default].
     *
     * @since 1.0.0
     */
    public val isRequired: Boolean = builder.isRequired

    /**
     * Whether [EnvironmentSchema.describe] hides the [default].
     *
     * @since 1.0.0
     */
    public val isSecret: Boolean = builder.isSecret

    internal val rules: List<SettingRule<T>> = builder.rules.immutableList()

    /**
     * The name of the type of the value, as the converter names it, such as `int`.
     *
     * @since 1.0.0
     */
    public val typeName: String get() = converter.typeName

    /**
     * Returns the name and the type of the setting, escaped.
     *
     * @return the setting, such as `Setting(PORT: int)`.
     * @since 1.0.0
     */
    override fun toString(): String = "Setting(${SafeText.text(name)}: ${SafeText.text(typeName)})"
}
