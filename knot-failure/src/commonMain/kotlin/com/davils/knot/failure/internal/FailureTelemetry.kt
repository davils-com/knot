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

import com.davils.knot.failure.FailureEvent
import com.davils.knot.failure.FailureHandlerConfiguration
import com.davils.knot.failure.FailureMetrics
import com.davils.knot.failure.UnexpectedFailure
import com.davils.knot.failure.sink.SinkResult
import com.davils.kollect.core.trace.SpanOutcome
import com.davils.konfetti.core.EventBus
import com.davils.konfetti.core.Topic
import com.davils.konfetti.core.publish.PublishResult
import com.davils.kore.core.failure.contained
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

private const val SPAN: String = "knot.failure"
private const val ID_ATTRIBUTE: String = "knot.failure.id"
private const val ORIGIN_ATTRIBUTE: String = "knot.failure.origin"
private const val SEVERITY_ATTRIBUTE: String = "knot.failure.severity"
private const val FINGERPRINT_ATTRIBUTE: String = "knot.failure.fingerprint"
private const val WRITTEN: String = "written"
private const val FAILED: String = "failed"
private const val TIMED_OUT: String = "timed_out"

internal class FailureTelemetry(
    private val configuration: FailureHandlerConfiguration,
    private val recent: RecentFailures,
) {
    private val local: MutableSharedFlow<UnexpectedFailure> = MutableSharedFlow(
        extraBufferCapacity = configuration.eventCapacity,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    private val topic: Topic<FailureEvent>? = configuration.events?.let { bus -> topicOf(bus) }
    private val meters: FailureMeters? = configuration.meters?.let(::FailureMeters)
    private val counters: FailureCounters = FailureCounters()

    val failures: Flow<UnexpectedFailure> = local.asSharedFlow()

    fun metrics(): FailureMetrics = counters.snapshot(recent.count())

    fun occurred(failure: UnexpectedFailure) {
        recent.record(failure.at)
        meters?.occurred(failure.origin, failure.severity)
        meters?.recent(recent.count())
    }

    fun ignored() {
        counters.ignored()
        meters?.ignored()
    }

    fun suppressed() {
        counters.suppressed()
        meters?.suppressed()
    }

    fun reported(failure: UnexpectedFailure) {
        counters.reported()
        local.tryEmit(failure)
        emit(FailureEvent.Occurred(failure))
        val span = configuration.tracer.start(SPAN)
        span.attribute(key = ID_ATTRIBUTE, value = failure.id.value)
        span.attribute(key = ORIGIN_ATTRIBUTE, value = failure.origin.kind)
        span.attribute(key = SEVERITY_ATTRIBUTE, value = failure.severity.name.lowercase())
        span.attribute(key = FINGERPRINT_ATTRIBUTE, value = failure.fingerprint.value)
        span.end(SpanOutcome.Error)
    }

    fun written(failure: UnexpectedFailure, result: SinkResult) {
        counters.written(result.isWritten)
        meters?.written(sink = result.sink, outcome = outcomeOf(result))
        emit(FailureEvent.Written(failure, result))
    }

    private fun emit(event: FailureEvent) {
        val target = topic?.takeIf { listened -> listened.hasConsumers } ?: return
        val result = contained { target.offer(event) }.getOrNull()
        if (result is PublishResult.Published) return
        counters.rejectedEvent()
        meters?.rejectedEvent()
    }

    private fun outcomeOf(result: SinkResult): String = when (result) {
        is SinkResult.Written -> WRITTEN
        is SinkResult.Failed -> FAILED
        is SinkResult.TimedOut -> TIMED_OUT
    }

    private companion object {
        fun topicOf(bus: EventBus): Topic<FailureEvent> {
            if (FailureEvent.TOPIC !in bus) bus.declare(FailureEvent.TOPIC)
            return bus[FailureEvent.TOPIC]
        }
    }
}
