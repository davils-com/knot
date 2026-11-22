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

package com.davils.knot.shutdown

private const val UNKNOWN_TYPE: String = "Throwable"
private val namePunctuation: Set<Char> = setOf('.', '_', ':', '-')

internal fun typeNameOf(failure: Throwable): String = failure::class.simpleName ?: UNKNOWN_TYPE

internal fun isValidName(name: String): Boolean {
    val first = name.firstOrNull()
    val isBounded = name.length <= ShutdownLimits.MAX_NAME_LENGTH
    return isBounded && first != null && isAsciiAlphanumeric(first) && name.all(::isNameCharacter)
}

private fun isNameCharacter(character: Char): Boolean = isAsciiAlphanumeric(character) || character in namePunctuation

private fun isAsciiAlphanumeric(character: Char): Boolean =
    character in 'a'..'z' || character in 'A'..'Z' || character in '0'..'9'
