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

private const val MESSAGE: String = "the failing sink refuses every report"

private fun refusal(): Throwable = IllegalStateException(MESSAGE)

/**
 * A [FailureSink] that throws what [failure] returns on every write, such as a full disk.
 *
 * @param failure creates the failure to throw.
 * @since 1.0.0
 */
public class FailingFailureSink(private val failure: () -> Throwable = ::refusal) : FailureSink {
    /**
     * Throws.
     *
     * @param report the report it never keeps.
     * @throws Throwable what [failure] returns.
     * @since 1.0.0
     */
    override suspend fun write(report: CrashReport): Unit = throw failure()

    /**
     * Returns the name of the sink.
     *
     * @return `FailingFailureSink`.
     * @since 1.0.0
     */
    override fun toString(): String = "FailingFailureSink"
}
