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
import com.davils.kore.core.processVariable
import com.davils.kore.core.processVariableNames
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe

private const val NEVER_SET: String = "KNOT_VARIABLE_THAT_IS_NEVER_SET"

class EnvironmentTest : FunSpec({
    context("the empty environment") {
        test("holds no variable") {
            Environment.EMPTY.variable("HOME") shouldBe null
            Environment.EMPTY.names.shouldBeEmpty()
        }

        test("names itself") {
            Environment.EMPTY.toString() shouldBe "Environment.EMPTY"
        }
    }

    context("the system environment") {
        test("reads every variable the process reports") {
            Environment.SYSTEM.names.forEach { name ->
                Environment.SYSTEM.variable(name) shouldBe processVariable(name)
            }
        }

        test("returns null for a variable that is not set") {
            Environment.SYSTEM.variable(NEVER_SET) shouldBe null
        }

        test("returns null for an inherited property of the platform") {
            Environment.SYSTEM.variable("constructor") shouldBe null
            Environment.SYSTEM.variable("__proto__") shouldBe null
        }

        test("returns null for a name no platform allows, instead of failing") {
            Environment.SYSTEM.variable("") shouldBe null
            Environment.SYSTEM.variable("SECRET\u0000NAME") shouldBe null
            Environment.SYSTEM.variable("N".repeat(Limits.MAX_NAME_LENGTH + 1)) shouldBe null
        }

        test("lists the names the process reports") {
            Environment.SYSTEM.names shouldBe processVariableNames().toSet()
        }

        test("names itself and no variable") {
            Environment.SYSTEM.toString() shouldBe "Environment.SYSTEM"
        }
    }
})
