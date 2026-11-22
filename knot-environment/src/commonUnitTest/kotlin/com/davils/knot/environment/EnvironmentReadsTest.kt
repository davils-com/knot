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

import com.davils.kore.core.arguments.conversion.ConversionException
import com.davils.kore.core.arguments.conversion.Converter
import com.davils.kore.core.arguments.conversion.Converters
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotContain
import kotlin.time.Duration.Companion.seconds

private const val PORT: String = "PORT"
private const val MISSING: String = "MISSING"
private const val SECRET: String = "t0ken-value"
private const val PORT_NUMBER: Int = 8080
private const val BIG_NUMBER: Long = 9_000_000_000L
private const val RATIO_VALUE: Double = 0.5
private const val TIMEOUT_SECONDS: Int = 30
private const val MAX_TIMEOUT_SECONDS: Int = 10
private const val FALLBACK: Int = 1
private const val THIRD: Int = 3
private const val HOSTS: String = "HOSTS"
private const val TOKEN: String = "TOKEN"
private const val TIMEOUT: String = "TIMEOUT"

private val variables: Environment = environment {
    variable(name = PORT, value = PORT_NUMBER.toString())
    variable(name = "BIG", value = BIG_NUMBER.toString())
    variable(name = "RATIO", value = RATIO_VALUE.toString())
    variable(name = "ENABLED", value = "true")
    variable(name = TIMEOUT, value = "30s")
    variable(name = "LEVEL", value = "info")
    variable(name = HOSTS, value = " a, b ,,c ")
    variable(name = "PORTS", value = "1,2,3")
    variable(name = "LABELS", value = "team=core, tier = gold ,team=edge")
    variable(name = "BROKEN_LABELS", value = "=x")
    variable(name = TOKEN, value = SECRET)
}

private fun failureOf(read: Environment.() -> Any?): EnvironmentException =
    shouldThrow<EnvironmentException> { variables.read() }

class EnvironmentReadsTest : FunSpec({
    context("typed reads") {
        test("convert the value to the type that is read") {
            variables.int(PORT) shouldBe PORT_NUMBER
            variables.long("BIG") shouldBe BIG_NUMBER
            variables.double("RATIO") shouldBe RATIO_VALUE
            variables.boolean("ENABLED") shouldBe true
            variables.duration(TIMEOUT) shouldBe TIMEOUT_SECONDS.seconds
            variables.enum<Level>("LEVEL") shouldBe Level.Info
        }

        test("give nothing for a variable that is not set") {
            variables.int(MISSING) shouldBe null
            variables.list(MISSING) shouldBe null
            variables.list(MISSING, Converters.int) shouldBe null
            variables.map(MISSING) shouldBe null
            variables.isSet(MISSING) shouldBe false
            variables.isSet(PORT) shouldBe true
        }

        test("fall back to a default or require the value") {
            variables.value(MISSING, Converters.int, default = FALLBACK) shouldBe FALLBACK
            variables.value(PORT, Converters.int, default = FALLBACK) shouldBe PORT_NUMBER
            variables.requireValue(PORT, Converters.int) shouldBe PORT_NUMBER
            failureOf { requireValue(MISSING, Converters.int) }.code shouldBe EnvironmentViolationCodes.MISSING_REQUIRED
        }

        test("refuse a value of the wrong type without showing it") {
            val failure = failureOf { int(TOKEN) }

            failure.code shouldBe EnvironmentViolationCodes.UNREADABLE_VALUE
            failure.message.orEmpty() shouldNotContain SECRET
            failure.message shouldBe """The variable "TOKEN" cannot be read as "int""""
            failure.cause shouldBe null
        }

        test("contain whatever a converter of the caller throws") {
            val throwing = Converter<Int> { raw -> throw ConversionException("refused $raw") }
            val crashing = Converter<Int> { error("crashed") }

            failureOf { value(TOKEN, throwing) }.message.orEmpty() shouldNotContain SECRET
            failureOf { value(TOKEN, crashing) }.code shouldBe EnvironmentViolationCodes.UNREADABLE_VALUE
        }

        test("check the bounds of a duration") {
            failureOf {
                duration(
                    TIMEOUT,
                    max = MAX_TIMEOUT_SECONDS.seconds,
                )
            }.code shouldBe EnvironmentViolationCodes.UNREADABLE_VALUE
        }
    }

    context("lists and maps") {
        test("split a list, trim its elements and drop empty ones") {
            variables.list(HOSTS) shouldBe listOf("a", "b", "c")
            variables.list("PORTS", Converters.int) shouldBe (FALLBACK..THIRD).toList()
            variables.list(PORT, separator = ';') shouldBe listOf("8080")
        }

        test("split a map, a later entry winning") {
            variables.map("LABELS") shouldBe mapOf("team" to "edge", "tier" to "gold")
        }

        test("refuse a map entry without a key") {
            failureOf { map("BROKEN_LABELS") }.code shouldBe EnvironmentViolationCodes.UNREADABLE_VALUE
        }

        test("refuse more elements than the limit") {
            val many = environment { variable(name = "MANY", value = ",".repeat(EnvironmentLimits.MAX_ELEMENTS)) }

            shouldThrow<EnvironmentException> { many.list("MANY") }.code shouldBe
                EnvironmentViolationCodes.UNREADABLE_VALUE
        }

        test("refuse an element of the wrong type") {
            failureOf { list(HOSTS, Converters.int) }.code shouldBe EnvironmentViolationCodes.UNREADABLE_VALUE
        }
    }
})
