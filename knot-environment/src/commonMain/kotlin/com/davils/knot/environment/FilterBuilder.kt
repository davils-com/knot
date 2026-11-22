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

import com.davils.kore.core.annotation.DavilsDsl

/**
 * Declares which variables an environment shows, for [EnvironmentBuilder.filter].
 *
 * Without an inclusion every variable is shown; with one, only the variables an inclusion names. An exclusion hides a
 * variable whatever includes it. Names and prefixes are compared exactly, or without regard to ASCII case after
 * [EnvironmentBuilder.ignoreCase], and never as patterns, so no declaration can make a read slow. Every name follows
 * the rules of [EnvironmentBuilder.variable] and counts against [EnvironmentLimits.MAX_RULES].
 *
 * @since 1.0.0
 */
@DavilsDsl
public class FilterBuilder internal constructor(private val add: (NameFilter) -> Unit) {
    /**
     * Shows the variables [names], and hides every variable that no inclusion names.
     *
     * @param names the names of the variables to show.
     * @since 1.0.0
     */
    public fun include(vararg names: String) {
        names.forEach { name -> add(NameFilter(text = name, isPrefix = false, isInclusion = true)) }
    }

    /**
     * Shows the variables whose names start with [prefix], and hides every variable that no inclusion names.
     *
     * @param prefix the prefix of the names to show.
     * @since 1.0.0
     */
    public fun includePrefix(prefix: String) {
        add(NameFilter(text = prefix, isPrefix = true, isInclusion = true))
    }

    /**
     * Hides the variables [names].
     *
     * @param names the names of the variables to hide.
     * @since 1.0.0
     */
    public fun exclude(vararg names: String) {
        names.forEach { name -> add(NameFilter(text = name, isPrefix = false, isInclusion = false)) }
    }

    /**
     * Hides the variables whose names start with [prefix].
     *
     * @param prefix the prefix of the names to hide.
     * @since 1.0.0
     */
    public fun excludePrefix(prefix: String) {
        add(NameFilter(text = prefix, isPrefix = true, isInclusion = false))
    }
}
