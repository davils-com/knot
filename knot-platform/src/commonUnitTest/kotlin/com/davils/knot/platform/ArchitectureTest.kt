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
import io.kotest.property.Arb
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll

class ArchitectureTest : FunSpec({
    context("recognises every spelling a JVM, Node.js or toolchain reports") {
        mapOf(
            "amd64" to Architecture.X64,
            "x86_64" to Architecture.X64,
            "x86-64" to Architecture.X64,
            "x64" to Architecture.X64,
            "em64t" to Architecture.X64,
            "aarch64" to Architecture.Arm64,
            "aarch_64" to Architecture.Arm64,
            "arm64" to Architecture.Arm64,
            " AMD64 " to Architecture.X64,
        ).forEach { (reported, expected) ->
            test("'$reported' is ${expected.identifier}") {
                Architecture.resolve(reported) shouldBe expected
            }
        }
    }

    context("never resolves a 32-bit machine to a 64-bit build") {
        listOf("x86", "i386", "i686", "arm", "armv7l").forEach { reported ->
            test("'$reported' does not resolve") {
                Architecture.resolve(reported) shouldBe null
            }
        }
    }

    context("leaves architectures without a build unresolved") {
        listOf("ppc64le", "s390x", "riscv64", "wasm", "").forEach { reported ->
            test("'$reported' does not resolve") {
                Architecture.resolve(reported) shouldBe null
            }
        }
    }

    context("refuses names it must not compare") {
        test("a name longer than the limit does not resolve") {
            val padded = "amd64".padEnd(Limits.MAX_NAME_LENGTH + 1)

            Architecture.resolve(padded) shouldBe null
        }

        test("a name with a character outside printable ASCII does not resolve") {
            Architecture.resolve("amd64\n") shouldBe null
            Architecture.resolve("аmd64") shouldBe null
        }

        test("any text resolves to nothing or to a known architecture") {
            checkAll(Arb.string(maxSize = Limits.MAX_NAME_LENGTH + 2)) { reported ->
                val known: List<Architecture?> = Architecture.entries + null

                Architecture.resolve(reported) shouldBeIn known
            }
        }
    }
})
