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

import com.davils.knot.environment.EnvironmentBuilder
import com.davils.knot.environment.EnvironmentViolationCodes
import com.davils.knot.environment.environment
import com.davils.knot.environment.schema.fixture.Duplicated
import com.davils.knot.environment.schema.fixture.LOOSE_SECRET
import com.davils.knot.environment.schema.fixture.Loose
import com.davils.knot.environment.schema.fixture.Mode
import com.davils.knot.environment.schema.fixture.Oversized
import com.davils.knot.environment.schema.fixture.ServerSettings
import com.davils.kore.core.validation.report.ValidationException
import com.davils.kore.core.validation.violation.ViolationCode
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotContain
import kotlin.time.Duration.Companion.seconds

private const val SECRET: String = "s3cr3t-token-value"
private const val TOKEN_NAME: String = "SERVER_TOKEN"
private const val DEFAULT_PORT: Int = 8080
private const val DEFAULT_TIMEOUT_SECONDS: Int = 30
private const val RATIO: Double = 0.25
private const val COUNT: Long = 7L

private fun violationsOf(
    schema: EnvironmentSchema,
    block: EnvironmentBuilder.() -> Unit,
): List<Pair<String, ViolationCode>> {
    val failure = shouldThrow<ValidationException> { schema.load(environment(block = block)) }
    return failure.report.violations.map { violation -> violation.path.toString() to violation.code }
}

class EnvironmentSchemaTest : FunSpec({
    context("loading") {
        test("reads, converts and defaults every setting") {
            val settings = ServerSettings.load(
                environment {
                    variable(name = TOKEN_NAME, value = SECRET)
                    variable(name = "SERVER_RATIO", value = RATIO.toString())
                    variable(name = "SERVER_COUNT", value = COUNT.toString())
                    variable(name = "SERVER_LEVEL", value = "HIGH")
                    variable(name = "SERVER_HOSTS", value = "a, b")
                    variable(name = "SERVER_MODE", value = "safe")
                    variable(name = "UNRELATED", value = "x")
                },
            )

            settings[ServerSettings.port] shouldBe DEFAULT_PORT
            settings.require(ServerSettings.token) shouldBe SECRET
            settings[ServerSettings.timeout] shouldBe DEFAULT_TIMEOUT_SECONDS.seconds
            settings[ServerSettings.ratio] shouldBe RATIO
            settings[ServerSettings.count] shouldBe COUNT
            settings[ServerSettings.debug] shouldBe false
            settings[ServerSettings.level] shouldBe "high"
            settings[ServerSettings.hosts] shouldBe listOf("a", "b")
            settings[ServerSettings.mode] shouldBe Mode.Safe
        }

        test("gives nothing for an optional setting that is not set") {
            val settings = ServerSettings.load(environment { variable(name = TOKEN_NAME, value = SECRET) })

            settings[ServerSettings.ratio] shouldBe null
            shouldThrow<IllegalStateException> { settings.require(ServerSettings.ratio) }
        }

        test("refuses a setting of another schema") {
            val settings = Loose.load(environment {})

            shouldThrow<IllegalArgumentException> { settings[ServerSettings.port] }
        }

        test("reports every problem at once, never with a value") {
            val failure = shouldThrow<ValidationException> {
                ServerSettings.load(
                    environment {
                        variable(name = "SERVER_PORT", value = "70000")
                        variable(name = "SERVER_TIMEOUT", value = SECRET)
                        variable(name = "SERVER_UNKNOWN", value = SECRET)
                    },
                )
            }
            val violations = failure.report.violations

            violations.map { violation -> violation.path.toString() to violation.code } shouldBe listOf(
                """settings["SERVER_PORT"]""" to EnvironmentViolationCodes.REJECTED_VALUE,
                """settings["SERVER_TOKEN"]""" to EnvironmentViolationCodes.MISSING_REQUIRED,
                """settings["SERVER_TIMEOUT"]""" to EnvironmentViolationCodes.UNREADABLE_VALUE,
                """settings["SERVER_UNKNOWN"]""" to EnvironmentViolationCodes.UNKNOWN_VARIABLE,
            )
            failure.message shouldNotContain SECRET
        }

        test("reports a failing expansion and a crashing rule") {
            violationsOf(Loose) {
                variable(name = "NAME", value = "${'$'}{MISSING}")
                variable(name = "CRASH", value = "1")
                expand()
            } shouldContainExactlyInAnyOrder listOf(
                """settings["NAME"]""" to EnvironmentViolationCodes.UNRESOLVED_REFERENCE,
                """settings["CRASH"]""" to EnvironmentViolationCodes.REJECTED_VALUE,
            )
        }

        test("refuses a broken schema") {
            violationsOf(Duplicated) {} shouldBe
                listOf("""settings["NAME"]""" to EnvironmentViolationCodes.INVALID_SCHEMA)
            violationsOf(Oversized) {}.first() shouldBe ("settings" to EnvironmentViolationCodes.INVALID_SCHEMA)
        }

        test("needs a prefix to be strict") {
            shouldThrow<IllegalArgumentException> { object : EnvironmentSchema(isStrict = true) {} }
        }

        test("shows the names of the settings and never their values") {
            val settings = ServerSettings.load(environment { variable(name = TOKEN_NAME, value = SECRET) })

            settings.toString() shouldBe "{SERVER_DEBUG=****, SERVER_PORT=****, SERVER_TIMEOUT=****, SERVER_TOKEN=****}"
        }
    }

    context("describing") {
        test("lists every setting with its type, requirement and default") {
            val described = ServerSettings.describe()

            described.take(2).map { description -> description.toString() } shouldBe listOf(
                "SERVER_PORT (int, optional, default 8080): the port to listen on",
                "SERVER_TOKEN (string, required): ",
            )
        }

        test("hides the default of a secret") {
            val described = Loose.describe()

            described[1].defaultText shouldBe "****"
            described[1].isSecret shouldBe true
            described.toString() shouldNotContain LOOSE_SECRET
        }

        test("is a value") {
            val first = Loose.describe().first()
            val again = Loose.describe().first()

            first shouldBe again
            first.hashCode() shouldBe again.hashCode()
            first.equals("NAME") shouldBe false
        }

        test("names a setting by its name and type") {
            ServerSettings.port.toString() shouldBe "Setting(SERVER_PORT: int)"
            ServerSettings.port.typeName shouldBe "int"
            ServerSettings.port.description shouldBe "the port to listen on"
            ServerSettings.token.isRequired shouldBe true
        }
    }
})
