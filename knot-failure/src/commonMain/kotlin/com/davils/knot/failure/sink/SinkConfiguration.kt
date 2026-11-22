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

import com.davils.knot.failure.FailureLimits
import com.davils.knot.failure.FailureSeverity
import com.davils.knot.failure.FailureViolationCodes
import com.davils.knot.failure.render.CrashReportRenderer
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.collection.immutableSet
import com.davils.kore.core.text.SafeText
import com.davils.kore.core.validation.Validatable
import com.davils.kore.core.validation.ValidationScope
import kotlin.time.Duration

private const val NAME_MESSAGE: String =
    "must be printable ASCII, not blank and at most ${FailureLimits.MAX_SINK_NAME_LENGTH} characters long"
private const val TIMEOUT_MESSAGE: String = "must be above zero and finite"
private const val SEVERITY_MESSAGE: String = "must take at least one severity"
private const val FIRST_PRINTABLE: Char = ' '
private const val LAST_PRINTABLE: Char = '~'

/**
 * The declaration of one sink of a failure handler.
 *
 * @property name the name of the sink.
 * @property sink the sink.
 * @property timeout how long one write may take.
 * @param severities the severities of the failures the sink receives.
 * @property renderer how a report is rendered for the sink.
 * @since 1.0.0
 */
public class SinkConfiguration internal constructor(
    public val name: String,
    public val sink: FailureSink,
    public val timeout: Duration,
    severities: Set<FailureSeverity>,
    public val renderer: CrashReportRenderer,
) : Validatable {
    /**
     * The severities of the failures the sink receives.
     *
     * @since 1.0.0
     */
    public val severities: Set<FailureSeverity> = severities.immutableSet()

    /**
     * Checks the name, the timeout and the severities of the sink.
     *
     * @since 1.0.0
     */
    override fun ValidationScope.validate() {
        if (!isValidSinkName(name)) fail(::name, NAME_MESSAGE, FailureViolationCodes.INVALID_SINK_NAME)
        if (!timeout.isPositive() || !timeout.isFinite()) {
            fail(::timeout, TIMEOUT_MESSAGE, FailureViolationCodes.INVALID_TIMEOUT)
        }
        if (severities.isEmpty()) fail(::severities, SEVERITY_MESSAGE, FailureViolationCodes.NO_SEVERITY)
    }

    /**
     * Returns the name, the timeout, the severities and the renderer.
     *
     * @return the configuration.
     * @since 1.0.0
     */
    override fun toString(): String =
        "SinkConfiguration(${SafeText.quoted(name)}, timeout=$timeout, severities=${severities.sorted()}, " +
            "renderer=$renderer)"
}

private fun isValidSinkName(name: String): Boolean {
    val isPrintable = name.all { character -> character in FIRST_PRINTABLE..LAST_PRINTABLE }
    return name.isNotBlank() && name.length <= FailureLimits.MAX_SINK_NAME_LENGTH && isPrintable
}
