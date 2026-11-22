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

import com.davils.knot.failure.UnexpectedFailure
import com.davils.knot.failure.internal.utcStampOf
import com.davils.knot.failure.render.CrashReportRenderer
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.text.SafeText

private const val NAME_PREFIX: String = "crash-"

/**
 * The crash report of one failure, as a [FailureSink] receives it.
 *
 * @property failure the failure.
 * @property sink the name of the sink the report is for.
 * @param renderer renders [text].
 * @since 1.0.0
 */
public class CrashReport internal constructor(
    public val failure: UnexpectedFailure,
    public val sink: String,
    private val renderer: CrashReportRenderer,
) {
    /**
     * The text of the report, rendered once, when it is first read.
     *
     * @since 1.0.0
     */
    public val text: String by lazy { renderer.render(failure) }

    /**
     * The media type of [text].
     *
     * @since 1.0.0
     */
    public val contentType: String get() = renderer.contentType

    /**
     * A file name for the report, `crash-YYYYMMDD-HHMMSS-mmm-<id>.<extension>` in UTC, as kide named its crash files.
     * It holds only letters, digits, `-` and `.`, so it is safe as a file name on every system; Knot never writes it.
     *
     * @since 1.0.0
     */
    public val suggestedName: String
        get() = "$NAME_PREFIX${utcStampOf(failure.at)}-${failure.id.value}.${renderer.extension}"

    /**
     * Returns the failure and the sink, never the text.
     *
     * @return the report.
     * @since 1.0.0
     */
    override fun toString(): String = "CrashReport(${failure.id} for ${SafeText.quoted(sink)})"
}
