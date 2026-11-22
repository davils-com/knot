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

import java.io.File
import java.util.concurrent.TimeUnit

private const val PROBE: String = "com.davils.knot.shutdown.process.fixture.ShutdownProbeKt"
private const val WAIT_SECONDS: Long = 60L

internal data class ProbeRun(val status: Int, val lines: List<String>, val hasEnded: Boolean)

internal fun probe(scenario: Scenario, isSignalled: Boolean = false): ProbeRun {
    val java = File(System.getProperty("java.home"), "bin/java").path
    val builder = ProcessBuilder(java, "-cp", System.getProperty("java.class.path"), PROBE, scenario.name)
    builder.redirectErrorStream(true)
    val process = builder.start()
    val lines = process.inputStream.bufferedReader().use { reader ->
        val first = listOfNotNull(reader.readLine())
        if (isSignalled) process.toHandle().destroy()
        first + reader.readLines()
    }
    val hasEnded = process.waitFor(WAIT_SECONDS, TimeUnit.SECONDS)
    return ProbeRun(status = process.exitValue(), lines = lines, hasEnded = hasEnded)
}
