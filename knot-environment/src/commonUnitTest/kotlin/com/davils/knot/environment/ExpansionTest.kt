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

package com.davils.knot.environment

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotContain

private const val HOST: String = "HOST"
private const val URL: String = "URL"
private const val SECRET: String = "p4ssw0rd-value"

private fun expanded(onMissing: MissingReference = MissingReference.Fail, block: EnvironmentBuilder.() -> Unit) =
    environment {
        block()
        expand(onMissing)
    }

private fun failureOf(environment: Environment, name: String): EnvironmentException =
    shouldThrow<EnvironmentException> { environment.variable(name) }

class ExpansionTest : FunSpec({
    context("references") {
        test("are replaced by the values they name, recursively") {
            val composed = expanded {
                variable(name = HOST, value = "example.com")
                variable(name = "PORT", value = "8443")
                variable(name = "ORIGIN", value = "${'$'}{HOST}:${'$'}{PORT}")
                variable(name = URL, value = "https://${'$'}{ORIGIN}/")
            }

            composed.variable(URL) shouldBe "https://example.com:8443/"
        }

        test("use their default when the variable is not set") {
            val composed = expanded {
                variable(name = URL, value = "${'$'}{HOST:-localhost}:${'$'}{PORT:-}")
            }

            composed.variable(URL) shouldBe "localhost:"
        }

        test("keep a lone dollar and turn two dollars into one") {
            expanded {
                variable(
                    name = "PRICE",
                    value = "5$ and $$ and \$x",
                )
            }.variable("PRICE") shouldBe "5$ and $ and \$x"
        }

        test("are not replaced without expand") {
            environment { variable(name = URL, value = "${'$'}{HOST}") }.variable(URL) shouldBe "${'$'}{HOST}"
        }

        test("are read through the scope from the unscoped names") {
            val composed = environment {
                variable(name = "APP_URL", value = "${'$'}{APP_HOST}/")
                variable(name = "APP_HOST", value = "example.com")
                scope("APP_")
                expand()
            }

            composed.variable(URL) shouldBe "example.com/"
        }
    }

    context("missing references") {
        val block: EnvironmentBuilder.() -> Unit = { variable(name = URL, value = "${'$'}{HOST}/") }

        test("fail by default, naming the variable that was read") {
            val failure = failureOf(expanded(block = block), URL)

            failure.code shouldBe EnvironmentViolationCodes.UNRESOLVED_REFERENCE
            failure.variable shouldBe URL
        }

        test("expand to nothing or stay as written when asked to") {
            expanded(MissingReference.Empty, block).variable(URL) shouldBe "/"
            expanded(MissingReference.Keep, block).variable(URL) shouldBe "${'$'}{HOST}/"
        }
    }

    context("broken references") {
        test("fail on a cycle") {
            val composed = expanded {
                variable(name = "A", value = "${'$'}{B}")
                variable(name = "B", value = "${'$'}{A}")
            }

            failureOf(composed, "A").code shouldBe EnvironmentViolationCodes.EXPANSION_CYCLE
        }

        test("fail on a malformed reference") {
            failureOf(expanded { variable(name = URL, value = "${'$'}{HOST") }, URL).code shouldBe
                EnvironmentViolationCodes.INVALID_REFERENCE
            failureOf(expanded { variable(name = URL, value = "${'$'}{}") }, URL).code shouldBe
                EnvironmentViolationCodes.INVALID_REFERENCE
        }

        test("fail on a chain deeper than the limit") {
            val depth = EnvironmentLimits.MAX_EXPANSION_DEPTH + 1
            val composed = expanded {
                repeat(depth) { index -> variable(name = "V$index", value = "${'$'}{V${index + 1}}") }
                variable(name = "V$depth", value = "end")
            }

            failureOf(composed, "V0").code shouldBe EnvironmentViolationCodes.EXPANSION_TOO_LARGE
        }

        test("fail on a value that grows beyond the limit, without exhausting memory") {
            val composed = expanded {
                variable(name = "V0", value = "x".repeat(4_096))
                repeat(12) { index -> variable(name = "V${index + 1}", value = "${'$'}{V$index}${'$'}{V$index}") }
            }

            failureOf(composed, "V12").code shouldBe EnvironmentViolationCodes.EXPANSION_TOO_LARGE
        }

        test("never show a value in the message") {
            val composed = expanded { variable(name = URL, value = "$SECRET${'$'}{MISSING}") }

            failureOf(composed, URL).message.orEmpty() shouldNotContain SECRET
        }
    }
})
