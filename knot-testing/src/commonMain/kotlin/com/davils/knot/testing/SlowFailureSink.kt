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
import kotlinx.coroutines.delay
import kotlin.time.Duration

/**
 * A [FailureSink] that takes [delay] for every write and then hands the report to [next], such as a slow remote
 * service. With virtual time, the delay costs nothing.
 *
 * @property delay how long every write takes.
 * @param next where the report goes after the delay.
 * @since 1.0.0
 */
public class SlowFailureSink(public val delay: Duration, private val next: FailureSink = RecordingFailureSink()) :
    FailureSink {
    /**
     * Waits for [delay] and writes [report] to the next sink.
     *
     * @param report the report.
     * @since 1.0.0
     */
    override suspend fun write(report: CrashReport) {
        delay(delay)
        next.write(report)
    }

    /**
     * Returns the delay.
     *
     * @return the sink, such as `SlowFailureSink(5s)`.
     * @since 1.0.0
     */
    override fun toString(): String = "SlowFailureSink($delay)"
}
