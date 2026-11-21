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

import com.davils.kore.testing.HostileFragments
import com.davils.kore.testing.isSafeToShow
import io.kotest.assertions.throwables.shouldThrowAny
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldNotContain
import io.kotest.property.Arb
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll

private const val SECRET: String = "s3cr3t-value"
private const val HOME: String = "HOME"
private const val ALICE_HOME: String = "/home/alice"

class LayeredEnvironmentTest : FunSpec({
    context("toString") {
        test("shows the names in order and masks every value") {
            val composed = environment {
                variable(name = "TOKEN", value = SECRET)
                variable(name = HOME, value = ALICE_HOME)
            }

            composed.toString() shouldBe "{HOME=****, TOKEN=****}"
        }

        test("never shows a value") {
            checkAll(Arb.string(minSize = 1, maxSize = 64)) { value ->
                val marked = "<${value.filterNot { character -> character == '\u0000' }}>"
                val composed = environment { variable(name = "VALUE", value = marked) }

                composed.toString() shouldNotContain marked
            }
        }

        test("escapes hostile names") {
            val hostile = listOf(
                HostileFragments.ESCAPE,
                HostileFragments.RIGHT_TO_LEFT_OVERRIDE,
                HostileFragments.LINE_SEPARATOR,
                "line\nbreak",
            )
            val composed = environment {
                hostile.forEach { name -> variable(name = "A$name", value = "x") }
            }

            isSafeToShow(composed.toString()) shouldBe true
        }
    }

    context("names") {
        test("are a snapshot that no caller can change") {
            val composed = environment { variable(name = HOME, value = ALICE_HOME) }
            val names: Set<String> = composed.names

            shouldThrowAny { (names as MutableSet<String>).clear() }
        }
    }

    context("identity") {
        test("is equal only to itself") {
            val left = environment { variable(name = HOME, value = ALICE_HOME) }
            val right = environment { variable(name = HOME, value = ALICE_HOME) }

            left shouldBe left
            left shouldNotBe right
        }
    }
})
