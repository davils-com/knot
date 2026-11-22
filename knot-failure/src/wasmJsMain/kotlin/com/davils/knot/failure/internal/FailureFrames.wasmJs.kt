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

private const val FRAME_PREFIX: String = "at "
private const val UNKNOWN_TYPE: String = "Throwable"

internal actual fun qualifiedTypeOf(cause: Throwable): String = cause::class.qualifiedName ?: UNKNOWN_TYPE

internal actual fun framesOf(cause: Throwable): List<String> {
    val text = cause.stackTraceToString()
    val lines = text.lines().map(String::trim)
    return lines.filter { line -> line.startsWith(FRAME_PREFIX) }.map { line -> line.removePrefix(FRAME_PREFIX) }
}

internal actual fun currentThreadName(): String? = null

internal actual fun stackLinesOf(cause: Throwable): List<String> = framesOf(cause)

internal actual fun isPlatformFatal(cause: Throwable): Boolean = false
