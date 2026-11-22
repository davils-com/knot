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

package com.davils.knot.failure.internal

import com.davils.knot.failure.FailureReport
import com.davils.knot.failure.UnexpectedFailure
import com.davils.knot.failure.sink.CrashReport
import com.davils.knot.failure.sink.SinkConfiguration
import com.davils.knot.failure.sink.SinkResult
import com.davils.knot.failure.typeNameOf
import com.davils.kore.core.failure.attempted
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withTimeoutOrNull

internal class SinkWriter(private val sinks: List<SinkConfiguration>, private val telemetry: FailureTelemetry) {
    suspend fun recorded(failure: UnexpectedFailure): FailureReport {
        val taking = sinks.filter { sink -> failure.severity in sink.severities }
        if (taking.isEmpty()) return FailureReport.Unrecorded(failure)
        val results = coroutineScope { taking.map { sink -> async { written(sink, failure) } }.awaitAll() }
        return FailureReport.Recorded(failure, results)
    }

    private suspend fun written(sink: SinkConfiguration, failure: UnexpectedFailure): SinkResult {
        val report = CrashReport(failure = failure, sink = sink.name, renderer = sink.renderer)
        val outcome = withTimeoutOrNull(sink.timeout) { attempted { sink.sink.write(report) } }
        val result = resultOf(sink, outcome)
        telemetry.written(failure, result)
        return result
    }

    private fun resultOf(sink: SinkConfiguration, outcome: Result<Unit>?): SinkResult {
        if (outcome == null) return SinkResult.TimedOut(sink = sink.name, timeout = sink.timeout)
        val failure = outcome.exceptionOrNull()
        val failed = failure?.let { thrown -> SinkResult.Failed(sink = sink.name, failureType = typeNameOf(thrown)) }
        return failed ?: SinkResult.Written(sink.name)
    }
}
