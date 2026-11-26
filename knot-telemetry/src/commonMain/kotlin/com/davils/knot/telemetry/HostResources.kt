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
import com.davils.knot.platform.Host
import com.davils.knot.platform.HostArchitecture
import com.davils.knot.platform.HostSystem
import com.davils.knot.telemetry.internal.resourceAttributesOf
import com.davils.kollect.core.otlp.Resource
import com.davils.kollect.core.otlp.resource

private const val SERVICE_NAME_VARIABLE: String = "OTEL_SERVICE_NAME"
private const val ATTRIBUTES_VARIABLE: String = "OTEL_RESOURCE_ATTRIBUTES"
private const val SERVICE_NAME: String = "service.name"
private const val OS_TYPE: String = "os.type"
private const val HOST_ARCH: String = "host.arch"
private const val RUNTIME_NAME: String = "process.runtime.name"
private const val RUNTIME_VERSION: String = "process.runtime.version"
private const val MISSING_SERVICE: String =
    "A resource needs a service name: pass one, or set OTEL_SERVICE_NAME or service.name in OTEL_RESOURCE_ATTRIBUTES"

private val keysOfKollect: Set<String> =
    setOf(SERVICE_NAME, "service.instance.id", "telemetry.sdk.name", "telemetry.sdk.language")
private val keysAllowedByDefault: Set<String> =
    setOf("service.version", "service.namespace", "deployment.environment.name")

/**
 * Creates the OTLP resource of this process for kollect, with the attributes of the OpenTelemetry semantic conventions
 * that describe the host Knot detects and those the operator sets through the environment.
 *
 * The attributes come from three sources; a later one replaces a key of an earlier one:
 * 1. the [host]: `os.type` (`linux`, `darwin`, `windows` or `freebsd`), `host.arch` (`amd64`, `arm64`, `x86` or
 *    `arm32`), `process.runtime.name` and, where the runtime reports it, `process.runtime.version`; what Knot cannot
 *    tell is left out, never guessed;
 * 2. `OTEL_RESOURCE_ATTRIBUTES` of [environment], a list of `key=value` pairs separated by commas whose values are
 *    percent-encoded UTF-8, as the OpenTelemetry specification defines it;
 * 3. [attributes] of the caller.
 *
 * The service name is [serviceName], else `OTEL_SERVICE_NAME`, else `service.name` of `OTEL_RESOURCE_ATTRIBUTES`.
 * `service.instance.id`, `telemetry.sdk.name` and `telemetry.sdk.language` are always those of kollect, so the same
 * keys from the environment or the caller are left out. Every other key is allowed for this resource only, so it still
 * passes the checks of kollect: the grammar of a key, at most 256 characters without control characters per value and
 * at most 32 attributes.
 *
 * kollect itself detects nothing about the host; calling this function is the decision of the application to send
 * these facts to its receiver.
 *
 * @param serviceName the name of the service, or `null` to read it from the environment.
 * @param attributes the attributes of the caller, which win over every other source.
 * @param environment the environment to read the variables of OpenTelemetry from; the live one by default.
 * @param host the host whose facts are added; the current one by default.
 * @return the resource.
 * @throws IllegalArgumentException if no service name is found, if `OTEL_RESOURCE_ATTRIBUTES` is malformed, or if
 * kollect refuses a key, a value or the number of attributes. The message never repeats a value.
 * @throws com.davils.knot.environment.EnvironmentException if [environment] cannot read a variable.
 * @since 1.1.0
 */
public fun hostResource(
    serviceName: String? = null,
    attributes: Map<String, String> = emptyMap(),
    environment: Environment = Environment.SYSTEM,
    host: Host = Host.current,
): Resource {
    val listed = environment.variable(ATTRIBUTES_VARIABLE)?.let(::resourceAttributesOf)
    val fromEnvironment = listed.orEmpty().toMap()
    val merged = hostAttributesOf(host) + fromEnvironment + attributes
    val name = serviceName ?: environment.variable(SERVICE_NAME_VARIABLE) ?: merged[SERVICE_NAME]
    requireNotNull(name) { MISSING_SERVICE }
    val added = merged.filterKeys { key -> key !in keysOfKollect }
    return resource(name) {
        added.forEach { (key, value) ->
            if (key !in keysAllowedByDefault) allow(key)
            attribute(key, value)
        }
    }
}

private fun hostAttributesOf(host: Host): Map<String, String> = buildMap {
    osTypeOf(host.system)?.let { type -> put(key = OS_TYPE, value = type) }
    architectureOf(host.architecture)?.let { architecture -> put(key = HOST_ARCH, value = architecture) }
    put(key = RUNTIME_NAME, value = host.runtime.identifier)
    host.runtimeVersion?.let { version -> put(key = RUNTIME_VERSION, value = version) }
}

private fun osTypeOf(system: HostSystem): String? = when (system) {
    HostSystem.Linux, HostSystem.Android, HostSystem.ChromeOs -> "linux"
    HostSystem.MacOs, HostSystem.Ios -> "darwin"
    HostSystem.Windows -> "windows"
    HostSystem.FreeBsd -> "freebsd"
    HostSystem.Unknown -> null
}

private fun architectureOf(architecture: HostArchitecture): String? = when (architecture) {
    HostArchitecture.X64 -> "amd64"
    HostArchitecture.Arm64 -> "arm64"
    HostArchitecture.X86 -> "x86"
    HostArchitecture.Arm32 -> "arm32"
    HostArchitecture.Unknown -> null
}
