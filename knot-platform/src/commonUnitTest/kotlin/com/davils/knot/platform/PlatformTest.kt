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

import com.davils.kore.testing.HostileFragments
import com.davils.kore.testing.isSafeToShow
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeIn
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.kotest.property.Arb
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll

private const val LINUX: String = "Linux"
private const val WINDOWS: String = "Windows 11"
private const val MAC: String = "Mac OS X"
private const val AMD64: String = "amd64"
private const val ARM64: String = "aarch64"
private const val LINUX_X64: String = "linux-x86_64"
private const val WINDOWS_X64: String = "windows-x86_64"
private const val MAC_ARM64: String = "macos-aarch64"
private const val FREEBSD: String = "FreeBSD"
private const val PLATFORM_COUNT: Int = 6

class PlatformTest : FunSpec({
    context("resolving") {
        test("pairs an operating system with an architecture") {
            val platform = Platform.resolve(osName = LINUX, osArch = AMD64)

            platform?.operatingSystem shouldBe OperatingSystem.Linux
            platform?.architecture shouldBe Architecture.X64
        }

        test("leaves no platform when either half is unknown") {
            Platform.resolve(osName = FREEBSD, osArch = AMD64) shouldBe null
            Platform.resolve(osName = LINUX, osArch = "i386") shouldBe null
        }

        test("treats two spellings of one machine as one value") {
            val left = Platform.resolve(osName = MAC, osArch = "arm64")
            val right = Platform.resolve(osName = "Darwin", osArch = ARM64)

            left shouldBe right
            left.hashCode() shouldBe right.hashCode()
        }

        test("reads what Node.js reports") {
            Platform.resolve(osName = "win32", osArch = "x64") shouldBe Platform.parse(WINDOWS_X64)
            Platform.resolve(osName = "darwin", osArch = "arm64") shouldBe Platform.parse(MAC_ARM64)
        }

        test("is never wasm, which has no native build") {
            Platform.resolve(osName = "wasm", osArch = AMD64) shouldBe null
            Platform.parseOrNull("wasm-x86_64") shouldBe null
            Platform.entries.none { platform -> platform.operatingSystem.isWasm } shouldBe true
        }

        test("tells different machines apart and is never equal to something else") {
            Platform.parse(LINUX_X64) shouldNotBe Platform.parse("linux-aarch64")
            Platform.parse(LINUX_X64) shouldNotBe Platform.parse(WINDOWS_X64)
            Platform.parse(LINUX_X64).equals(LINUX_X64) shouldBe false
        }

        test("gives nothing or a supported platform for any pair of names") {
            checkAll(Arb.string(), Arb.string()) { osName, osArch ->
                val known: List<Platform?> = Platform.entries + null

                Platform.resolve(osName = osName, osArch = osArch) shouldBeIn known
            }
        }
    }

    context("identifier") {
        test("names the directory of native binaries the way Kreate packages them") {
            Platform.resolve(osName = WINDOWS, osArch = AMD64)?.identifier shouldBe WINDOWS_X64
            Platform.parse(MAC_ARM64).toString() shouldBe MAC_ARM64
        }

        test("reads back as the same platform") {
            Platform.entries.forEach { platform -> Platform.parse(platform.identifier) shouldBe platform }
        }

        test("is refused when it names no supported platform") {
            Platform.parseOrNull("linux-x86") shouldBe null
            shouldThrow<IllegalArgumentException> { Platform.parse("freebsd-x86_64") }.message shouldBe
                "Not a supported platform, which is one of linux-aarch64, linux-x86_64, macos-aarch64, " +
                "macos-x86_64, windows-aarch64, windows-x86_64"
        }

        test("is never repeated in the message of a refusal") {
            val hostile = "linux${HostileFragments.ESCAPE}[2J"
            val failure = shouldThrow<IllegalArgumentException> { Platform.parse(hostile) }

            failure.message.orEmpty() shouldNotContain HostileFragments.ESCAPE
        }
    }

    context("predicates") {
        test("name the operating system without comparing enums") {
            val windows = Platform.parse("windows-aarch64")
            val linux = Platform.parse(LINUX_X64)
            val mac = Platform.parse("macos-x86_64")

            listOf(windows.isWindows, windows.isLinux, windows.isMacOs, windows.isUnix) shouldBe
                listOf(true, false, false, false)
            listOf(linux.isLinux, linux.isUnix, linux.isWindows, mac.isMacOs, mac.isUnix) shouldBe
                listOf(true, true, false, true, true)
        }
    }

    context("entries") {
        test("pair every operating system with every architecture") {
            Platform.entries shouldHaveSize PLATFORM_COUNT
        }

        test("have the identifiers of the set, none of them twice") {
            Platform.identifiers shouldContainExactlyInAnyOrder Platform.entries.map { platform -> platform.identifier }
            Platform.identifiers shouldHaveSize PLATFORM_COUNT
        }
    }

    context("current machine") {
        test("resolves to what the runtime reports about it") {
            val reported = Platform.resolve(osName = hostOsName(), osArch = hostOsArch())

            Platform.current shouldBe reported
        }

        test("is read once and held") {
            Platform.current shouldBeSameInstanceAs Platform.current
        }

        test("is required to be supported") {
            Platform.current?.let { current -> Platform.requireCurrent() shouldBe current }
        }
    }

    context("requiring a supported machine") {
        test("gives the platform of supported names") {
            Platform.requireSupported(osName = LINUX, osArch = AMD64) shouldBe Platform.parse(LINUX_X64)
        }

        test("explains which machine is not supported") {
            val failure = shouldThrow<IllegalStateException> {
                Platform.requireSupported(osName = FREEBSD, osArch = "riscv64")
            }

            failure.message.orEmpty() shouldContain
                """The system "FreeBSD" with the architecture "riscv64" is not supported, only linux-aarch64"""
        }

        test("shows hostile names escaped") {
            val names = listOf(
                HostileFragments.ESCAPE,
                HostileFragments.RIGHT_TO_LEFT_OVERRIDE,
                HostileFragments.LINE_SEPARATOR,
                HostileFragments.LONE_HIGH_SURROGATE,
                "line\nbreak",
            )

            names.forEach { name ->
                val failure = shouldThrow<IllegalStateException> {
                    Platform.requireSupported(osName = name, osArch = name)
                }

                isSafeToShow(failure.message.orEmpty()) shouldBe true
            }
        }
    }
})
