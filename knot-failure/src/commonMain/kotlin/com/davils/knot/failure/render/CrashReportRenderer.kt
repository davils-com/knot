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

import com.davils.knot.failure.UnexpectedFailure

private const val TEXT_TYPE: String = "text/plain; charset=utf-8"
private const val TEXT_EXTENSION: String = "txt"

/**
 * Turns a failure into the text of a crash report, for a [FailureSink][com.davils.knot.failure.sink.FailureSink].
 *
 * [text] writes the plain text report of kide; [json] writes one JSON object. Every text that may come from input,
 * such as messages, stack frames and names, is escaped, so a report is safe to show in a terminal, a log or a web
 * page. A renderer of the application must keep to the same rule.
 *
 * @since 1.0.0
 */
public fun interface CrashReportRenderer {
    /**
     * The media type of the reports, such as `text/plain; charset=utf-8`.
     *
     * @since 1.0.0
     */
    public val contentType: String get() = TEXT_TYPE

    /**
     * The file extension a report of this renderer would have, such as `txt`.
     *
     * @since 1.0.0
     */
    public val extension: String get() = TEXT_EXTENSION

    /**
     * Renders [failure].
     *
     * @param failure the failure.
     * @return the text of the crash report.
     * @since 1.0.0
     */
    public fun render(failure: UnexpectedFailure): String

    /**
     * The renderers of Knot.
     *
     * @since 1.0.0
     */
    public companion object {
        /**
         * Returns the renderer of the plain text report, which looks like this.
         *
         * ```text
         * crash report
         *
         * id:          0195…
         * time:        2026-11-22T12:00:00Z
         * severity:    fatal
         * origin:      thread "main"
         * …
         *
         * java.lang.IllegalStateException: the message
         *     at …
         * caused by: …
         * ```
         *
         * @param options what the report shows.
         * @return the renderer.
         * @since 1.0.0
         */
        public fun text(options: RenderOptions = RenderOptions()): CrashReportRenderer = TextRenderer(options)

        /**
         * Returns the renderer of a JSON report: one object with the fields of the failure and an array `causes` of
         * objects with `type`, `message` and `frames`. Every string is escaped as JSON, unsafe code points as
         * `\uXXXX`.
         *
         * @param options what the report shows.
         * @return the renderer.
         * @since 1.0.0
         */
        public fun json(options: RenderOptions = RenderOptions()): CrashReportRenderer = JsonRenderer(options)
    }
}
