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

package com.davils.knot.failure

import com.davils.knot.failure.sink.SinkResult
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.collection.immutableList

/**
 * What a failure handler did with one failure.
 *
 * @since 1.0.0
 */
public sealed interface FailureReport {
    /**
     * The failure was reported and handed to the sinks that take its severity.
     *
     * @property failure the failure.
     * @param results how writing to each sink ended, in the order of the sinks.
     * @since 1.0.0
     */
    public class Recorded internal constructor(public val failure: UnexpectedFailure, results: List<SinkResult>) :
        FailureReport {
        /**
         * How writing to each sink ended, in the order of the sinks.
         *
         * @since 1.0.0
         */
        public val results: List<SinkResult> = results.immutableList()

        /**
         * Returns the failure and the results.
         *
         * @return the report.
         * @since 1.0.0
         */
        override fun toString(): String = "Recorded(${failure.id}, $results)"
    }

    /**
     * The failure was reported, but no sink takes its severity.
     *
     * @property failure the failure.
     * @since 1.0.0
     */
    public class Unrecorded internal constructor(public val failure: UnexpectedFailure) : FailureReport {
        /**
         * Returns the failure.
         *
         * @return the report.
         * @since 1.0.0
         */
        override fun toString(): String = "Unrecorded(${failure.id})"
    }

    /**
     * The storm guard suppressed the failure; it was counted and nothing else.
     *
     * @property failure the failure.
     * @since 1.0.0
     */
    public class Suppressed internal constructor(public val failure: UnexpectedFailure) : FailureReport {
        /**
         * Returns the failure.
         *
         * @return the report.
         * @since 1.0.0
         */
        override fun toString(): String = "Suppressed(${failure.id})"
    }

    /**
     * The classification ignores the failure, or the handler is closed; it was counted and nothing else.
     *
     * @property cause the failure.
     * @since 1.0.0
     */
    public class Ignored internal constructor(public val cause: Throwable) : FailureReport {
        /**
         * Returns the type of the failure, never its message.
         *
         * @return the report.
         * @since 1.0.0
         */
        override fun toString(): String = "Ignored(${typeNameOf(cause)})"
    }
}
