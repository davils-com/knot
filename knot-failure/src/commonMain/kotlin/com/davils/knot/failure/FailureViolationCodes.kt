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

import com.davils.kore.core.validation.violation.ViolationCode

/**
 * The codes that the validation of a failure handler reports, stable names in the `knot.failure` namespace.
 *
 * @since 1.0.0
 */
public object FailureViolationCodes {
    /**
     * The storm window is not above zero or not finite.
     *
     * @since 1.0.0
     */
    public val INVALID_WINDOW: ViolationCode = ViolationCode("knot.failure.invalid-window")

    /**
     * A count lies outside its bounds, such as the reports of a storm window or the capacity of the failure flow.
     *
     * @since 1.0.0
     */
    public val INVALID_LIMIT: ViolationCode = ViolationCode("knot.failure.invalid-limit")

    /**
     * The name of a sink is blank, longer than [FailureLimits.MAX_SINK_NAME_LENGTH] or not printable ASCII.
     *
     * @since 1.0.0
     */
    public val INVALID_SINK_NAME: ViolationCode = ViolationCode("knot.failure.invalid-sink-name")

    /**
     * The timeout of a sink is not above zero or not finite.
     *
     * @since 1.0.0
     */
    public val INVALID_TIMEOUT: ViolationCode = ViolationCode("knot.failure.invalid-timeout")

    /**
     * More than [FailureLimits.MAX_SINKS] sinks are declared.
     *
     * @since 1.0.0
     */
    public val TOO_MANY_SINKS: ViolationCode = ViolationCode("knot.failure.too-many-sinks")

    /**
     * A sink takes no severity, so it never receives a report.
     *
     * @since 1.0.0
     */
    public val NO_SEVERITY: ViolationCode = ViolationCode("knot.failure.no-severity")
}
