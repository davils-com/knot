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

private const val CASE_DISTANCE: Int = 'a' - 'A'

internal fun foldedName(name: String): String = buildString(name.length) {
    name.forEach { character -> append(foldedCharacter(character)) }
}

internal fun Environment.foldedVariable(name: String): String? {
    val exact = variable(name)
    if (exact != null) return exact
    val folded = foldedName(name)
    return names.firstOrNull { candidate -> foldedName(candidate) == folded }?.let(::variable)
}

private fun foldedCharacter(character: Char): Char {
    if (character !in 'a'..'z') return character
    return character - CASE_DISTANCE
}
