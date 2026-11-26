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

@file:OptIn(ExperimentalAtomicApi::class, DavilsInternal::class)

package com.davils.knot.failure

import com.davils.knot.failure.internal.FailureTelemetry
import com.davils.knot.failure.internal.RecentFailures
import com.davils.knot.failure.internal.SinkWriter
import com.davils.knot.failure.internal.StormGuard
import com.davils.knot.failure.internal.currentThreadName
import com.davils.knot.shutdown.ShutdownReason
import com.davils.kollect.core.health.Health
import com.davils.kollect.core.health.HealthIndicator
import com.davils.kollect.core.health.ReasonCode
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.concurrency.Guard
import com.davils.kore.core.concurrency.guard
import com.davils.kore.core.failure.attempted
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.ExperimentalAtomicApi

private const val WRITER: String = "knot-failure-report"
private const val FATAL: String = "knot-failure-fatal"

/**
 * Takes in the failures that no code handled: it classifies them, protects itself against storms of the same
 * failure, hands the crash report of each to the sinks of the application, tells about them through [failures], the
 * topic [FailureEvent.TOPIC], its meters and its spans, and runs a shutdown on a fatal one.
 *
 * Failures arrive through [report], which waits for the sinks, through [submit] and [coroutineHandler], which return at
 * once and write in the background, and, on the JVM, through the handler of uncaught exceptions that
 * `installUncaughtHandler` installs. Knot never writes a file: a crash report reaches the disk only through a
 * [FailureSink][com.davils.knot.failure.sink.FailureSink] of the application.
 *
 * Nothing a sink, a rule, a renderer or an observer throws ever reaches the handler as another failure: it is
 * contained and counted. A fatal failure runs the fatal action of the handler exactly once, after its report was
 * written. The handler is safe to use from every thread and coroutine. It is a [HealthIndicator]: degraded when
 * [FailureHandlerBuilder.degradedAt] failures happened within the last storm window, down once it is closed.
 *
 * @property configuration the declaration of the handler.
 * @since 1.0.0
 */
public class FailureHandler internal constructor(
    public val configuration: FailureHandlerConfiguration,
    private val scope: CoroutineScope,
) : HealthIndicator, AutoCloseable {
    private val recent: RecentFailures = RecentFailures(configuration.storm.window, configuration.clock)
    private val telemetry: FailureTelemetry = FailureTelemetry(configuration, recent)
    private val storm: StormGuard = StormGuard(configuration.storm)
    private val writer: SinkWriter = SinkWriter(configuration.sinks, telemetry)
    private val guard: Guard = guard()
    private val writes: MutableSet<Job> = mutableSetOf()
    private val closed: AtomicBoolean = AtomicBoolean(false)
    private val fatalOnce: AtomicBoolean = AtomicBoolean(false)
    private val closers: MutableList<AutoCloseable> = mutableListOf()

    /**
     * The reported failures, for collectors in the process; a slow collector loses the oldest beyond
     * [FailureHandlerBuilder.eventCapacity]. Suppressed and ignored failures are not on it.
     *
     * @since 1.0.0
     */
    public val failures: Flow<UnexpectedFailure> get() = telemetry.failures

    /**
     * Whether the handler is closed; a closed handler ignores every failure.
     *
     * @since 1.0.0
     */
    public val isClosed: Boolean get() = closed.load()

    /**
     * Takes in [cause], waits until every sink that takes it wrote its report and, for a fatal failure, runs the fatal
     * action.
     *
     * @param cause the failure.
     * @param origin where it happened.
     * @return what the handler did with it.
     * @since 1.0.0
     */
    public suspend fun report(cause: Throwable, origin: FailureOrigin): FailureReport {
        val admitted = admitted(cause, origin)
        if (admitted !is FailureReport.Unrecorded) return admitted
        val recorded = writer.recorded(admitted.failure)
        fatal(admitted.failure)
        return recorded
    }

    /**
     * Takes in [cause] and returns at once; the reports are written in the background, on the scope of the handler,
     * and a fatal failure runs the fatal action once they are written.
     *
     * @param cause the failure.
     * @param origin where it happened.
     * @since 1.0.0
     */
    public fun submit(cause: Throwable, origin: FailureOrigin) {
        val admitted = admitted(cause, origin) as? FailureReport.Unrecorded ?: return
        val failure = admitted.failure
        val written = track(scope.launch(CoroutineName(WRITER)) { attempted { writer.recorded(failure) } })
        if (failure.severity != FailureSeverity.Fatal) return
        detached().launch(CoroutineName(FATAL)) {
            written.join()
            fatal(failure)
        }
    }

    /**
     * Returns a handler for the failures of coroutines that [submit]s each with [origin].
     *
     * @param origin where the failures happen; a coroutine by default.
     * @return the handler.
     * @since 1.0.0
     */
    public fun coroutineHandler(origin: FailureOrigin = FailureOrigin.Coroutine(COROUTINE)): CoroutineExceptionHandler =
        CoroutineExceptionHandler { _, cause -> submit(cause, origin) }

    /**
     * Waits until every report that [submit] started is written.
     *
     * @since 1.0.0
     */
    public suspend fun drain() {
        val running = guard.locked { writes.toList() }
        running.joinAll()
    }

    /**
     * Reads the counters of the handler.
     *
     * @return a snapshot of the counters.
     * @since 1.0.0
     */
    public fun metrics(): FailureMetrics = telemetry.metrics()

    /**
     * Tells how the handler is: down once closed, degraded with [ReasonCode.ThresholdCrossed] when
     * [FailureHandlerBuilder.degradedAt] failures happened within the last storm window, up otherwise.
     *
     * @return the health.
     * @since 1.0.0
     */
    override suspend fun check(): Health {
        if (isClosed) return Health.down(ReasonCode.StateMatched)
        val isDegraded = telemetry.metrics().recent >= configuration.degradedAt
        return Health.degraded(ReasonCode.ThresholdCrossed).takeIf { isDegraded } ?: Health.Up
    }

    /**
     * Closes the handler: it ignores every further failure, cancels the reports still being written and removes the
     * handlers of uncaught exceptions it installed. A fatal action that runs already runs to its end. Closing twice has
     * no further effect.
     *
     * @since 1.0.0
     */
    override fun close() {
        if (!closed.compareAndSet(expectedValue = false, newValue = true)) return
        val installed = guard.locked { closers.toList().also { closers.clear() } }
        installed.forEach { closer -> closer.close() }
        val running = guard.locked { writes.toList() }
        running.forEach { job -> job.cancel() }
    }

    /**
     * Returns whether the handler is closed and its counters.
     *
     * @return the handler.
     * @since 1.0.0
     */
    override fun toString(): String = "FailureHandler(closed=$isClosed, ${metrics()})"

    internal fun whenClosed(closer: AutoCloseable) {
        guard.locked { closers += closer }
    }

    private fun admitted(cause: Throwable, origin: FailureOrigin): FailureReport {
        val severity = configuration.classification.severityOf(cause)
        if (isClosed || severity == null) return ignored(cause)
        return taken(cause, origin, severity)
    }

    private fun taken(cause: Throwable, origin: FailureOrigin, severity: FailureSeverity): FailureReport {
        val fingerprint = FailureFingerprint.of(cause)
        val at = configuration.clock.now()
        val verdict = storm.admit(fingerprint, at, isForced = severity == FailureSeverity.Fatal)
        val failure = UnexpectedFailure(
            id = FailureId(configuration.ids.next().toString()),
            cause = cause,
            origin = origin,
            severity = severity,
            fingerprint = fingerprint,
            at = at,
            thread = currentThreadName(),
            suppressed = verdict.suppressed,
        )
        telemetry.occurred(failure)
        if (!verdict.isAdmitted) return suppressed(failure)
        telemetry.reported(failure)
        return FailureReport.Unrecorded(failure)
    }

    private fun ignored(cause: Throwable): FailureReport {
        telemetry.ignored()
        return FailureReport.Ignored(cause)
    }

    private fun suppressed(failure: UnexpectedFailure): FailureReport {
        telemetry.suppressed()
        return FailureReport.Suppressed(failure)
    }

    private suspend fun fatal(failure: UnexpectedFailure) {
        val action = configuration.fatalAction
        val isFatal = failure.severity == FailureSeverity.Fatal
        val isFirst = isFatal && fatalOnce.compareAndSet(expectedValue = false, newValue = true)
        if (action == null || !isFirst) return
        attempted { action(ShutdownReason.FatalError(cause = failure.cause, origin = failure.origin.subject)) }
    }

    private fun track(job: Job): Job {
        guard.locked { writes += job }
        job.invokeOnCompletion { guard.locked { writes -= job } }
        return job
    }

    private fun detached(): CoroutineScope = CoroutineScope(scope.coroutineContext.minusKey(Job))

    private companion object {
        const val COROUTINE: String = "coroutine"
    }
}
