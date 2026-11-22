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

package com.davils.knot.failure.storm

import com.davils.kore.core.annotation.DavilsDsl
import com.davils.kore.core.dsl.ValidatingBuilder
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

private const val SUBJECT: String = "failure storm"
private const val DEFAULT_MAX_REPORTS: Int = 10

/**
 * Declares how a failure handler protects itself against a storm of the same failure, for
 * [FailureHandlerBuilder.storm][com.davils.knot.failure.FailureHandlerBuilder.storm].
 *
 * The first failure of a fingerprint opens a window of [window]; within it, at most [maxReports] failures of that
 * fingerprint are reported and the others suppressed and counted. The next reported failure tells how many were
 * suppressed. Fatal failures are never suppressed.
 *
 * @since 1.0.0
 */
@DavilsDsl
public class FailureStormBuilder internal constructor() : ValidatingBuilder<FailureStormConfiguration>() {
    override val subject: String get() = SUBJECT

    /**
     * How long a window lasts; one minute by default, above zero and finite.
     *
     * @since 1.0.0
     */
    public var window: Duration = 1.minutes

    /**
     * How many failures of one fingerprint a window reports; 10 by default, between 1 and
     * [FailureLimits.MAX_STORM_REPORTS][com.davils.knot.failure.FailureLimits.MAX_STORM_REPORTS].
     *
     * @since 1.0.0
     */
    public var maxReports: Int = DEFAULT_MAX_REPORTS

    override fun assemble(): FailureStormConfiguration = FailureStormConfiguration(
        window = window,
        maxReports = maxReports,
    )
}
