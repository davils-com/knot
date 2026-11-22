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

@file:OptIn(DavilsInternal::class)

package com.davils.knot.failure.sink

import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.text.SafeText
import kotlin.time.Duration

/**
 * How writing a crash report to one sink ended.
 *
 * @property sink the name of the sink.
 * @since 1.0.0
 */
public sealed class SinkResult(public val sink: String) {
    /**
     * Whether the sink took the report.
     *
     * @since 1.0.0
     */
    public val isWritten: Boolean get() = this is Written

    /**
     * The sink took the report.
     *
     * @since 1.0.0
     */
    public class Written internal constructor(sink: String) : SinkResult(sink) {
        /**
         * Returns the result.
         *
         * @return the result, such as `Written("disk")`.
         * @since 1.0.0
         */
        override fun toString(): String = "Written(${SafeText.quoted(sink)})"
    }

    /**
     * The sink threw.
     *
     * @property failureType the simple name of the type of what it threw, never its message.
     * @since 1.0.0
     */
    public class Failed internal constructor(sink: String, public val failureType: String) : SinkResult(sink) {
        /**
         * Returns the result.
         *
         * @return the result, such as `Failed("disk", IOException)`.
         * @since 1.0.0
         */
        override fun toString(): String = "Failed(${SafeText.quoted(sink)}, $failureType)"
    }

    /**
     * The sink did not return within its timeout and was cancelled.
     *
     * @property timeout the timeout of the sink.
     * @since 1.0.0
     */
    public class TimedOut internal constructor(sink: String, public val timeout: Duration) : SinkResult(sink) {
        /**
         * Returns the result.
         *
         * @return the result, such as `TimedOut("disk", 5s)`.
         * @since 1.0.0
         */
        override fun toString(): String = "TimedOut(${SafeText.quoted(sink)}, $timeout)"
    }
}
