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
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

private const val HOME: String = "HOME"
private const val VALUE: String = "value"
private const val GHOST: String = LISTED_NAME

class EnvironmentEdgeTest : FunSpec({
    context("name grammar") {
        test("accepts portable names and refuses everything else") {
            NameGrammar.Posix.accepts("_a1") shouldBe true
            NameGrammar.Posix.accepts("Az_09") shouldBe true
            NameGrammar.Posix.accepts("") shouldBe false
            NameGrammar.Posix.accepts("9A") shouldBe false
            NameGrammar.Posix.accepts("A-B") shouldBe false
            NameGrammar.Posix.accepts("Ä") shouldBe false
            NameGrammar.Lenient.accepts("") shouldBe true
        }
    }

    context("folding") {
        test("finds a variable in another spelling and nothing when there is none") {
            val source = environment { variable(name = "Path", value = VALUE) }

            source.foldedVariable("PATH") shouldBe VALUE
            source.foldedVariable("Path") shouldBe VALUE
            source.foldedVariable(HOME) shouldBe null
            foldedName("a-Z_z9") shouldBe "A-Z_Z9"
        }
    }

    context("sources that list names they do not hold") {
        test("leave them out of a snapshot and of the names") {
            val composed = environment(base = ListingEnvironment()) { snapshot() }

            composed.names shouldBe emptySet()
            environment(base = ListingEnvironment()) {}.names shouldBe emptySet()
        }
    }

    context("aliases and filters together") {
        test("never read a fallback that a filter hides") {
            val composed = environment {
                variable(name = GHOST, value = VALUE)
                alias(HOME, GHOST)
                filter { exclude(GHOST) }
            }

            composed.variable(HOME) shouldBe null
        }
    }

    context("expansion") {
        test("keeps a dollar at the end and before other characters") {
            val composed = environment {
                variable(name = HOME, value = "a$")
                variable(name = GHOST, value = "\$b")
                expand()
            }

            composed.variable(HOME) shouldBe "a$"
            composed.variable(GHOST) shouldBe "\$b"
        }
    }

    context("limits of the builder") {
        test("refuse a dotenv layer and a filter beyond their limits") {
            val failure = shouldThrow<ValidationException> {
                environment {
                    repeat(EnvironmentLimits.MAX_LAYERS) { layer(Environment.EMPTY) }
                    dotenv("A=1")
                    repeat(EnvironmentLimits.MAX_RULES) { index -> require("V$index") }
                    filter { include(HOME) }
                }
            }
            val codes = failure.report.violations.map { violation -> violation.code }

            codes.contains(EnvironmentViolationCodes.TOO_MANY_LAYERS) shouldBe true
            codes.contains(EnvironmentViolationCodes.TOO_MANY_RULES) shouldBe true
        }
    }
})
