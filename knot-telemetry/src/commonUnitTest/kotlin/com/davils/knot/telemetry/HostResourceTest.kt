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

package com.davils.knot.telemetry

import com.davils.knot.environment.Environment
import com.davils.knot.environment.environment
import com.davils.knot.platform.Host
import com.davils.knot.platform.hostProbe
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.maps.shouldContain
import io.kotest.matchers.maps.shouldNotContainKey
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotContain

private const val SHOP: String = "shop"
private const val SERVICE_NAME: String = "service.name"
private const val SECRET: String = "s3cr3t"
private const val SERVICE_VARIABLE: String = "OTEL_SERVICE_NAME"
private const val ATTRIBUTES_VARIABLE: String = "OTEL_RESOURCE_ATTRIBUTES"
private const val FROM_ATTRIBUTES: String = "from-attributes"
private const val OS_TYPE: String = "os.type"
private const val HOST_ARCH: String = "host.arch"
private const val DARWIN: String = "darwin"
private const val LINUX: String = "linux"
private const val X86: String = "x86"

private val linuxJvm: Host = Host.detect(
    hostProbe {
        isJvm = true
        osName = "Linux"
        osArch = "amd64"
        javaVersion = "25.0.1"
    },
)

private fun Environment.resourceOf(
    serviceName: String? = SHOP,
    attributes: Map<String, String> = emptyMap(),
): Map<String, String> = hostResource(
    serviceName = serviceName,
    attributes = attributes,
    environment = this,
    host = linuxJvm,
).attributes.toMap()

class HostResourceTest : FunSpec({
    context("the facts of the host") {
        test("follow the semantic conventions of OpenTelemetry") {
            val attributes = Environment.EMPTY.resourceOf()

            attributes shouldContain (OS_TYPE to LINUX)
            attributes shouldContain (HOST_ARCH to "amd64")
            attributes shouldContain ("process.runtime.name" to "jvm")
            attributes shouldContain (SERVICE_NAME to SHOP)
        }

        test("name the process and the machine where the runtime reports them") {
            val host = Host.detect(
                hostProbe {
                    isJvm = true
                    processId = 4_242L
                    hostNameVariable = "build-01"
                },
            )
            val withProcess = hostResource(serviceName = SHOP, environment = Environment.EMPTY, host = host)
            val attributes = withProcess.attributes.toMap()

            attributes shouldContain ("process.pid" to "4242")
            attributes shouldContain ("host.name" to "build-01")
            Environment.EMPTY.resourceOf() shouldNotContainKey "host.name"
        }

        test("name every system and architecture by its value in the semantic conventions") {
            val cases = listOf(
                Triple(first = "Mac OS X", second = "aarch64", third = DARWIN to "arm64"),
                Triple(first = "Windows 11", second = X86, third = "windows" to X86),
                Triple(first = "FreeBSD", second = "arm", third = "freebsd" to "arm32"),
            )

            cases.forEach { (osName, osArch, expected) ->
                val host = Host.detect(
                    hostProbe {
                        isJvm = true
                        this.osName = osName
                        this.osArch = osArch
                    },
                )
                val attributes = hostResource(serviceName = SHOP, environment = Environment.EMPTY, host = host)
                val shown = attributes.attributes.toMap()

                shown shouldContain (OS_TYPE to expected.first)
                shown shouldContain (HOST_ARCH to expected.second)
            }
        }

        test("count mobile systems to the system family they run on") {
            val families = mapOf("Android" to LINUX, "ChromeOS" to LINUX, "iOS" to DARWIN)

            families.forEach { (platform, family) ->
                val host = Host.detect(
                    hostProbe {
                        hasWindow = true
                        clientHintsPlatform = platform
                    },
                )
                val attributes = hostResource(serviceName = SHOP, environment = Environment.EMPTY, host = host)

                attributes.attributes.toMap() shouldContain (OS_TYPE to family)
            }
        }

        test("describe the running process unless told otherwise") {
            val named = environment { variable(name = SERVICE_VARIABLE, value = SHOP) }
            val attributes = hostResource(environment = named).attributes.toMap()

            attributes shouldContain (SERVICE_NAME to SHOP)
            attributes.keys.contains("process.runtime.name") shouldBe true
        }

        test("leave out what Knot cannot tell") {
            val unknown = Host.detect(hostProbe { isJvm = true })
            val attributes = hostResource(serviceName = SHOP, environment = Environment.EMPTY, host = unknown)

            attributes.attributes.toMap() shouldNotContainKey OS_TYPE
            attributes.attributes.toMap() shouldNotContainKey HOST_ARCH
        }
    }

    context("the service name") {
        test("comes from the caller first, then OTEL_SERVICE_NAME, then OTEL_RESOURCE_ATTRIBUTES") {
            val both = environment {
                variable(name = SERVICE_VARIABLE, value = "from-variable")
                variable(name = ATTRIBUTES_VARIABLE, value = "$SERVICE_NAME=$FROM_ATTRIBUTES")
            }
            val attributesOnly = environment {
                variable(name = ATTRIBUTES_VARIABLE, value = "$SERVICE_NAME=$FROM_ATTRIBUTES")
            }

            both.resourceOf() shouldContain (SERVICE_NAME to SHOP)
            both.resourceOf(serviceName = null) shouldContain (SERVICE_NAME to "from-variable")
            attributesOnly.resourceOf(serviceName = null) shouldContain (SERVICE_NAME to FROM_ATTRIBUTES)
        }

        test("is required") {
            shouldThrow<IllegalArgumentException> { Environment.EMPTY.resourceOf(serviceName = null) }
        }
    }

    context("the attributes of the operator and the caller") {
        test("replace the facts of the host, and the caller replaces the operator") {
            val operator = environment {
                variable(
                    name = ATTRIBUTES_VARIABLE,
                    value = "$HOST_ARCH=custom,k8s.pod.name=pod-1,deployment.environment.name=prod",
                )
            }

            val attributes = operator.resourceOf(attributes = mapOf("k8s.pod.name" to "pod-2"))

            attributes shouldContain (HOST_ARCH to "custom")
            attributes shouldContain ("k8s.pod.name" to "pod-2")
            attributes shouldContain ("deployment.environment.name" to "prod")
        }

        test("never replace the identity kollect gives a resource") {
            val operator = environment {
                variable(name = ATTRIBUTES_VARIABLE, value = "service.instance.id=forged,telemetry.sdk.name=forged")
            }

            val attributes = operator.resourceOf()

            attributes.values shouldContainAll listOf("kollect", SHOP)
            attributes.values.contains("forged") shouldBe false
        }

        test("refuse a malformed list without repeating its values") {
            val operator = environment { variable(name = ATTRIBUTES_VARIABLE, value = "broken$SECRET") }

            val failure = shouldThrow<IllegalArgumentException> { operator.resourceOf() }
            failure.message.orEmpty() shouldNotContain SECRET
        }

        test("are bounded by the limits of kollect") {
            val many = (1..40).associate { index -> "custom.key$index" to "value" }

            shouldThrow<IllegalArgumentException> { Environment.EMPTY.resourceOf(attributes = many) }
        }
    }
})
