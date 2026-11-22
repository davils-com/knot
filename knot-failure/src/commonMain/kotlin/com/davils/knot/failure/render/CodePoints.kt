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

package com.davils.knot.failure.render

private const val HIGH_SURROGATE_START: Int = 0xD800
private const val LOW_SURROGATE_START: Int = 0xDC00
private const val SUPPLEMENTARY_START: Int = 0x10000
private const val SURROGATE_SHIFT: Int = 10
private const val LOW_BITS: Int = 0x3FF

internal fun codePointAt(text: String, index: Int): Int {
    val high = text[index]
    val low = text.getOrNull(index + 1)
    if (low == null || !high.isHighSurrogate() || !low.isLowSurrogate()) return high.code
    val highBits = high.code - HIGH_SURROGATE_START
    val lowBits = low.code - LOW_SURROGATE_START
    return highBits.shl(SURROGATE_SHIFT) + lowBits + SUPPLEMENTARY_START
}

internal fun unitsOf(codePoint: Int): List<Char> {
    if (codePoint < SUPPLEMENTARY_START) return listOf(codePoint.toChar())
    val offset = codePoint - SUPPLEMENTARY_START
    val high = (HIGH_SURROGATE_START + (offset shr SURROGATE_SHIFT)).toChar()
    val low = (LOW_SURROGATE_START + (offset and LOW_BITS)).toChar()
    return listOf(high, low)
}
