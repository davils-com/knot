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

private const val REQUIRED: String = "required"
private const val OPTIONAL: String = "optional"

/**
 * The description of one [Setting], for documentation and help output, as [EnvironmentSchema.describe] gives it.
 *
 * It never holds the value of a variable. Every text is escaped through
 * [SafeText][com.davils.kore.core.text.SafeText], so it is safe to show.
 *
 * @since 1.0.0
 */
public class SettingDescription internal constructor(setting: Setting<*>) {
    /**
     * The full name of the variable, escaped.
     *
     * @since 1.0.0
     */
    public val name: String = SafeText.text(setting.name)

    /**
     * The name of the type of the value, escaped.
     *
     * @since 1.0.0
     */
    public val typeName: String = SafeText.text(setting.typeName)

    /**
     * Whether the setting is required.
     *
     * @since 1.0.0
     */
    public val isRequired: Boolean = setting.isRequired

    /**
     * The default, escaped, `****` for a secret setting, or `null` without a default.
     *
     * @since 1.0.0
     */
    public val defaultText: String? = setting.default?.let { default -> shownDefault(setting, default) }

    /**
     * What the setting is for, escaped.
     *
     * @since 1.0.0
     */
    public val description: String = SafeText.text(setting.description)

    /**
     * Whether the setting is a secret.
     *
     * @since 1.0.0
     */
    public val isSecret: Boolean = setting.isSecret

    private val facts: List<Any?> get() = listOf(name, typeName, isRequired, defaultText, description, isSecret)

    /**
     * Tells whether [other] describes a setting the same way.
     *
     * @param other the object to compare with.
     * @return `true` if [other] is an equal description.
     * @since 1.0.0
     */
    override fun equals(other: Any?): Boolean = other is SettingDescription && facts == other.facts

    /**
     * Returns a hash code consistent with [equals].
     *
     * @return the hash code.
     * @since 1.0.0
     */
    override fun hashCode(): Int = facts.hashCode()

    /**
     * Returns the description in one line, such as `PORT (int, default 8080): the port to listen on`.
     *
     * @return the description.
     * @since 1.0.0
     */
    override fun toString(): String {
        val requirement = REQUIRED.takeIf { isRequired } ?: OPTIONAL
        val shownDefault = defaultText?.let { text -> ", default $text" }.orEmpty()
        return "$name ($typeName, $requirement$shownDefault): $description"
    }

    private fun shownDefault(setting: Setting<*>, default: Any): String {
        if (setting.isSecret) return SafeText.REDACTED
        return SafeText.text(default.toString())
    }
}
