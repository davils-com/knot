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

import com.davils.kore.core.Limits
import com.davils.kore.core.validation.report.ValidationException
import com.davils.kore.core.validation.violation.ViolationCode
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotContain

private const val HOME: String = "HOME"
private const val SECRET: String = "s3cr3t-value"
private const val SHELL: String = "SHELL"
private const val TOKEN: String = "TOKEN"
private const val EDITOR: String = "EDITOR"
private const val SCOPE_PATH: String = "scope"
private const val KNOT_HOME: String = "KNOT_HOME"
private const val PREFIX: String = "KNOT_"
private const val ANY_VALUE: String = "x"
private const val VARIABLES_PATH: String = "variables"
private const val HOME_OVERRIDE_PATH: String = """variables["HOME"]"""
private const val LAYERS_PATH: String = "layers"
private const val HOME_REMOVAL_PATH: String = """removals["HOME"]"""
private const val INVALID_NAME: String = "A=B"
private const val SH: String = "/bin/sh"

private fun refusalOf(block: EnvironmentBuilder.() -> Unit): List<Pair<String, ViolationCode>> {
    val failure = shouldThrow<ValidationException> { environment(block = block) }
    return failure.report.violations.map { violation -> violation.path.toString() to violation.code }
}

class EnvironmentBuilderTest : FunSpec({
    context("precedence") {
        val base = environment {
            variable(name = HOME, value = "/base")
            variable(name = SHELL, value = SH)
            variable(name = TOKEN, value = SECRET)
        }
        val first = environment {
            variable(name = HOME, value = "/first")
            variable(name = EDITOR, value = "vi")
        }
        val second = environment { variable(name = HOME, value = "/second") }

        test("reads the base when nothing is stacked on it") {
            val composed = environment(base = base) {}

            composed.variable(SHELL) shouldBe SH
        }

        test("lets a later layer win over an earlier one and over the base") {
            val composed = environment(base = base) {
                layer(first)
                layer(second)
            }

            composed.variable(HOME) shouldBe "/second"
            composed.variable(EDITOR) shouldBe "vi"
        }

        test("lets an override win over every layer") {
            val composed = environment(base = base) {
                layer(first)
                variable(name = HOME, value = "/override")
            }

            composed.variable(HOME) shouldBe "/override"
        }

        test("hides a removed variable of the base and of every layer") {
            val composed = environment(base = base) {
                layer(first)
                remove(HOME)
                remove(TOKEN)
            }

            composed.variable(HOME) shouldBe null
            composed.variable(TOKEN) shouldBe null
            composed.names shouldContainExactlyInAnyOrder setOf(SHELL, EDITOR)
        }

        test("stacks a layer declared with a block") {
            val composed = environment(base = base) {
                layer { variable(name = HOME, value = "/inline") }
            }

            composed.variable(HOME) shouldBe "/inline"
        }

        test("leaves unknown variables unset") {
            environment(base = base) { layer(first) }.variable("MISSING") shouldBe null
        }
    }

    context(SCOPE_PATH) {
        val base = environment {
            variable(name = KNOT_HOME, value = "/opt/knot")
            variable(name = PREFIX, value = "bare")
            variable(name = HOME, value = "/home/alice")
        }

        test("reads the variables of the prefix under their short names") {
            val scoped = environment(base = base) { scope(PREFIX) }

            scoped.variable(HOME) shouldBe "/opt/knot"
            scoped.variable(KNOT_HOME) shouldBe null
        }

        test("lists only the short names of the prefix, never the bare prefix") {
            val scoped = environment(base = base) { scope(PREFIX) }

            scoped.names shouldContainExactly setOf(HOME)
        }

        test("applies to overrides and removals, which name the prefix") {
            val scoped = environment(base = base) {
                variable(name = "KNOT_LEVEL", value = "debug")
                remove(KNOT_HOME)
                scope(PREFIX)
            }

            scoped.variable("LEVEL") shouldBe "debug"
            scoped.variable(HOME) shouldBe null
        }
    }

    context("reading live") {
        test("sees a change of a layer after the environment was built") {
            val changing = ChangingEnvironment()
            val composed = environment { layer(changing) }

            composed.variable(HOME) shouldBe null
            changing.value = "/later"

            composed.variable(HOME) shouldBe "/later"
            composed.names shouldContainExactly setOf(HOME)
        }
    }

    context("names") {
        test("refuses a blank name") {
            refusalOf { variable(name = " ", value = ANY_VALUE) } shouldBe
                listOf(VARIABLES_PATH to EnvironmentViolationCodes.INVALID_NAME)
        }

        test("refuses a name with an equals sign or a NUL character") {
            refusalOf {
                variable(name = INVALID_NAME, value = ANY_VALUE)
                remove("A\u0000")
            } shouldBe listOf(
                VARIABLES_PATH to EnvironmentViolationCodes.INVALID_NAME,
                "removals" to EnvironmentViolationCodes.INVALID_NAME,
            )
        }

        test("refuses a name longer than the limit and never repeats it") {
            val long = "N".repeat(Limits.MAX_NAME_LENGTH + 1)
            val failure = shouldThrow<ValidationException> { environment { variable(name = long, value = ANY_VALUE) } }

            failure.report.violations.map { violation -> violation.code } shouldBe
                listOf(EnvironmentViolationCodes.TOO_LONG)
            failure.report.render() shouldNotContain long
        }

        test("accepts a name of exactly the limit") {
            val name = "N".repeat(Limits.MAX_NAME_LENGTH)

            environment { variable(name = name, value = ANY_VALUE) }.variable(name) shouldBe ANY_VALUE
        }
    }

    context("values") {
        test("refuses a value with a NUL character under the name of its variable") {
            refusalOf { variable(name = HOME, value = "a\u0000b") } shouldBe
                listOf(HOME_OVERRIDE_PATH to EnvironmentViolationCodes.INVALID_VALUE)
        }

        test("refuses a value longer than the limit") {
            refusalOf { variable(name = HOME, value = "v".repeat(Limits.MAX_ARGUMENT_LENGTH + 1)) } shouldBe
                listOf(HOME_OVERRIDE_PATH to EnvironmentViolationCodes.TOO_LONG)
        }

        test("never shows a refused value") {
            val failure = shouldThrow<ValidationException> {
                environment { variable(name = TOKEN, value = "$SECRET\u0000") }
            }

            failure.report.render() shouldNotContain SECRET
            failure.message shouldNotContain SECRET
        }
    }

    context("changes") {
        test("refuses a variable that is set twice") {
            refusalOf {
                variable(name = HOME, value = "a")
                variable(name = HOME, value = "b")
            } shouldBe listOf(HOME_OVERRIDE_PATH to EnvironmentViolationCodes.DUPLICATE_CHANGE)
        }

        test("refuses a variable that is removed twice") {
            refusalOf {
                remove(HOME)
                remove(HOME)
            } shouldBe listOf(HOME_REMOVAL_PATH to EnvironmentViolationCodes.DUPLICATE_CHANGE)
        }

        test("refuses a variable that is both set and removed") {
            refusalOf {
                remove(HOME)
                variable(name = HOME, value = "a")
            } shouldBe listOf(HOME_REMOVAL_PATH to EnvironmentViolationCodes.CONFLICTING_CHANGE)
        }

        test("reports too many changes once and keeps none beyond the limit") {
            refusalOf {
                repeat(EnvironmentLimits.MAX_CHANGES + 2) { index -> variable(name = "V$index", value = ANY_VALUE) }
            } shouldBe listOf(VARIABLES_PATH to EnvironmentViolationCodes.TOO_MANY_CHANGES)
        }

        test("counts overrides and removals together") {
            refusalOf {
                repeat(EnvironmentLimits.MAX_CHANGES) { index -> variable(name = "V$index", value = ANY_VALUE) }
                remove("EXTRA")
            } shouldBe listOf(VARIABLES_PATH to EnvironmentViolationCodes.TOO_MANY_CHANGES)
        }
    }

    context(LAYERS_PATH) {
        test("reports too many layers once") {
            refusalOf {
                repeat(EnvironmentLimits.MAX_LAYERS + 2) { layer(Environment.EMPTY) }
            } shouldBe listOf(LAYERS_PATH to EnvironmentViolationCodes.TOO_MANY_LAYERS)
        }

        test("reports too many layers declared with blocks without running the extra blocks") {
            var ran = 0

            refusalOf {
                repeat(EnvironmentLimits.MAX_LAYERS + 1) { layer { ran += 1 } }
            } shouldBe listOf(LAYERS_PATH to EnvironmentViolationCodes.TOO_MANY_LAYERS)
            ran shouldBe EnvironmentLimits.MAX_LAYERS
        }

        test("reports the problems of a layer declared with a block under its position") {
            refusalOf {
                layer { variable(name = HOME, value = "ok") }
                layer { variable(name = INVALID_NAME, value = ANY_VALUE) }
            } shouldBe listOf("layers[1].variables" to EnvironmentViolationCodes.INVALID_NAME)
        }
    }

    context("scope rules") {
        test("refuses a second scope") {
            refusalOf {
                scope("A_")
                scope("B_")
                scope("C_")
            } shouldBe listOf(SCOPE_PATH to EnvironmentViolationCodes.REPEATED_SCOPE)
        }

        test("refuses an invalid prefix") {
            refusalOf { scope("") } shouldBe listOf(SCOPE_PATH to EnvironmentViolationCodes.INVALID_NAME)
        }
    }

    context("evaluation") {
        test("refuses a declaration after the environment was built") {
            val builder = EnvironmentBuilder(Environment.EMPTY)
            builder.build()

            shouldThrow<IllegalStateException> { builder.variable(name = HOME, value = ANY_VALUE) }
            shouldThrow<IllegalStateException> { builder.remove(HOME) }
            shouldThrow<IllegalStateException> { builder.layer(Environment.EMPTY) }
            shouldThrow<IllegalStateException> { builder.layer {} }
            shouldThrow<IllegalStateException> { builder.scope("A_") }
        }

        test("reports every problem at once under the subject environment") {
            val failure = shouldThrow<ValidationException> {
                environment {
                    variable(name = "", value = ANY_VALUE)
                    remove("=")
                }
            }

            failure.report.subject shouldBe "environment"
            failure.report.violations.size shouldBe 2
        }
    }
})
