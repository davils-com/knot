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

private const val LINUX: String = "linux"
private const val WINDOWS: String = "windows"
private const val ANDROID: String = "android"
private const val FREEBSD: String = "freebsd"

private val reportedPrefixes: List<Pair<String, HostSystem>> = listOf(
    LINUX to HostSystem.Linux,
    "mac" to HostSystem.MacOs,
    "darwin" to HostSystem.MacOs,
    WINDOWS to HostSystem.Windows,
    "win32" to HostSystem.Windows,
    ANDROID to HostSystem.Android,
    FREEBSD to HostSystem.FreeBsd,
)

private val clientHintNames: Map<String, HostSystem> = mapOf(
    LINUX to HostSystem.Linux,
    "macos" to HostSystem.MacOs,
    WINDOWS to HostSystem.Windows,
    ANDROID to HostSystem.Android,
    "ios" to HostSystem.Ios,
    "chrome os" to HostSystem.ChromeOs,
    "chromeos" to HostSystem.ChromeOs,
    FREEBSD to HostSystem.FreeBsd,
)

private val userAgentTokens: List<Pair<String, HostSystem>> = listOf(
    ANDROID to HostSystem.Android,
    "cros" to HostSystem.ChromeOs,
    "iphone" to HostSystem.Ios,
    "ipad" to HostSystem.Ios,
    "ipod" to HostSystem.Ios,
    WINDOWS to HostSystem.Windows,
    "macintosh" to HostSystem.MacOs,
    "mac os x" to HostSystem.MacOs,
    FREEBSD to HostSystem.FreeBsd,
    LINUX to HostSystem.Linux,
)

private val systemSources: List<Pair<DetectionSource, (HostProbe) -> HostSystem?>> = listOf(
    DetectionSource.SystemProperty to { probe -> probe.osName?.let(::reportedSystem) },
    DetectionSource.DenoBuild to { probe -> probe.denoOs?.let(::reportedSystem) },
    DetectionSource.Process to { probe -> probe.processPlatform?.let(::reportedSystem) },
    DetectionSource.ClientHints to { probe -> probe.clientHintsPlatform?.let(::clientHintSystem) },
    DetectionSource.UserAgent to { probe -> probe.userAgent?.let(::userAgentSystem) },
)

internal fun systemOf(probe: HostProbe): Detected<HostSystem> {
    val detected = systemSources.firstNotNullOfOrNull { (source, read) ->
        read(probe)?.let { system -> Detected(system, source) }
    }
    return detected ?: Detected(HostSystem.Unknown, DetectionSource.None)
}

internal fun reportedSystem(reported: String): HostSystem? {
    val name = comparableName(reported) ?: return null
    return reportedPrefixes.firstOrNull { (prefix, _) -> name.startsWith(prefix) }?.second
}

private fun clientHintSystem(reported: String): HostSystem? {
    val name = comparableName(reported) ?: return null
    return clientHintNames[name]
}

private fun userAgentSystem(reported: String): HostSystem? {
    val agent = comparableName(reported, maxLength = PlatformLimits.MAX_USER_AGENT_LENGTH) ?: return null
    return userAgentTokens.firstOrNull { (token, _) -> token in agent }?.second
}
