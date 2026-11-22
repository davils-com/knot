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

private const val UNDERSCORE: Char = '_'

/**
 * The grammar the names declared in an [environment] must follow, on top of the rules every name follows.
 *
 * Every name must not be blank or contain `=` or the NUL character. [Posix] additionally asks for the portable names of
 * POSIX shells, which every operating system and shell accepts.
 *
 * @since 1.0.0
 */
public enum class NameGrammar {
    /**
     * Every name that an operating system keeps; the default.
     *
     * @since 1.0.0
     */
    Lenient,

    /**
     * A letter or an underscore first, then letters, digits and underscores, all ASCII, such as `KNOT_HOME`.
     *
     * @since 1.0.0
     */
    Posix,
    ;

    internal fun accepts(name: String): Boolean {
        if (this == Lenient) return true
        val first = name.firstOrNull()
        return first != null && isPosixStart(first) && name.all(::isPosixPart)
    }

    private fun isPosixStart(character: Char): Boolean = character == UNDERSCORE || isAsciiLetter(character)

    private fun isPosixPart(character: Char): Boolean = isPosixStart(character) || character in '0'..'9'

    private fun isAsciiLetter(character: Char): Boolean = character in 'a'..'z' || character in 'A'..'Z'
}
