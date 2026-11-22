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

package com.davils.knot.shutdown.internal

import com.davils.knot.shutdown.RegistrationRefusal
import com.davils.knot.shutdown.ShutdownConfiguration
import com.davils.knot.shutdown.ShutdownEvent
import com.davils.knot.shutdown.ShutdownMetrics
import com.davils.knot.shutdown.ShutdownPhase
import com.davils.knot.shutdown.ShutdownReason
import com.davils.knot.shutdown.exit.ExitCode
import com.davils.knot.shutdown.report.PhaseReport
import com.davils.knot.shutdown.report.ShutdownReport
import com.davils.knot.shutdown.report.TaskOutcome
import com.davils.knot.shutdown.report.TaskReport
import com.davils.knot.shutdown.task.ShutdownTaskName
import com.davils.kollect.core.trace.Span
import com.davils.kollect.core.trace.SpanOutcome
import com.davils.konfetti.core.EventBus
import com.davils.konfetti.core.Topic
import com.davils.konfetti.core.publish.PublishResult
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.failure.contained
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlin.time.Duration

private const val SHUTDOWN_SPAN: String = "knot.shutdown"
private const val TASK_SPAN: String = "knot.shutdown.task"
private const val NAME_ATTRIBUTE: String = "knot.shutdown.name"
private const val REASON_ATTRIBUTE: String = "knot.shutdown.reason"
private const val TASK_ATTRIBUTE: String = "knot.shutdown.task"
private const val PHASE_ATTRIBUTE: String = "knot.shutdown.phase"
private const val OUTCOME_ATTRIBUTE: String = "knot.shutdown.outcome"
private const val SHUTTING_DOWN: String = "shutting_down"
private const val INVALID: String = "invalid"

internal class ShutdownTelemetry(private val configuration: ShutdownConfiguration) {
    private val name: String = configuration.name
    private val local: MutableSharedFlow<ShutdownEvent> = MutableSharedFlow(
        extraBufferCapacity = configuration.eventCapacity,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    private val topic: Topic<ShutdownEvent>? = configuration.events?.let { bus -> topicOf(bus) }
    private val meters: ShutdownMeters? = configuration.meters?.let { registry -> ShutdownMeters(registry, name) }
    private val counters: ShutdownCounters = ShutdownCounters()

    val events: Flow<ShutdownEvent> = local.asSharedFlow()

    fun metrics(registeredTasks: Int): ShutdownMetrics = counters.snapshot(registeredTasks)

    fun registered(count: Int) {
        meters?.registered(count)
    }

    fun refused(refusal: RegistrationRefusal) {
        counters.refused()
        val reason = SHUTTING_DOWN.takeIf { refusal is RegistrationRefusal.ShuttingDown } ?: INVALID
        meters?.refused(reason)
    }

    fun initiated(reason: ShutdownReason, tasks: Int): Span {
        meters?.stage(ShutdownStage.ShuttingDown)
        emit(ShutdownEvent.Initiated(name, reason, tasks))
        val span = configuration.tracer.start(SHUTDOWN_SPAN)
        span.attribute(key = NAME_ATTRIBUTE, value = name)
        span.attribute(key = REASON_ATTRIBUTE, value = reason.toString())
        return span
    }

    fun phaseStarted(phase: ShutdownPhase, tasks: List<ShutdownTaskName>) {
        emit(ShutdownEvent.PhaseStarted(name, phase, tasks.map { task -> task.value }))
    }

    fun startTask(task: ShutdownTaskName, phase: ShutdownPhase): Span {
        val span = configuration.tracer.start(TASK_SPAN)
        span.attribute(key = TASK_ATTRIBUTE, value = task.value)
        span.attribute(key = PHASE_ATTRIBUTE, value = phase.name)
        return span
    }

    fun concluded(report: TaskReport, span: Span) {
        val kind = OutcomeKind.of(report.outcome)
        counters.outcome(kind)
        meters?.task(kind, elapsedOf(report.outcome))
        span.attribute(key = OUTCOME_ATTRIBUTE, value = kind.label)
        span.end(spanOutcomeOf(report.isCompleted))
        emit(ShutdownEvent.TaskConcluded(name, report))
    }

    fun phaseCompleted(report: PhaseReport) {
        emit(ShutdownEvent.PhaseCompleted(name, report))
    }

    fun terminated(report: ShutdownReport, span: Span) {
        meters?.stage(ShutdownStage.Terminated)
        meters?.terminated(report.elapsed)
        span.end(spanOutcomeOf(report.isClean))
        emit(ShutdownEvent.Terminated(name, report))
    }

    fun escalated(exitCode: ExitCode, after: Duration) {
        counters.escalated()
        meters?.escalated()
        emit(ShutdownEvent.Escalated(name, exitCode, after))
    }

    private fun emit(event: ShutdownEvent) {
        local.tryEmit(event)
        val target = topic?.takeIf { listened -> listened.hasConsumers } ?: return
        val result = contained { target.offer(event) }.getOrNull()
        if (result is PublishResult.Published) return
        counters.rejectedEvent()
        meters?.rejectedEvent()
    }

    private fun elapsedOf(outcome: TaskOutcome): Duration? = when (outcome) {
        is TaskOutcome.Completed -> outcome.elapsed
        is TaskOutcome.Failed -> outcome.elapsed
        is TaskOutcome.TimedOut -> outcome.timeout
        is TaskOutcome.Abandoned -> outcome.timeout
        TaskOutcome.DeadlineExceeded -> null
    }

    private fun spanOutcomeOf(isSuccess: Boolean): SpanOutcome {
        if (isSuccess) return SpanOutcome.Ok
        return SpanOutcome.Error
    }

    private companion object {
        fun topicOf(bus: EventBus): Topic<ShutdownEvent> {
            if (ShutdownEvent.TOPIC !in bus) bus.declare(ShutdownEvent.TOPIC)
            return bus[ShutdownEvent.TOPIC]
        }
    }
}
