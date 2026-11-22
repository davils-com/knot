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

private const val BITS_64: String = "64"
private const val BITS_32: String = "32"

private val reportedNames: Map<String, HostArchitecture> = mapOf(
    "x86_64" to HostArchitecture.X64,
    "amd64" to HostArchitecture.X64,
    "x86-64" to HostArchitecture.X64,
    "x64" to HostArchitecture.X64,
    "em64t" to HostArchitecture.X64,
    "aarch64" to HostArchitecture.Arm64,
    "aarch_64" to HostArchitecture.Arm64,
    "arm64" to HostArchitecture.Arm64,
    "x86" to HostArchitecture.X86,
    "i386" to HostArchitecture.X86,
    "i486" to HostArchitecture.X86,
    "i586" to HostArchitecture.X86,
    "i686" to HostArchitecture.X86,
    "ia32" to HostArchitecture.X86,
    "arm" to HostArchitecture.Arm32,
    "arm32" to HostArchitecture.Arm32,
    "armv7" to HostArchitecture.Arm32,
    "armv7l" to HostArchitecture.Arm32,
    "armhf" to HostArchitecture.Arm32,
)

private val clientHintNames: Map<Pair<String, String>, HostArchitecture> = mapOf(
    Pair(first = "x86", second = BITS_64) to HostArchitecture.X64,
    Pair(first = "x86", second = BITS_32) to HostArchitecture.X86,
    Pair(first = "arm", second = BITS_64) to HostArchitecture.Arm64,
    Pair(first = "arm", second = BITS_32) to HostArchitecture.Arm32,
)

private val userAgentTokens: List<Pair<String, HostArchitecture>> = listOf(
    "x86_64" to HostArchitecture.X64,
    "win64" to HostArchitecture.X64,
    "wow64" to HostArchitecture.X64,
    "x64" to HostArchitecture.X64,
    "amd64" to HostArchitecture.X64,
    "aarch64" to HostArchitecture.Arm64,
    "arm64" to HostArchitecture.Arm64,
    "armv8l" to HostArchitecture.Arm32,
    "armv7" to HostArchitecture.Arm32,
    "i686" to HostArchitecture.X86,
    "i386" to HostArchitecture.X86,
)

private val architectureSources: List<Pair<DetectionSource, (HostProbe) -> HostArchitecture?>> = listOf(
    DetectionSource.SystemProperty to { probe -> probe.osArch?.let(::reportedArchitecture) },
    DetectionSource.DenoBuild to { probe -> probe.denoArch?.let(::reportedArchitecture) },
    DetectionSource.Process to { probe -> probe.processArch?.let(::reportedArchitecture) },
    DetectionSource.ClientHints to ::clientHintArchitecture,
    DetectionSource.UserAgent to { probe -> probe.userAgent?.let(::userAgentArchitecture) },
)

internal fun architectureOf(probe: HostProbe): Detected<HostArchitecture> {
    val detected = architectureSources.firstNotNullOfOrNull { (source, read) ->
        read(probe)?.let { architecture -> Detected(architecture, source) }
    }
    return detected ?: Detected(HostArchitecture.Unknown, DetectionSource.None)
}

private fun reportedArchitecture(reported: String): HostArchitecture? {
    val name = comparableName(reported) ?: return null
    return reportedNames[name]
}

private fun clientHintArchitecture(probe: HostProbe): HostArchitecture? {
    val architecture = probe.clientHintsArchitecture?.let(::comparableName)
    val bitness = probe.clientHintsBitness?.let(::comparableName)
    if (architecture == null || bitness == null) return null
    return clientHintNames[architecture to bitness]
}

private fun userAgentArchitecture(reported: String): HostArchitecture? {
    val agent = comparableName(reported, maxLength = PlatformLimits.MAX_USER_AGENT_LENGTH) ?: return null
    return userAgentTokens.firstOrNull { (token, _) -> token in agent }?.second
}
