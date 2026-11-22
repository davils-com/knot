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

internal class NameFilter(val text: String, val isPrefix: Boolean, val isInclusion: Boolean) {
    fun renamed(prefix: String): NameFilter =
        NameFilter(text = prefix + text, isPrefix = isPrefix, isInclusion = isInclusion)

    fun matches(name: String, fold: (String) -> String): Boolean {
        val folded = fold(name)
        val pattern = fold(text)
        if (isPrefix) return folded.startsWith(pattern)
        return folded == pattern
    }
}
