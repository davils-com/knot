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

import com.davils.knot.shutdown.process.fixture.ANNOUNCED
import com.davils.knot.shutdown.process.fixture.HALT_CODE
import com.davils.knot.shutdown.process.fixture.READY
import com.davils.knot.shutdown.process.fixture.RELEASED
import com.davils.knot.shutdown.process.fixture.SYSTEM_EXIT_CODE
import com.davils.knot.shutdown.process.fixture.Scenario
import com.davils.knot.shutdown.process.fixture.probe
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import java.io.File

private const val SIGTERM_STATUS: Int = 143
private const val FATAL_STATUS: Int = 70

class ShutdownProcessTest : FunSpec({
    test("runs the shutdown when main returns and keeps the status of the JVM") {
        val run = probe(Scenario.MainReturns)

        run.status shouldBe 0
        run.lines shouldContainAll listOf(READY, ANNOUNCED, RELEASED)
    }

    test("runs the shutdown on System.exit with the status it was given") {
        val run = probe(Scenario.SystemExit)

        run.status shouldBe SYSTEM_EXIT_CODE
        run.lines shouldContainAll listOf(ANNOUNCED, RELEASED)
    }

    test("ends a clean exit with 0 and a failed one with 1") {
        probe(Scenario.CleanExit).status shouldBe 0
        probe(Scenario.FailedExit).status shouldBe 1
    }

    test("ends on a fatal error with the fatal exit code after the shutdown") {
        val run = probe(Scenario.FatalError)

        run.status shouldBe FATAL_STATUS
        run.lines shouldContainAll listOf(ANNOUNCED, RELEASED)
    }

    test("halts a shutdown that overruns its budget") {
        val run = probe(Scenario.Hang)

        run.status shouldBe HALT_CODE
        run.lines shouldNotContain RELEASED
    }

    test("runs the shutdown on SIGTERM").config(enabled = File.separatorChar == '/') {
        val run = probe(Scenario.Signal, isSignalled = true)

        run.status shouldBe SIGTERM_STATUS
        run.lines shouldContainAll listOf(READY, ANNOUNCED, RELEASED)
    }
})
