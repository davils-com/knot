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

@file:OptIn(ExperimentalKotest::class)

package com.davils.knot.testing

import com.davils.knot.failure.FailureReport
import com.davils.knot.failure.FailureSeverity
import com.davils.knot.failure.failures
import com.davils.knot.failure.sink.SinkResult
import io.kotest.common.ExperimentalKotest
import io.kotest.core.spec.style.FunSpec
import io.kotest.core.test.testCoroutineScheduler
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlin.time.Duration.Companion.seconds

private const val RECORDING: String = "recording"

class SinksTest : FunSpec({
    coroutineTestScope = true

    test("the recording sink keeps every report and waits for them") {
        val sink = RecordingFailureSink()
        val handler = failures(this) { sinks { sink(name = RECORDING, sink = sink) } }
        val waiting = async(start = CoroutineStart.UNDISPATCHED) { sink.awaitCount(1) }

        handler.report(TestFailures.failure().cause, TestFailures.ORIGIN)

        waiting.await().size shouldBe 1
        sink.toString() shouldBe "RecordingFailureSink(1 reports)"
        sink.clear()
        sink.reports shouldBe emptyList()
    }

    test("the failing sink fails every write") {
        val handler = failures(this) { sinks { sink(name = "failing", sink = FailingFailureSink()) } }

        val report = handler.report(TestFailures.failure().cause, TestFailures.ORIGIN) as FailureReport.Recorded

        report.results.single().shouldBeInstanceOf<SinkResult.Failed>()
        FailingFailureSink().toString() shouldBe "FailingFailureSink"
    }

    test("the slow sink takes its delay and then writes") {
        val next = RecordingFailureSink()
        val handler = failures(this) { sinks { sink(name = "slow", sink = SlowFailureSink(2.seconds, next)) } }
        val started = testCoroutineScheduler.timeSource.markNow()

        handler.report(TestFailures.failure().cause, TestFailures.ORIGIN)

        started.elapsedNow() shouldBe 2.seconds
        next.reports.size shouldBe 1
        SlowFailureSink(2.seconds).toString() shouldBe "SlowFailureSink(2s)"
    }

    test("test failures carry their defaults") {
        val failure = TestFailures.failure(severity = FailureSeverity.Fatal, suppressed = 1)

        failure.id.value shouldBe TestFailures.ID
        failure.at shouldBe TestFailures.AT
        failure.thread shouldBe "test"
        failure.severity shouldBe FailureSeverity.Fatal
    }
})
