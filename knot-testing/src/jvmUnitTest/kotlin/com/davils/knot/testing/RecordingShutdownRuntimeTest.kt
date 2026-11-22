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

package com.davils.knot.testing

import com.davils.knot.shutdown.ShutdownReason
import com.davils.knot.shutdown.exit.ExitCode
import com.davils.knot.shutdown.process.Escalation
import com.davils.knot.shutdown.process.FatalErrorPolicy
import com.davils.knot.shutdown.process.install
import com.davils.knot.shutdown.shutdown
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import java.util.concurrent.Executors
import kotlin.time.Duration.Companion.seconds

private const val BROKEN: String = "broken"

private val background: CoroutineScope = CoroutineScope(Executors.newCachedThreadPool().asCoroutineDispatcher())

class RecordingShutdownRuntimeTest : FunSpec({
    test("records hooks, timers, exits and halts instead of touching the JVM") {
        val runtime = RecordingShutdownRuntime()
        val target = shutdown(background) {}
        val installation = target.install(runtime) { escalation = Escalation.Halt(after = 1.seconds) }

        runtime.hookNames shouldBe setOf("knot-shutdown-application")
        val exit = shouldThrow<ProcessExit> { installation.exit(ShutdownReason.Requested()) }
        exit.code shouldBe ExitCode.SUCCESS
        runtime.exits shouldBe listOf(ExitCode.SUCCESS)
        runtime.toString() shouldBe "RecordingShutdownRuntime(hooks=1, timers=0, exits=[0], halts=[])"
    }

    test("fires timers, which halt, and runs hooks") {
        val runtime = RecordingShutdownRuntime()
        val handle = runtime.schedule("timer", 1.seconds) { runtime.halt(ExitCode.TIMED_OUT) }
        var hasHookRun = false
        val hook = runtime.addHook("hook") { hasHookRun = true }

        runtime.timerDelays shouldBe listOf(1.seconds)
        shouldThrow<ProcessHalt> { runtime.fireTimers() }.code shouldBe ExitCode.TIMED_OUT
        runtime.halts shouldBe listOf(ExitCode.TIMED_OUT)
        runtime.runHooks()
        hasHookRun shouldBe true
        handle.close()
        hook.close()
        runtime.timerDelays shouldBe emptyList()
        runtime.hookNames shouldBe emptySet()
    }

    test("hands a crash to the installed handler or reports it") {
        val runtime = RecordingShutdownRuntime()
        val failure = IllegalStateException(BROKEN)
        runtime.crash(failure)
        runtime.reported shouldBe listOf(failure)

        shutdown(background) {}.install(runtime) { fatalErrors = FatalErrorPolicy.Exit() }
        val fatal = StackOverflowError(BROKEN)

        shouldThrow<ProcessExit> { runtime.crash(fatal) }.code shouldBe ExitCode.SOFTWARE
        runtime.reported shouldBe listOf(failure, fatal)
    }
})
