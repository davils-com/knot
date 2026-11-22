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

import com.davils.kore.core.validation.report.ValidationException
import com.davils.kore.core.validation.violation.ViolationCode
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe

private const val HOME: String = "HOME"
private const val LEGACY_HOME: String = "OLD_HOME"
private const val PATH: String = "PATH"
private const val VALUE: String = "value"
private const val OTHER: String = "other"
private const val PREFIX: String = "APP_"
private const val APP_HOME: String = "APP_HOME"
private const val DASHED: String = "my-variable"
private const val VARIABLES_PATH: String = "variables"

private fun violationsOf(block: EnvironmentBuilder.() -> Unit): List<Pair<String, ViolationCode>> {
    val failure = shouldThrow<ValidationException> { environment(block = block) }
    return failure.report.violations.map { violation -> violation.path.toString() to violation.code }
}

class EnvironmentRulesTest : FunSpec({
    context("defaults") {
        test("give a value only when nothing else sets the variable") {
            val base = environment { variable(name = HOME, value = VALUE) }
            val composed = environment(base = base) {
                default(name = HOME, value = OTHER)
                defaults(mapOf(PATH to OTHER))
            }

            composed.variable(HOME) shouldBe VALUE
            composed.variable(PATH) shouldBe OTHER
            composed.names shouldContainExactlyInAnyOrder setOf(HOME, PATH)
        }

        test("are hidden by a removal of the same variable, which is reported") {
            violationsOf {
                default(name = HOME, value = VALUE)
                remove(HOME)
            } shouldBe listOf("""removals["HOME"]""" to EnvironmentViolationCodes.CONFLICTING_CHANGE)
        }

        test("must not be declared twice") {
            violationsOf {
                default(name = HOME, value = VALUE)
                default(name = HOME, value = OTHER)
            } shouldBe listOf("""defaults["HOME"]""" to EnvironmentViolationCodes.DUPLICATE_CHANGE)
        }
    }

    context("bulk declarations") {
        test("set every variable of a map, as overrides and as a layer") {
            val composed = environment {
                variables(mapOf(HOME to VALUE))
                layer(mapOf(PATH to OTHER))
            }

            composed.variable(HOME) shouldBe VALUE
            composed.variable(PATH) shouldBe OTHER
        }

        test("remove several variables at once") {
            val base = environment {
                variable(name = HOME, value = VALUE)
                variable(name = PATH, value = VALUE)
            }

            environment(base = base) { remove(HOME, PATH) }.names shouldBe emptySet()
        }

        test("report a broken map layer under its index") {
            violationsOf { layer(mapOf("A=B" to VALUE)) } shouldContain
                ("layers[0].variables" to EnvironmentViolationCodes.INVALID_NAME)
        }
    }

    context("alias declarations") {
        val legacy = environment { variable(name = LEGACY_HOME, value = VALUE) }

        test("read a fallback when the variable itself is not set") {
            val composed = environment(base = legacy) { alias(HOME, LEGACY_HOME) }

            composed.variable(HOME) shouldBe VALUE
            composed.names shouldContainExactlyInAnyOrder setOf(HOME, LEGACY_HOME)
        }

        test("prefer the variable itself and the earlier fallback") {
            val composed = environment(base = legacy) {
                variable(name = PATH, value = OTHER)
                alias(HOME, PATH, LEGACY_HOME)
            }

            composed.variable(HOME) shouldBe OTHER
        }

        test("never follow a fallback through another alias") {
            val composed = environment {
                alias(HOME, PATH)
                alias(PATH, HOME)
            }

            composed.variable(HOME) shouldBe null
            composed.names shouldBe emptySet()
        }

        test("refuse an alias without fallbacks, with too many or with itself") {
            violationsOf {
                alias(HOME)
                alias(
                    PATH,
                    *arrayOf(
                        "F0", "F1", "F2", "F3", "F4", "F5", "F6", "F7", "F8",
                        "F9", "F10", "F11", "F12", "F13", "F14", "F15", "F16",
                    ),
                )
                alias(LEGACY_HOME, LEGACY_HOME)
            } shouldContainExactlyInAnyOrder listOf(
                """aliases["HOME"]""" to EnvironmentViolationCodes.INVALID_ALIAS,
                """aliases["PATH"]""" to EnvironmentViolationCodes.INVALID_ALIAS,
                """aliases["OLD_HOME"]""" to EnvironmentViolationCodes.INVALID_ALIAS,
            )
        }

        test("refuse an alias declared twice and a broken fallback") {
            violationsOf {
                alias(HOME, PATH)
                alias(HOME, LEGACY_HOME)
                alias(PATH, "")
            } shouldContainExactlyInAnyOrder listOf(
                """aliases["HOME"]""" to EnvironmentViolationCodes.DUPLICATE_CHANGE,
                "aliases" to EnvironmentViolationCodes.INVALID_NAME,
            )
        }
    }

    context("filters") {
        val base = environment {
            variable(name = HOME, value = VALUE)
            variable(name = PATH, value = VALUE)
            variable(name = APP_HOME, value = VALUE)
        }

        test("show only the included variables") {
            val composed = environment(base = base) {
                filter {
                    include(HOME)
                    includePrefix(PREFIX)
                }
            }

            composed.names shouldContainExactlyInAnyOrder setOf(HOME, APP_HOME)
            composed.variable(PATH) shouldBe null
        }

        test("hide the excluded variables whatever includes them") {
            val composed = environment(base = base) {
                filter {
                    includePrefix(PREFIX)
                    exclude(APP_HOME)
                    excludePrefix("PA")
                }
            }

            composed.names shouldBe emptySet()
        }

        test("refuse a broken name") {
            violationsOf { filter { exclude("") } } shouldBe listOf("filters" to EnvironmentViolationCodes.INVALID_NAME)
        }
    }

    context("required variables") {
        test("are reported when they are not set") {
            violationsOf {
                variable(name = HOME, value = VALUE)
                require(HOME, PATH)
            } shouldBe listOf("""required["PATH"]""" to EnvironmentViolationCodes.MISSING_REQUIRED)
        }

        test("must not be declared twice") {
            violationsOf {
                variable(name = HOME, value = VALUE)
                require(HOME, HOME)
            } shouldBe listOf("""required["HOME"]""" to EnvironmentViolationCodes.DUPLICATE_CHANGE)
        }
    }

    context("prefixed declarations") {
        test("put the prefix in front of every declared name, nested blocks adding up") {
            val composed = environment {
                prefixed(PREFIX) {
                    variable(name = HOME, value = VALUE)
                    prefixed("DB_") { default(name = "URL", value = OTHER) }
                    filter { exclude(PATH) }
                }
                variable(name = PATH, value = VALUE)
            }

            composed.names shouldContainExactlyInAnyOrder setOf(APP_HOME, "APP_DB_URL", PATH)
        }

        test("combine with a scope into short names") {
            val composed = environment {
                prefixed(PREFIX) { variable(name = HOME, value = VALUE) }
                scope(PREFIX)
            }

            composed.variable(HOME) shouldBe VALUE
        }
    }

    context("case") {
        val windows = environment { variable(name = "Path", value = VALUE) }

        test("is compared exactly by default") {
            environment(base = windows) {}.variable(PATH) shouldBe null
        }

        test("is ignored after ignoreCase, keeping the spelling of the source") {
            val composed = environment(base = windows) {
                ignoreCase()
                ignoreCase()
                variable(name = "home", value = OTHER)
            }

            composed.variable(PATH) shouldBe VALUE
            composed.variable("path") shouldBe VALUE
            composed.variable(HOME) shouldBe OTHER
            composed.names shouldContainExactlyInAnyOrder setOf("Path", "home")
        }

        test("finds two spellings of one variable to be duplicates") {
            violationsOf {
                ignoreCase()
                variable(name = HOME, value = VALUE)
                variable(name = "home", value = VALUE)
            } shouldBe listOf("""variables["HOME"]""" to EnvironmentViolationCodes.DUPLICATE_CHANGE)
        }

        test("hides every spelling of a removed variable") {
            val composed = environment(base = windows) {
                ignoreCase()
                remove(PATH)
            }

            composed.variable("Path") shouldBe null
        }
    }

    context("snapshots") {
        test("read a live base once, when the environment is built") {
            val live = ChangingEnvironment()
            live.value = VALUE
            val frozen = environment(base = live) { snapshot() }
            val following = environment(base = live) {}

            live.value = OTHER

            frozen.variable(HOME) shouldBe VALUE
            following.variable(HOME) shouldBe OTHER
            frozen.names shouldBe setOf(HOME)
        }
    }

    context("grammar") {
        test("accepts every name by default") {
            environment { variable(name = DASHED, value = VALUE) }.variable(DASHED) shouldBe VALUE
        }

        test("asks for portable names when it is POSIX") {
            violationsOf {
                grammar = NameGrammar.Posix
                variable(name = DASHED, value = VALUE)
                variable(name = "1ST", value = VALUE)
                variable(name = "_OK_1", value = VALUE)
            } shouldBe listOf(
                VARIABLES_PATH to EnvironmentViolationCodes.INVALID_NAME,
                VARIABLES_PATH to EnvironmentViolationCodes.INVALID_NAME,
            )
        }
    }

    context("limits") {
        test("report too many rules once and keep none beyond them") {
            violationsOf {
                repeat(EnvironmentLimits.MAX_RULES + 1) { index -> require("V$index") }
                filter { include(HOME) }
            }.filter { (_, code) -> code == EnvironmentViolationCodes.TOO_MANY_RULES } shouldBe
                listOf("aliases" to EnvironmentViolationCodes.TOO_MANY_RULES)
        }

        test("count defaults among the changes") {
            violationsOf {
                repeat(EnvironmentLimits.MAX_CHANGES + 1) { index -> default(name = "V$index", value = VALUE) }
            } shouldBe listOf(VARIABLES_PATH to EnvironmentViolationCodes.TOO_MANY_CHANGES)
        }
    }

    context("an evaluated builder") {
        test("refuses every further declaration") {
            val builder = EnvironmentBuilder(Environment.EMPTY)
            builder.build()

            listOf<EnvironmentBuilder.() -> Unit>(
                { default(name = HOME, value = VALUE) },
                { alias(HOME, PATH) },
                { filter {} },
                { require(HOME) },
                { prefixed(PREFIX) {} },
                { ignoreCase() },
                { expand() },
                { snapshot() },
                { grammar = NameGrammar.Posix },
                { dotenv("") },
            ).forEach { declaration -> shouldThrow<IllegalStateException> { builder.declaration() } }
        }
    }
})
