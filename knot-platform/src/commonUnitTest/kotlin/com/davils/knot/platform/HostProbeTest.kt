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

private const val LINUX: String = "linux"

class HostProbeTest : FunSpec({
    context("facts") {
        test("keeps every declared fact as it was reported") {
            val probe = hostProbe {
                isJvm = true
                osName = " Linux "
                osArch = "amd64"
                javaVersion = "25"
                processPlatform = LINUX
                processArch = "x64"
                nodeVersion = "22"
                denoOs = LINUX
                denoArch = "x86_64"
                denoVersion = "2"
                bunVersion = "1"
                hasWindow = true
                isDedicatedWorker = true
                isSharedWorker = true
                isServiceWorker = true
                userAgent = "agent"
                clientHintsPlatform = "Linux"
                isClientHintsMobile = false
                clientHintsArchitecture = "x86"
                clientHintsBitness = "64"
            }

            probe.osName shouldBe " Linux "
            probe.isServiceWorker shouldBe true
            probe.isClientHintsMobile shouldBe false
            probe.clientHintsBitness shouldBe "64"
        }

        test("leaves every fact that is not declared unreported") {
            val probe = hostProbe {}

            probe.isJvm shouldBe false
            probe.userAgent shouldBe null
            probe.isClientHintsMobile shouldBe null
        }
    }

    context("value") {
        test("is equal for equal facts and different otherwise") {
            val linux = hostProbe { processPlatform = LINUX }

            linux shouldBe hostProbe { processPlatform = LINUX }
            linux.hashCode() shouldBe hostProbe { processPlatform = LINUX }.hashCode()
            linux shouldNotBe hostProbe { processPlatform = "darwin" }
            linux shouldNotBe hostProbe {
                processPlatform = LINUX
                hasWindow = true
            }
            linux.equals(LINUX) shouldBe false
        }

        test("shows the flags that are set and the texts that are reported") {
            val probe = hostProbe {
                hasWindow = true
                isClientHintsMobile = false
                userAgent = "agent"
            }

            probe.toString() shouldBe "HostProbe(hasWindow=true, isClientHintsMobile=false, userAgent=\"agent\")"
            hostProbe {}.toString() shouldBe "HostProbe()"
        }

        test("shows hostile texts escaped") {
            HostileFragments.all.forEach { fragment ->
                val probe = hostProbe { userAgent = "agent$fragment" }

                isSafeToShow(probe.toString()) shouldBe true
            }
        }
    }
})
