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

package com.davils.knot.shutdown.process

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes

private const val NAME: String = "knot-test"
private const val BROKEN: String = "broken"
private const val WAIT_SECONDS: Long = 10L

class JvmShutdownRuntimeTest : FunSpec({
    val runtime = ShutdownRuntime.Jvm

    test("adds a hook and removes it again") {
        val hook = runtime.addHook(NAME) {}

        hook.close()
        hook.close()
    }

    test("runs a scheduled action and cancels one that has not run") {
        val ran = CountDownLatch(1)
        runtime.schedule(NAME, 1.milliseconds) { ran.countDown() }
        val cancelled = runtime.schedule(NAME, 1.minutes) { error(BROKEN) }

        ran.await(WAIT_SECONDS, TimeUnit.SECONDS) shouldBe true
        cancelled.close()
    }

    test("installs and reads the default handler of uncaught exceptions") {
        val original = runtime.uncaughtHandler()
        val handler = Thread.UncaughtExceptionHandler { _, _ -> }
        try {
            runtime.installUncaughtHandler(handler)
            runtime.uncaughtHandler() shouldBe handler
        } finally {
            runtime.installUncaughtHandler(original)
        }
    }

    test("reports an uncaught exception as the JVM does") {
        val original = System.err
        val captured = ByteArrayOutputStream()
        PrintStream(captured, true, Charsets.UTF_8).use { stream ->
            try {
                System.setErr(stream)
                runtime.reportUncaught(Thread.currentThread(), IllegalStateException(BROKEN))
            } finally {
                System.setErr(original)
            }
        }

        captured.toString(Charsets.UTF_8) shouldContain "Exception in thread \"${Thread.currentThread().name}\""
    }
})
