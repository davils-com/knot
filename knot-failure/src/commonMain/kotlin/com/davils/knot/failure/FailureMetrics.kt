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

/**
 * A snapshot of the counters of a failure handler, read by [FailureHandler.metrics].
 *
 * @property reported the failures that were reported.
 * @property suppressed the failures the storm guard suppressed.
 * @property ignored the failures the classification ignored or that reached a closed handler.
 * @property written the reports a sink took.
 * @property sinkFailures the writes to a sink that failed or timed out.
 * @property recent the failures, suppressed ones included, within the last storm window.
 * @property rejectedEvents the events the event bus did not take.
 * @since 1.0.0
 */
public class FailureMetrics internal constructor(
    public val reported: Long,
    public val suppressed: Long,
    public val ignored: Long,
    public val written: Long,
    public val sinkFailures: Long,
    public val recent: Int,
    public val rejectedEvents: Long,
) {
    /**
     * Returns every counter.
     *
     * @return the counters.
     * @since 1.0.0
     */
    override fun toString(): String =
        "FailureMetrics(reported=$reported, suppressed=$suppressed, ignored=$ignored, written=$written, " +
            "sinkFailures=$sinkFailures, recent=$recent, rejectedEvents=$rejectedEvents)"
}
