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

package com.davils.knot.platform

import com.davils.kore.core.Limits
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeIn
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.kotest.property.Arb
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll

private const val LINUX: String = "Linux"

class OperatingSystemTest : FunSpec({
    context("recognises an operating system from the name the runtime reports") {
        mapOf(
            LINUX to OperatingSystem.Linux,
            "linux" to OperatingSystem.Linux,
            "Mac OS X" to OperatingSystem.MacOs,
            "macOS" to OperatingSystem.MacOs,
            "Darwin" to OperatingSystem.MacOs,
            "darwin" to OperatingSystem.MacOs,
            "Windows 11" to OperatingSystem.Windows,
            "Windows Server 2022" to OperatingSystem.Windows,
            "win32" to OperatingSystem.Windows,
            "  LINUX  " to OperatingSystem.Linux,
            "wasm" to OperatingSystem.Wasm,
            " WASM " to OperatingSystem.Wasm,
        ).forEach { (reported, expected) ->
            test("'$reported' is ${expected.identifier}") {
                OperatingSystem.resolve(reported) shouldBe expected
            }
        }
    }

    context("leaves every other system unresolved instead of guessing") {
        listOf("FreeBSD", "SunOS", "AIX", "Android", "web", "wasm32", "wasmtime", "", "   ").forEach { reported ->
            test("'$reported' does not resolve") {
                OperatingSystem.resolve(reported) shouldBe null
            }
        }
    }

    context("refuses names it must not compare") {
        test("a name longer than the limit does not resolve") {
            val padded = LINUX.padEnd(Limits.MAX_NAME_LENGTH + 1, 'x')

            OperatingSystem.resolve(padded) shouldBe null
        }

        test("a name of exactly the limit is still compared") {
            val padded = LINUX.padEnd(Limits.MAX_NAME_LENGTH, 'x')

            OperatingSystem.resolve(padded) shouldBe OperatingSystem.Linux
        }

        test("a name with a character outside printable ASCII does not resolve") {
            OperatingSystem.resolve("Linux‮") shouldBe null
        }

        test("any text resolves to nothing or to a known system") {
            checkAll(Arb.string(maxSize = Limits.MAX_NAME_LENGTH + 2)) { reported ->
                val known: List<OperatingSystem?> = OperatingSystem.entries + null

                OperatingSystem.resolve(reported) shouldBeIn known
            }
        }
    }

    context("unix") {
        test("is true for Linux and macOS and false for Windows and wasm") {
            OperatingSystem.entries.associateWith { system -> system.isUnix } shouldBe mapOf(
                OperatingSystem.Linux to true,
                OperatingSystem.MacOs to true,
                OperatingSystem.Windows to false,
                OperatingSystem.Wasm to false,
            )
        }
    }

    context("wasm") {
        test("is true for wasm only") {
            OperatingSystem.entries.filter { system -> system.isWasm } shouldBe listOf(OperatingSystem.Wasm)
        }
    }

    context("current system") {
        test("resolves to what the runtime reports about it") {
            OperatingSystem.current shouldBe OperatingSystem.resolve(hostOsName())
        }

        test("is read once and held") {
            OperatingSystem.current shouldBeSameInstanceAs OperatingSystem.current
        }
    }
})
