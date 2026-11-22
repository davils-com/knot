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

package com.davils.knot.shutdown

import com.davils.knot.shutdown.fixture.Journal
import com.davils.knot.shutdown.fixture.virtualShutdown
import com.davils.knot.shutdown.task.ShutdownTaskName
import com.davils.kore.core.validation.report.ValidationException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.common.ExperimentalKotest
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

private const val BASE: String = "base"
private const val ADDED: String = "added"

class ShutdownRegistrationTest : FunSpec({
    coroutineTestScope = true

    context("registering") {
        test("adds tasks that run with the shutdown and may wait on earlier ones") {
            val journal = Journal()
            val shutdown = virtualShutdown { release { task(BASE) { action { journal.write(BASE) } } } }

            val registered = shutdown.register {
                release {
                    task(ADDED) {
                        after(BASE)
                        action { journal.write(ADDED) }
                    }
                }
            }
            val registration = registered.getOrThrow()

            registration.tasks shouldBe setOf(ShutdownTaskName(ADDED))
            shutdown.metrics().registeredTasks shouldBe 2
            shutdown.initiate()
            journal.entries shouldBe listOf(BASE, ADDED)
        }

        test("refuses an empty, a broken and a duplicate registration") {
            val shutdown = virtualShutdown { release { task(BASE) { action {} } } }

            val empty = shutdown.register {}
            val duplicate = shutdown.register { release { task(BASE) { action {} } } }

            empty.shouldBeInstanceOf<ShutdownRegistration.Refused>()
            duplicate.shouldBeInstanceOf<ShutdownRegistration.Refused>()
            val refusal = duplicate.refusal
            refusal.shouldBeInstanceOf<RegistrationRefusal.Invalid>()
            refusal.report.violations
                .single()
                .code shouldBe ShutdownViolationCodes.DUPLICATE_TASK
            shouldThrow<ValidationException> { empty.getOrThrow() }
            shutdown.metrics().refusedRegistrations shouldBe 2L
        }

        test("refuses a registration once the shutdown started") {
            val shutdown = virtualShutdown()
            shutdown.initiate()

            val registration = shutdown.register { release { task(ADDED) { action {} } } }

            registration shouldBe registration
            (registration as ShutdownRegistration.Refused).refusal shouldBe RegistrationRefusal.ShuttingDown
            shouldThrow<IllegalStateException> { registration.getOrThrow() }
        }

        test("refuses a registration beyond the limit of tasks") {
            val shutdown = virtualShutdown {
                stop { repeat(ShutdownLimits.MAX_TASKS) { index -> task("t$index") { action {} } } }
            }

            val registration = shutdown.register { release { task(ADDED) { action {} } } }

            registration.shouldBeInstanceOf<ShutdownRegistration.Refused>()
        }
    }

    context("unregistering") {
        test("removes the tasks once") {
            val shutdown = virtualShutdown()
            val registration = shutdown.register { release { task(ADDED) { action {} } } }.getOrThrow()

            registration.unregister() shouldBe true
            registration.unregister() shouldBe false
            shutdown.metrics().registeredTasks shouldBe 0
        }

        test("keeps tasks that another task waits on") {
            val shutdown = virtualShutdown()
            val base = shutdown.register { release { task(BASE) { action {} } } }.getOrThrow()
            shutdown.register {
                release {
                    task(ADDED) {
                        after(BASE)
                        action {}
                    }
                }
            }

            base.unregister() shouldBe false
        }

        test("keeps the tasks once the shutdown started") {
            val shutdown = virtualShutdown()
            val registration = shutdown.register { release { task(ADDED) { action {} } } }.getOrThrow()
            shutdown.initiate()

            registration.unregister() shouldBe false
        }
    }
})
