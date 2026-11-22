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

package com.davils.knot.shutdown

import com.davils.kore.core.validation.report.ValidationException
import com.davils.kore.core.validation.violation.ViolationCode
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds

private const val TASK: String = "task"
private const val OTHER: String = "other"
private const val TASK_PATH: String = """tasks["task"]"""
private const val AFTER_PATH: String = "$TASK_PATH.after"

private val detachedScope: CoroutineScope = CoroutineScope(Job())

private fun violationsOf(block: ShutdownBuilder.() -> Unit): List<Pair<String, ViolationCode>> {
    val failure = shouldThrow<ValidationException> { shutdown(detachedScope, block) }
    return failure.report.violations.map { violation -> violation.path.toString() to violation.code }
}

private fun warningsOf(block: ShutdownBuilder.() -> Unit): List<ViolationCode> {
    val builder = ShutdownBuilder()
    builder.block()
    return builder.validate().warnings.map { violation -> violation.code }
}

class ShutdownValidationTest : FunSpec({
    context("the shutdown") {
        test("refuses a broken name and broken budgets") {
            violationsOf {
                name = "-bad name"
                gracePeriod = 2.hours
                taskTimeout = Duration.ZERO
                cancellationGrace = Duration.INFINITE
                eventCapacity = 0
            } shouldContainExactlyInAnyOrder listOf(
                "name" to ShutdownViolationCodes.INVALID_NAME,
                "gracePeriod" to ShutdownViolationCodes.INVALID_GRACE_PERIOD,
                "taskTimeout" to ShutdownViolationCodes.INVALID_TIMEOUT,
                "cancellationGrace" to ShutdownViolationCodes.INVALID_CANCELLATION_GRACE,
                "eventCapacity" to ShutdownViolationCodes.INVALID_EVENT_CAPACITY,
            )
        }

        test("refuses a name that is too long") {
            violationsOf { name = "a".repeat(ShutdownLimits.MAX_NAME_LENGTH + 1) } shouldBe
                listOf("name" to ShutdownViolationCodes.INVALID_NAME)
        }

        test("warns about timeouts longer than the grace period") {
            warningsOf {
                gracePeriod = 1.seconds
                taskTimeout = 2.seconds
                stop {
                    task(TASK) {
                        timeout = 3.seconds
                        action {}
                    }
                }
            } shouldBe listOf(
                ShutdownViolationCodes.TIMEOUT_EXCEEDS_GRACE,
                ShutdownViolationCodes.TIMEOUT_EXCEEDS_GRACE,
            )
        }

        test("refuses more tasks than the limit") {
            violationsOf {
                stop { repeat(ShutdownLimits.MAX_TASKS + 1) { index -> task("t$index") { action {} } } }
            } shouldBe listOf("tasks" to ShutdownViolationCodes.TOO_MANY_TASKS)
        }
    }

    context("a task") {
        test("refuses a broken name, a broken timeout and a missing action") {
            violationsOf {
                stop {
                    task("bad name") { action {} }
                    task(TASK) { timeout = Duration.ZERO }
                }
            } shouldContainExactlyInAnyOrder listOf(
                """tasks["bad name"].name""" to ShutdownViolationCodes.INVALID_TASK_NAME,
                "$TASK_PATH.timeout" to ShutdownViolationCodes.INVALID_TIMEOUT,
                TASK_PATH to ShutdownViolationCodes.MISSING_ACTION,
            )
        }

        test("refuses a name declared twice") {
            violationsOf {
                stop { task(TASK) { action {} } }
                release { task(TASK) { action {} } }
            } shouldBe listOf(TASK_PATH to ViolationCode.DUPLICATE)
        }

        test("refuses to wait on too many tasks") {
            val names = Array(ShutdownLimits.MAX_DEPENDENCIES + 1) { index -> "d$index" }

            violationsOf {
                stop {
                    names.forEach { name -> task(name) { action {} } }
                    task(TASK) {
                        names.forEach { name -> after(name) }
                        action {}
                    }
                }
            } shouldBe listOf(AFTER_PATH to ShutdownViolationCodes.TOO_MANY_DEPENDENCIES)
        }
    }

    context("dependencies") {
        test("refuse an unknown task and a task of a later phase") {
            violationsOf {
                release { task(OTHER) { action {} } }
                stop {
                    task(TASK) {
                        after("missing", OTHER)
                        action {}
                    }
                }
            } shouldContainExactlyInAnyOrder listOf(
                AFTER_PATH to ShutdownViolationCodes.UNKNOWN_DEPENDENCY,
                AFTER_PATH to ShutdownViolationCodes.LATER_PHASE_DEPENDENCY,
            )
        }

        test("allow a task of an earlier phase") {
            shutdown(detachedScope) {
                stop { task(OTHER) { action {} } }
                release {
                    task(TASK) {
                        after(OTHER)
                        action {}
                    }
                }
            }.configuration.tasks.size shouldBe 2
        }

        test("refuse a cycle and every task waiting on it") {
            violationsOf {
                stop {
                    task(TASK) {
                        after(OTHER)
                        action {}
                    }
                    task(OTHER) {
                        after(TASK)
                        action {}
                    }
                    task("waiting") {
                        after(TASK)
                        action {}
                    }
                    task("self") {
                        after("self")
                        action {}
                    }
                }
            }.map { (path, _) -> path } shouldContainExactlyInAnyOrder listOf(
                TASK_PATH,
                """tasks["other"]""",
                """tasks["waiting"]""",
                """tasks["self"]""",
            )
        }
    }

    context("an evaluated builder") {
        test("refuses every further declaration") {
            val builder = ShutdownBuilder()
            builder.build()

            shouldThrow<IllegalStateException> { builder.stop {} }
        }
    }
})
