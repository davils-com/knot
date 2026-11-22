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

internal actual fun qualifiedTypeOf(cause: Throwable): String = cause.javaClass.name

internal actual fun framesOf(cause: Throwable): List<String> =
    cause.stackTrace.map { frame -> "${frame.className}.${frame.methodName}" }

internal actual fun currentThreadName(): String? = Thread.currentThread().name

internal actual fun stackLinesOf(cause: Throwable): List<String> = cause.stackTrace.map { frame -> frame.toString() }

internal actual fun isPlatformFatal(cause: Throwable): Boolean = cause is VirtualMachineError
