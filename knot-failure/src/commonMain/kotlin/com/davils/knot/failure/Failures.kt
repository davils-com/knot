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

package com.davils.knot.failure

import kotlinx.coroutines.CoroutineScope

/**
 * Declares a failure handler that writes its reports on [scope].
 *
 * ```kotlin
 * val failures = failures(applicationScope) {
 *     shutdown = applicationShutdown
 *     classify { fatal<DatabaseCorruptedException>() }
 *     storm { maxReports = 20 }
 *     sinks {
 *         sink("disk", diskSink) { renderer = CrashReportRenderer.json() }
 *         sink("alerts", alertSink) { severities = setOf(FailureSeverity.Fatal) }
 *     }
 * }
 * ```
 *
 * Creating a handler has no effect outside it: it neither touches a file nor installs a handler of uncaught
 * exceptions; on the JVM, `installUncaughtHandler` does that.
 *
 * @param scope the scope the reports are written on; a fatal action runs outside its job, so cancelling it never
 * stops a running shutdown.
 * @param block declares the handler.
 * @return the handler.
 * @throws com.davils.kore.core.validation.report.ValidationException if the declaration breaks a rule; the report lists
 * every problem with the codes of [FailureViolationCodes].
 * @since 1.0.0
 */
public fun failures(scope: CoroutineScope, block: FailureHandlerBuilder.() -> Unit = {}): FailureHandler {
    val builder = FailureHandlerBuilder()
    builder.block()
    return FailureHandler(builder.build(), scope)
}
