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

package com.davils.knot.failure.sink

import com.davils.knot.failure.FailureSeverity
import com.davils.knot.failure.render.CrashReportRenderer
import com.davils.kore.core.annotation.DavilsDsl
import com.davils.kore.core.dsl.ValidatingBuilder
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

private const val SUBJECT: String = "failure sink"

/**
 * Declares how a handler writes to one sink, for [FailureSinksBuilder.sink].
 *
 * @property name the name of the sink.
 * @since 1.0.0
 */
@DavilsDsl
public class SinkBuilder internal constructor(public val name: String, private val sink: FailureSink) :
    ValidatingBuilder<SinkConfiguration>() {
    override val subject: String get() = SUBJECT

    /**
     * How long one write may take before it is cancelled; 5 seconds by default, above zero and finite.
     *
     * @since 1.0.0
     */
    public var timeout: Duration = DEFAULT_TIMEOUT

    /**
     * The severities of the failures the sink receives; every severity by default.
     *
     * @since 1.0.0
     */
    public var severities: Set<FailureSeverity> = FailureSeverity.entries.toSet()

    /**
     * How a report is rendered for the sink; [CrashReportRenderer.text] by default.
     *
     * @since 1.0.0
     */
    public var renderer: CrashReportRenderer = CrashReportRenderer.text()

    override fun assemble(): SinkConfiguration = SinkConfiguration(
        name = name,
        sink = sink,
        timeout = timeout,
        severities = severities,
        renderer = renderer,
    )

    private companion object {
        val DEFAULT_TIMEOUT: Duration = 5.seconds
    }
}
