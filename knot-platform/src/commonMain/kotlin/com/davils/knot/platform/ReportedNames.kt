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

package com.davils.knot.platform

import com.davils.kore.core.Limits

private const val FIRST_PRINTABLE: Char = ' '
private const val LAST_PRINTABLE: Char = '~'

internal fun comparableName(reported: String): String? {
    val isComparable = reported.length <= Limits.MAX_NAME_LENGTH && reported.all(::isPrintableAscii)
    if (!isComparable) return null
    val trimmed = reported.trim()
    return trimmed.lowercase()
}

private fun isPrintableAscii(character: Char): Boolean = character in FIRST_PRINTABLE..LAST_PRINTABLE
