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

package com.davils.knot.shutdown.process.fixture

import com.davils.knot.shutdown.Shutdown
import com.davils.knot.shutdown.exit.ExitCode
import com.davils.knot.shutdown.process.Escalation
import com.davils.knot.shutdown.process.FatalErrorPolicy
import com.davils.knot.shutdown.process.ProcessInstallation
import com.davils.knot.shutdown.process.install
import com.davils.knot.shutdown.shutdown
import com.davils.knot.shutdown.task.ShutdownContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors
import kotlin.system.exitProcess
import kotlin.time.Duration.Companion.milliseconds

const val READY: String = "ready"
const val ANNOUNCED: String = "announced"
const val RELEASED: String = "released"
const val HALT_CODE: Int = 99
const val SYSTEM_EXIT_CODE: Int = 3
private const val BROKEN: String = "broken"
private const val WAIT_MILLIS: Long = 60_000L

fun main(arguments: Array<String>) {
    val scenario = Scenario.valueOf(arguments.single())
    val installation = installationOf(probeShutdown(scenario))
    mark(READY)
    run(scenario, installation)
}

private fun probeShutdown(scenario: Scenario): Shutdown = shutdown(probeScope()) {
    gracePeriod = scenario.gracePeriod
    announce { task("announce") { action { mark(ANNOUNCED) } } }
    release { task("release") { action { release(scenario) } } }
}

private fun probeScope(): CoroutineScope = CoroutineScope(Executors.newCachedThreadPool().asCoroutineDispatcher())

private fun installationOf(shutdown: Shutdown): ProcessInstallation = shutdown.install {
    fatalErrors = FatalErrorPolicy.Exit()
    escalation = Escalation.Halt(after = 500.milliseconds, exitCode = ExitCode(HALT_CODE))
}

private suspend fun ShutdownContext.release(scenario: Scenario) {
    if (scenario == Scenario.Hang) withContext(NonCancellable) { delay(WAIT_MILLIS) }
    check(scenario != Scenario.FailedExit) { BROKEN }
    mark(RELEASED)
}

private fun run(scenario: Scenario, installation: ProcessInstallation) {
    when (scenario) {
        Scenario.MainReturns -> {
            System.out.flush()
        }

        Scenario.SystemExit -> {
            exitProcess(SYSTEM_EXIT_CODE)
        }

        Scenario.CleanExit, Scenario.FailedExit, Scenario.Hang -> {
            runBlocking { installation.exit() }
        }

        Scenario.FatalError -> {
            crash()
        }

        Scenario.Signal -> {
            Thread.sleep(WAIT_MILLIS)
        }
    }
}

private fun crash() {
    val crashing = Thread { throw StackOverflowError(BROKEN) }
    crashing.start()
    crashing.join()
    Thread.sleep(WAIT_MILLIS)
}

private fun mark(line: String) {
    System.out.println(line)
    System.out.flush()
}
