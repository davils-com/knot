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

package com.davils.knot.environment.schema

import com.davils.knot.environment.CrashingEnvironment
import com.davils.knot.environment.EnvironmentLimits
import com.davils.knot.environment.EnvironmentViolationCodes
import com.davils.knot.environment.environment
import com.davils.knot.environment.schema.fixture.ServerSettings
import com.davils.kore.core.validation.report.ValidationException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

private const val TOKEN_NAME: String = "SERVER_TOKEN"
private const val HOSTS_NAME: String = "SERVER_HOSTS"
private const val TOKEN: String = "token"

class SchemaEdgeTest : FunSpec({
    context("lists") {
        test("drop empty elements and trim the others") {
            val settings = ServerSettings.load(
                environment {
                    variable(name = TOKEN_NAME, value = TOKEN)
                    variable(name = HOSTS_NAME, value = ",, a ,b,")
                },
            )

            settings[ServerSettings.hosts] shouldBe listOf("a", "b")
        }

        test("refuse more elements than the limit") {
            val failure = shouldThrow<ValidationException> {
                ServerSettings.load(
                    environment {
                        variable(name = TOKEN_NAME, value = TOKEN)
                        variable(name = HOSTS_NAME, value = ",".repeat(EnvironmentLimits.MAX_ELEMENTS))
                    },
                )
            }

            failure.report.violations
                .single()
                .code shouldBe EnvironmentViolationCodes.UNREADABLE_VALUE
        }
    }

    context("an environment that fails to read") {
        test("makes every setting unreadable") {
            val failure = shouldThrow<ValidationException> { ServerSettings.load(CrashingEnvironment()) }
            val codes = failure.report.violations
                .map { violation -> violation.code }
                .toSet()

            codes shouldBe setOf(EnvironmentViolationCodes.UNREADABLE_VALUE)
        }
    }
})
