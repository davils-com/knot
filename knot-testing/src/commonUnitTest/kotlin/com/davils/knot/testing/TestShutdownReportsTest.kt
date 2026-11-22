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

import com.davils.knot.shutdown.ShutdownPhase
import com.davils.knot.shutdown.exit.ExitCode
import com.davils.knot.shutdown.report.TaskOutcome
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class TestShutdownReportsTest : FunSpec({
    test("builds reports of tasks, phases and shutdowns") {
        val failed = TestShutdownReports.task("cache", outcome = TaskOutcome.DeadlineExceeded)
        val report = TestShutdownReports.report(
            TestShutdownReports.phase(ShutdownPhase.Release, TestShutdownReports.task("database"), failed),
            exitCode = ExitCode.FAILURE,
        )

        report.tasks.map { task -> task.name.value } shouldBe listOf("database", "cache")
        report.isClean shouldBe false
        report.startedAt shouldBe TestShutdownReports.STARTED_AT
        report.exitCode shouldBe ExitCode.FAILURE
        TestShutdownReports.report().exitCode shouldBe ExitCode.SUCCESS
    }
})
