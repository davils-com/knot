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

package com.davils.knot.shutdown

import com.davils.kollect.core.export.MetricExporter
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

private const val DEFAULT_EXPORTER_NAME: String = "metric-exporter"
private val reportMargin: Duration = 1.seconds

/**
 * Declares the task that stops the kollect metric exporter [exporter] in [ShutdownPhase.Flush], the last phase, so the
 * meters of every earlier phase leave the process.
 *
 * The task calls [MetricExporter.shutdown]: the exporter takes a last snapshot, sends it with every waiting request and
 * drops what is still left as `ShutdownDeadline`, so every point stays accounted for. It sends for at most
 * [flushTimeout] and never longer than its budget leaves, less a second for the report of the exporter.
 *
 * @param exporter the exporter, which stops sending for good.
 * @param flushTimeout how long the exporter may send; 5 seconds by default.
 * @param name the name of the task.
 * @since 1.1.0
 */
public fun ShutdownPlanBuilder<*>.metricExporter(
    exporter: MetricExporter,
    flushTimeout: Duration = 5.seconds,
    name: String = DEFAULT_EXPORTER_NAME,
) {
    flush {
        task(name) {
            timeout = flushTimeout + reportMargin
            action {
                val budget = minOf(a = flushTimeout, b = remaining - reportMargin)
                exporter.shutdown(budget.coerceAtLeast(Duration.ZERO))
            }
        }
    }
}
