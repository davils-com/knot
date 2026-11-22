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

package com.davils.knot.failure

import com.davils.knot.failure.fixture.ProcessEnd
import com.davils.knot.failure.fixture.RecordingRuntime
import com.davils.knot.failure.render.CrashReportRenderer
import com.davils.knot.shutdown.exit.ExitCode
import com.davils.knot.shutdown.process.install
import com.davils.knot.shutdown.shutdown
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import java.util.concurrent.Executors
import kotlin.time.Clock

private const val BROKEN: String = "broken"

private val background: CoroutineScope = CoroutineScope(Executors.newCachedThreadPool().asCoroutineDispatcher())

class UncaughtFailuresTest : FunSpec({
    context("the handler of uncaught exceptions") {
        test("hands a failure to the handler with the thread as its origin, then to the previous handler") {
            val runtime = RecordingRuntime()
            val seen = mutableListOf<Throwable>()
            runtime.installUncaughtHandler { _, cause -> seen += cause }
            val handler = failures(background)
            handler.installUncaughtHandler(runtime)
            val cause = IllegalStateException(BROKEN)
            val reported = handler.failures

            runtime.crash(cause)
            handler.drain()

            seen shouldBe listOf(cause)
            handler.metrics().reported shouldBe 1L
            reported.toString() shouldContain "Flow"
        }

        test("reports the failure through the runtime when there was no previous handler") {
            val runtime = RecordingRuntime()
            val handler = failures(background)
            handler.installUncaughtHandler(runtime)

            runtime.crash(IllegalStateException(BROKEN))

            runtime.reported.size shouldBe 1
        }

        test("gives the previous handler back once and keeps a later one") {
            val runtime = RecordingRuntime()
            val previous = Thread.UncaughtExceptionHandler { _, _ -> }
            runtime.installUncaughtHandler(previous)
            val handler = failures(background)
            val handle = handler.installUncaughtHandler(runtime)

            handle.close()
            handle.close()
            runtime.uncaughtHandler() shouldBe previous
            val second = handler.installUncaughtHandler(runtime)
            val later = Thread.UncaughtExceptionHandler { _, _ -> }
            runtime.installUncaughtHandler(later)
            second.close()
            runtime.uncaughtHandler() shouldBe later
        }

        test("is removed when the failure handler closes") {
            val runtime = RecordingRuntime()
            val handler = failures(background)
            handler.installUncaughtHandler(runtime)

            handler.close()

            runtime.uncaughtHandler() shouldBe null
        }

        test("installs into the real JVM by default") {
            val original = Thread.getDefaultUncaughtExceptionHandler()
            val handler = failures(background)

            handler.installUncaughtHandler().close()

            Thread.getDefaultUncaughtExceptionHandler() shouldBe original
        }
    }

    context("ending the process") {
        test("lets a fatal failure end the process through the installation after its report") {
            val runtime = RecordingRuntime()
            val installation = shutdown(background) {}.install(runtime)
            val handler = failures(background) {
                classify { fatal<IllegalStateException>() }
                exitThrough(installation)
            }

            handler.report(IllegalStateException(BROKEN), FailureOrigin.Thread("main"))

            runtime.ends.single().code shouldBe ExitCode.SOFTWARE
            runtime.ends.single().shouldBeEnd()
        }
    }

    context("the text report on the JVM") {
        test("survives a cycle of causes") {
            val first = IllegalStateException(BROKEN)
            val second = IllegalArgumentException(BROKEN, first)
            first.initCause(second)
            val failure = UnexpectedFailure(
                id = FailureId(BROKEN),
                cause = first,
                origin = FailureOrigin.Thread(BROKEN),
                severity = FailureSeverity.Fatal,
                fingerprint = FailureFingerprint.of(first),
                at = Clock.System.now(),
                thread = null,
                suppressed = 0,
            )

            val text = CrashReportRenderer.text().render(failure)

            text.lines().count { line -> line.startsWith("caused by") } shouldBe 1
            val classifying = failures(background) { classify { fatal<UnsupportedOperationException>() } }
            val classification = classifying.configuration.classification
            classification.isFatal(first) shouldBe false
        }
    }
})

private fun ProcessEnd.shouldBeEnd() {
    isHalt shouldBe false
}
