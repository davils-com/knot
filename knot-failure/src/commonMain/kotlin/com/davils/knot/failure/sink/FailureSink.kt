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

package com.davils.knot.failure.sink

/**
 * Where an application keeps the crash reports of its unexpected failures: a file, a log, a database, a remote
 * service. The application writes the sink; Knot never touches a file.
 *
 * ```kotlin
 * val disk = FailureSink { report ->
 *     withContext(Dispatchers.IO) {
 *         Files.writeString(crashes.resolve(report.suggestedName), report.text)
 *     }
 * }
 * ```
 *
 * A handler calls [write] once per report the sink takes, on the scope of the handler, at most for the timeout of the
 * sink; whatever the sink throws is contained and reported as a [SinkResult.Failed], never as another failure. A sink
 * that keeps reports also decides how many it keeps. A sink is called from several coroutines at once and must be safe
 * for that.
 *
 * @since 1.0.0
 */
public fun interface FailureSink {
    /**
     * Writes [report].
     *
     * @param report the crash report.
     * @since 1.0.0
     */
    public suspend fun write(report: CrashReport)
}
