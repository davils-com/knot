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

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

private const val PROCESS_ID: Long = 4_242L
private const val HOST: String = "build-01.example.com"
private const val COMPUTER: String = "WORKSTATION"
private const val NODE: String = "22.11.0"
private const val LONGEST_LABEL: Int = 63

private fun nodeHost(block: HostProbeBuilder.() -> Unit): Host = Host.detect(
    hostProbe {
        nodeVersion = NODE
        block()
    },
)

class HostProcessTest : FunSpec({
    context("process id") {
        test("is the id a runtime with a process reports") {
            nodeHost { processId = PROCESS_ID }.processId shouldBe PROCESS_ID
            val jvm = Host.detect(
                hostProbe {
                    isJvm = true
                    processId = PROCESS_ID
                },
            )

            jvm.processId shouldBe PROCESS_ID
        }

        test("is unknown when it is not positive or none is reported") {
            nodeHost { processId = 0L }.processId shouldBe null
            nodeHost { processId = -1L }.processId shouldBe null
            nodeHost {}.processId shouldBe null
        }

        test("is unknown in a browser even when one is declared") {
            val browser = Host.detect(
                hostProbe {
                    hasWindow = true
                    processId = PROCESS_ID
                },
            )

            browser.processId shouldBe null
        }
    }

    context("host name") {
        test("comes from HOSTNAME before COMPUTERNAME") {
            nodeHost {
                hostNameVariable = HOST
                computerNameVariable = COMPUTER
            }.hostName shouldBe HOST
            nodeHost { computerNameVariable = COMPUTER }.hostName shouldBe COMPUTER
        }

        test("is trimmed and falls back when HOSTNAME holds no host name") {
            nodeHost { hostNameVariable = " $HOST\n" }.hostName shouldBe HOST
            nodeHost {
                hostNameVariable = "not a host"
                computerNameVariable = COMPUTER
            }.hostName shouldBe COMPUTER
        }

        test("refuses what RFC 1123 does not allow") {
            listOf(
                "",
                "-leading",
                "trailing-",
                "two..dots",
                "under_score",
                "host\u0000name",
                "a".repeat(LONGEST_LABEL + 1),
                List(size = 64) { "abc" }.joinToString(separator = "."),
            ).forEach { name ->
                nodeHost { hostNameVariable = name }.hostName shouldBe null
            }
        }

        test("keeps the longest label RFC 1123 allows") {
            val label = "a".repeat(LONGEST_LABEL)

            nodeHost { hostNameVariable = label }.hostName shouldBe label
        }

        test("is unknown in a browser even when one is declared") {
            val browser = Host.detect(
                hostProbe {
                    hasWindow = true
                    hostNameVariable = HOST
                },
            )

            browser.hostName shouldBe null
        }
    }

    context("value") {
        test("tells hosts of other processes and machines apart, but never shows them") {
            val host = nodeHost {
                processId = PROCESS_ID
                hostNameVariable = HOST
            }

            host shouldBe nodeHost {
                processId = PROCESS_ID
                hostNameVariable = HOST
            }
            host shouldNotBe nodeHost { hostNameVariable = HOST }
            host shouldNotBe nodeHost { processId = PROCESS_ID }
            host.toString().contains(HOST) shouldBe false
            host.toString().contains(PROCESS_ID.toString()) shouldBe false
        }

        test("probes with other process ids differ and show the id") {
            val probe = hostProbe {
                processId = PROCESS_ID
                computerNameVariable = COMPUTER
            }

            probe shouldNotBe hostProbe { computerNameVariable = COMPUTER }
            probe.hashCode() shouldBe hostProbe {
                processId = PROCESS_ID
                computerNameVariable = COMPUTER
            }.hashCode()
            probe.toString() shouldBe "HostProbe(processId=4242, computerNameVariable=\"WORKSTATION\")"
        }
    }
})
