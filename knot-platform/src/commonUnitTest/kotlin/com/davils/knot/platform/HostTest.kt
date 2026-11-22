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
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.kotest.property.Arb
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll

private const val CHROME_WINDOWS: String = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
    "(KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"
private const val FIREFOX_WINDOWS_32: String = "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:115.0) Gecko/20100101 " +
    "Firefox/115.0"
private const val SAFARI_MAC: String = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 " +
    "(KHTML, like Gecko) Version/18.1 Safari/605.1.15"
private const val FIREFOX_LINUX: String = "Mozilla/5.0 (X11; Linux x86_64; rv:133.0) Gecko/20100101 Firefox/133.0"
private const val FIREFOX_LINUX_ARM: String = "Mozilla/5.0 (X11; Linux aarch64; rv:133.0) Gecko/20100101 " +
    "Firefox/133.0"
private const val CHROME_ANDROID: String = "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 " +
    "(KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36"
private const val SAFARI_IPHONE: String = "Mozilla/5.0 (iPhone; CPU iPhone OS 18_1 like Mac OS X) " +
    "AppleWebKit/605.1.15 (KHTML, like Gecko) Version/18.1 Mobile/15E148 Safari/604.1"
private const val CHROME_OS: String = "Mozilla/5.0 (X11; CrOS x86_64 14541.0.0) AppleWebKit/537.36 " +
    "(KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"
private const val FIREFOX_FREEBSD: String = "Mozilla/5.0 (X11; FreeBSD amd64; rv:133.0) Gecko/20100101 " +
    "Firefox/133.0"
private const val SAFARI_IPAD: String = "Mozilla/5.0 (iPad; CPU OS 18_1 like Mac OS X) AppleWebKit/605.1.15 " +
    "(KHTML, like Gecko) Version/18.1 Mobile/15E148 Safari/604.1"
private const val NODE_VERSION: String = "22.11.0"
private const val LINUX: String = "linux"
private const val DENO_VERSION: String = "2.1.4"
private const val BUN_VERSION: String = "1.1.38"
private const val X64: String = "x64"

class HostTest : FunSpec({
    context("runtime") {
        test("is the JVM when the probe says so, before every other fact") {
            detect {
                isJvm = true
                nodeVersion = NODE_VERSION
                hasWindow = true
            }.runtime shouldBe HostRuntime.Jvm
        }

        test("is Deno or Bun even when they offer the process object of Node.js") {
            detect {
                denoVersion = DENO_VERSION
                nodeVersion = NODE_VERSION
            }.runtime shouldBe HostRuntime.Deno
            detect {
                denoOs = LINUX
            }.runtime shouldBe HostRuntime.Deno
            detect {
                bunVersion = BUN_VERSION
                nodeVersion = NODE_VERSION
            }.runtime shouldBe HostRuntime.Bun
        }

        test("is Node.js when it reports its version") {
            detect { nodeVersion = NODE_VERSION }.runtime shouldBe HostRuntime.NodeJs
        }

        test("tells the web workers apart by their global scope") {
            detect { isServiceWorker = true }.runtime shouldBe HostRuntime.ServiceWorker
            detect { isSharedWorker = true }.runtime shouldBe HostRuntime.SharedWorker
            detect { isDedicatedWorker = true }.runtime shouldBe HostRuntime.DedicatedWorker
        }

        test("is the browser for a page with a window and a document") {
            detect { hasWindow = true }.runtime shouldBe HostRuntime.Browser
        }

        test("is an unknown WebAssembly runtime when nothing is reported") {
            detect {}.runtime shouldBe HostRuntime.UnknownWasm
        }

        test("keeps the version only of the runtime that reports it") {
            detect {
                isJvm = true
                javaVersion = "25.0.1"
            }.runtimeVersion shouldBe "25.0.1"
            detect { nodeVersion = " $NODE_VERSION " }.runtimeVersion shouldBe NODE_VERSION
            detect { denoVersion = DENO_VERSION }.runtimeVersion shouldBe DENO_VERSION
            detect { bunVersion = BUN_VERSION }.runtimeVersion shouldBe BUN_VERSION
            detect {
                hasWindow = true
                nodeVersion = null
                javaVersion = "25"
            }.runtimeVersion shouldBe null
        }

        test("ignores a version that is too long, blank or not printable ASCII") {
            detect { nodeVersion = "1".repeat(PlatformLimits.MAX_VERSION_LENGTH + 1) }.runtimeVersion shouldBe null
            detect { nodeVersion = "1".repeat(PlatformLimits.MAX_VERSION_LENGTH) }.runtimeVersion shouldNotBe null
            detect { nodeVersion = "   " }.runtimeVersion shouldBe null
            detect { nodeVersion = "22${HostileFragments.ESCAPE}" }.runtimeVersion shouldBe null
        }
    }

    context("system") {
        test("comes from the system properties of the JVM") {
            val host = detect {
                isJvm = true
                osName = "Mac OS X"
            }

            host.system shouldBe HostSystem.MacOs
            host.systemSource shouldBe DetectionSource.SystemProperty
        }

        test("comes from what Node.js and Bun report as the platform of the process") {
            val reported = mapOf(
                LINUX to HostSystem.Linux,
                "darwin" to HostSystem.MacOs,
                "win32" to HostSystem.Windows,
                "freebsd" to HostSystem.FreeBsd,
                "android" to HostSystem.Android,
            )

            reported.forEach { (platform, system) ->
                val host = detect {
                    nodeVersion = NODE_VERSION
                    processPlatform = platform
                }

                host.system shouldBe system
                host.systemSource shouldBe DetectionSource.Process
            }
        }

        test("comes from the build of Deno before the process object") {
            val host = detect {
                denoOs = "windows"
                processPlatform = LINUX
            }

            host.system shouldBe HostSystem.Windows
            host.systemSource shouldBe DetectionSource.DenoBuild
        }

        test("comes from the client hints of a browser before its user agent") {
            val host = detect {
                hasWindow = true
                clientHintsPlatform = "Chrome OS"
                userAgent = CHROME_WINDOWS
            }

            host.system shouldBe HostSystem.ChromeOs
            host.systemSource shouldBe DetectionSource.ClientHints
        }

        test("reads every platform the client hints name") {
            val hinted = mapOf(
                "Windows" to HostSystem.Windows,
                "macOS" to HostSystem.MacOs,
                "Linux" to HostSystem.Linux,
                "Android" to HostSystem.Android,
                "iOS" to HostSystem.Ios,
                "ChromeOS" to HostSystem.ChromeOs,
                "FreeBSD" to HostSystem.FreeBsd,
            )

            hinted.forEach { (platform, system) ->
                detect { clientHintsPlatform = platform }.system shouldBe system
            }
        }

        test("falls back to the user agent when the client hints name nothing known") {
            val host = detect {
                clientHintsPlatform = "Unknown"
                userAgent = FIREFOX_LINUX
            }

            host.system shouldBe HostSystem.Linux
            host.systemSource shouldBe DetectionSource.UserAgent
        }

        test("reads the user agents of the common browsers") {
            val agents = mapOf(
                CHROME_WINDOWS to HostSystem.Windows,
                FIREFOX_WINDOWS_32 to HostSystem.Windows,
                SAFARI_MAC to HostSystem.MacOs,
                FIREFOX_LINUX to HostSystem.Linux,
                CHROME_ANDROID to HostSystem.Android,
                SAFARI_IPHONE to HostSystem.Ios,
                SAFARI_IPAD to HostSystem.Ios,
                CHROME_OS to HostSystem.ChromeOs,
                FIREFOX_FREEBSD to HostSystem.FreeBsd,
            )

            agents.forEach { (agent, system) -> detect { userAgent = agent }.system shouldBe system }
        }

        test("is unknown when no source names a known system") {
            val host = detect {
                nodeVersion = NODE_VERSION
                processPlatform = "sunos"
                userAgent = "Node.js/22"
            }

            host.system shouldBe HostSystem.Unknown
            host.systemSource shouldBe DetectionSource.None
        }

        test("ignores a user agent that is too long or not printable ASCII") {
            val tooLong = CHROME_WINDOWS + " ".repeat(PlatformLimits.MAX_USER_AGENT_LENGTH)

            detect { userAgent = tooLong }.system shouldBe HostSystem.Unknown
            detect { userAgent = "Windows${HostileFragments.RIGHT_TO_LEFT_OVERRIDE}" }.system shouldBe
                HostSystem.Unknown
        }
    }

    context("architecture") {
        test("comes from what the JVM, Deno and Node.js report") {
            detect {
                isJvm = true
                osArch = "amd64"
            }.architecture shouldBe HostArchitecture.X64
            detect { denoArch = "aarch64" }.architecture shouldBe HostArchitecture.Arm64
            detect { processArch = "ia32" }.architecture shouldBe HostArchitecture.X86
            detect { processArch = "arm" }.architecture shouldBe HostArchitecture.Arm32
        }

        test("names the source it comes from") {
            detect { processArch = X64 }.architectureSource shouldBe DetectionSource.Process
            detect { denoArch = "x86_64" }.architectureSource shouldBe DetectionSource.DenoBuild
        }

        test("comes from the high entropy client hints only with their bitness") {
            val hinted = mapOf(
                Pair(first = "x86", second = "64") to HostArchitecture.X64,
                Pair(first = "x86", second = "32") to HostArchitecture.X86,
                Pair(first = "arm", second = "64") to HostArchitecture.Arm64,
                Pair(first = "arm", second = "32") to HostArchitecture.Arm32,
            )

            hinted.forEach { (hints, architecture) ->
                val host = detect {
                    clientHintsArchitecture = hints.first
                    clientHintsBitness = hints.second
                }

                host.architecture shouldBe architecture
                host.architectureSource shouldBe DetectionSource.ClientHints
            }
            detect {
                clientHintsArchitecture = "x86"
                userAgent = FIREFOX_LINUX_ARM
            }.architectureSource shouldBe DetectionSource.UserAgent
            detect {
                clientHintsBitness = "64"
            }.architecture shouldBe HostArchitecture.Unknown
        }

        test("reads the user agents of the common browsers") {
            val agents = mapOf(
                CHROME_WINDOWS to HostArchitecture.X64,
                FIREFOX_WINDOWS_32 to HostArchitecture.X64,
                FIREFOX_LINUX to HostArchitecture.X64,
                FIREFOX_LINUX_ARM to HostArchitecture.Arm64,
                CHROME_OS to HostArchitecture.X64,
                FIREFOX_FREEBSD to HostArchitecture.X64,
                "Mozilla/5.0 (X11; Linux armv7l)" to HostArchitecture.Arm32,
                "Mozilla/5.0 (X11; Linux i686)" to HostArchitecture.X86,
            )

            agents.forEach { (agent, architecture) ->
                detect { userAgent = agent }.architecture shouldBe architecture
            }
        }

        test("never takes the Intel of a Mac user agent for the architecture") {
            detect { userAgent = SAFARI_MAC }.architecture shouldBe HostArchitecture.Unknown
        }

        test("is unknown when nothing names a known architecture") {
            val host = detect { processArch = "riscv64" }

            host.architecture shouldBe HostArchitecture.Unknown
            host.architectureSource shouldBe DetectionSource.None
        }
    }

    context("native platform") {
        test("is the platform of the machine behind Node.js") {
            detect {
                nodeVersion = NODE_VERSION
                processPlatform = LINUX
                processArch = X64
            }.platform shouldBe Platform.parse("linux-x86_64")
        }

        test("is the platform the JVM reports") {
            detect {
                isJvm = true
                osName = "Windows 11"
                osArch = "aarch64"
            }.platform shouldBe Platform.parse("windows-aarch64")
        }

        test("is never derived from a browser, whose facts are a hint") {
            val host = detect {
                hasWindow = true
                clientHintsPlatform = "Windows"
                clientHintsArchitecture = "x86"
                clientHintsBitness = "64"
            }

            host.isAuthoritative shouldBe false
            host.platform shouldBe null
        }

        test("is absent for a system or an architecture without a native build") {
            detect {
                processPlatform = "android"
                processArch = "arm64"
            }.platform shouldBe null
            detect {
                processPlatform = LINUX
                processArch = "ia32"
            }.platform shouldBe null
            detect {
                processPlatform = LINUX
                userAgent = FIREFOX_LINUX
            }.platform shouldBe null
        }

        test("is nothing or a supported platform for any reported facts") {
            checkAll(Arb.string(), Arb.string(), Arb.string()) { platform, architecture, agent ->
                val host = detect {
                    processPlatform = platform
                    processArch = architecture
                    userAgent = agent
                }
                val known: List<Platform?> = Platform.entries + null

                (host.platform in known) shouldBe true
            }
        }
    }

    context("mobile") {
        test("is false for a runtime with a process of its own") {
            detect {
                nodeVersion = NODE_VERSION
                userAgent = CHROME_ANDROID
            }.isMobile shouldBe false
        }

        test("follows the client hints before the user agent") {
            detect {
                isClientHintsMobile = false
                userAgent = CHROME_ANDROID
            }.isMobile shouldBe false
        }

        test("follows the mobile token of the user agent") {
            detect { userAgent = SAFARI_IPHONE }.isMobile shouldBe true
            detect { userAgent = CHROME_WINDOWS }.isMobile shouldBe false
        }

        test("is unknown when a browser tells nothing") {
            detect { hasWindow = true }.isMobile shouldBe null
        }
    }

    context("value") {
        test("is equal for equal facts and different otherwise") {
            val node = detect { processPlatform = LINUX }

            node shouldBe detect { processPlatform = LINUX }
            node.hashCode() shouldBe detect { processPlatform = LINUX }.hashCode()
            node shouldNotBe detect { processPlatform = "darwin" }
            node.equals(LINUX) shouldBe false
        }

        test("shows its facts and their sources") {
            val host = detect {
                nodeVersion = NODE_VERSION
                processPlatform = LINUX
                processArch = X64
            }

            host.toString() shouldBe "Host(runtime=nodejs, system=linux (process), architecture=x86_64 (process), " +
                "version=\"22.11.0\", mobile=false)"
        }

        test("shows a hostile version escaped") {
            HostileFragments.all.forEach { fragment ->
                val host = detect { nodeVersion = "v$fragment" }

                isSafeToShow(host.toString()) shouldBe true
            }
        }
    }

    context("current host") {
        test("is detected once and held") {
            Host.current shouldBeSameInstanceAs Host.current
        }

        test("has the same runtime when detected in detail") {
            Host.detailed().runtime shouldBe Host.current.runtime
        }

        test("has the native platform of the current platform") {
            Host.current.platform shouldBe Platform.current
        }
    }
})

private fun detect(block: HostProbeBuilder.() -> Unit): Host = Host.detect(hostProbe(block))
