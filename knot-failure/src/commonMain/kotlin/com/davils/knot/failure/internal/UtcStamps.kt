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

package com.davils.knot.failure.internal

import kotlin.time.Instant

private const val DATE_END: Int = 10
private const val TIME_START: Int = 11
private const val TIME_END: Int = 19
private const val NANOS_PER_MILLI: Int = 1_000_000
private const val MILLI_DIGITS: Int = 3
private const val DATE_SEPARATOR: String = "-"
private const val TIME_SEPARATOR: String = ":"

internal fun utcStampOf(at: Instant): String {
    val iso = at.toString()
    val dateText = iso.substring(startIndex = 0, endIndex = DATE_END)
    val timeText = iso.substring(startIndex = TIME_START, endIndex = TIME_END)
    val date = dateText.replace(oldValue = DATE_SEPARATOR, newValue = "")
    val time = timeText.replace(oldValue = TIME_SEPARATOR, newValue = "")
    val millis = (at.nanosecondsOfSecond / NANOS_PER_MILLI).toString().padStart(MILLI_DIGITS, '0')
    return "$date-$time-$millis"
}
