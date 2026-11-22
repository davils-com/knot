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

package com.davils.knot.testing

import com.davils.knot.failure.sink.CrashReport
import com.davils.knot.failure.sink.FailureSink
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update

/**
 * A [FailureSink] that keeps every report in memory, for tests of failure handling.
 *
 * It is safe to write to from every thread and coroutine.
 *
 * @since 1.0.0
 */
public class RecordingFailureSink : FailureSink {
    private val recorded: MutableStateFlow<List<CrashReport>> = MutableStateFlow(emptyList())

    /**
     * The reports written so far, in the order they arrived.
     *
     * @since 1.0.0
     */
    public val reports: List<CrashReport> get() = recorded.value

    /**
     * Keeps [report].
     *
     * @param report the report.
     * @since 1.0.0
     */
    override suspend fun write(report: CrashReport) {
        recorded.update { reports -> reports + report }
    }

    /**
     * Waits until at least [count] reports were written.
     *
     * @param count the number of reports to wait for.
     * @return the reports written so far.
     * @since 1.0.0
     */
    public suspend fun awaitCount(count: Int): List<CrashReport> = recorded.first { reports -> reports.size >= count }

    /**
     * Forgets every report.
     *
     * @since 1.0.0
     */
    public fun clear() {
        recorded.value = emptyList()
    }

    /**
     * Returns the number of reports.
     *
     * @return the sink, such as `RecordingFailureSink(2 reports)`.
     * @since 1.0.0
     */
    override fun toString(): String = "RecordingFailureSink(${reports.size} reports)"
}
