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

package com.davils.knot.failure.render

import com.davils.knot.failure.FailureLimits

private const val DEFAULT_CAUSE_DEPTH: Int = 8

/**
 * What a [CrashReportRenderer] puts into a crash report.
 *
 * Messages and stack traces may hold secrets and text of an attacker: a renderer escapes them, and a report that
 * leaves the process may leave them out.
 *
 * @property hasMessages whether the messages of the failure and its causes are shown; `true` by default.
 * @property hasStackTrace whether the stack frames are shown; `true` by default.
 * @property maxCauseDepth how many causes of the failure are shown, between 0 and [FailureLimits.MAX_CAUSE_DEPTH];
 * 8 by default.
 * @property maxLength the longest report, between 1 and [FailureLimits.MAX_REPORT_LENGTH]; a longer one is cut and
 * marked as cut.
 * @throws IllegalArgumentException if [maxCauseDepth] or [maxLength] lies outside its bounds.
 * @since 1.0.0
 */
public class RenderOptions(
    public val hasMessages: Boolean = true,
    public val hasStackTrace: Boolean = true,
    public val maxCauseDepth: Int = DEFAULT_CAUSE_DEPTH,
    public val maxLength: Int = FailureLimits.MAX_REPORT_LENGTH,
) {
    init {
        require(maxCauseDepth in 0..FailureLimits.MAX_CAUSE_DEPTH) { "The cause depth lies outside its bounds" }
        require(maxLength in 1..FailureLimits.MAX_REPORT_LENGTH) { "The length lies outside its bounds" }
    }

    /**
     * Returns the options.
     *
     * @return the options.
     * @since 1.0.0
     */
    override fun toString(): String = "RenderOptions(hasMessages=$hasMessages, hasStackTrace=$hasStackTrace, " +
        "maxCauseDepth=$maxCauseDepth, maxLength=$maxLength)"
}
