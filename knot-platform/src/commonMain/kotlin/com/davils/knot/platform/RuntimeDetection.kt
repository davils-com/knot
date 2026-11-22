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

private const val MOBILE_TOKEN: String = "mobi"

private val runtimeChecks: List<Pair<(HostProbe) -> Boolean, HostRuntime>> = listOf(
    { probe: HostProbe -> probe.isJvm } to HostRuntime.Jvm,
    { probe: HostProbe -> probe.denoVersion != null || probe.denoOs != null } to HostRuntime.Deno,
    { probe: HostProbe -> probe.bunVersion != null } to HostRuntime.Bun,
    { probe: HostProbe -> probe.nodeVersion != null } to HostRuntime.NodeJs,
    { probe: HostProbe -> probe.isServiceWorker } to HostRuntime.ServiceWorker,
    { probe: HostProbe -> probe.isSharedWorker } to HostRuntime.SharedWorker,
    { probe: HostProbe -> probe.isDedicatedWorker } to HostRuntime.DedicatedWorker,
    { probe: HostProbe -> probe.hasWindow } to HostRuntime.Browser,
)

internal fun runtimeOf(probe: HostProbe): HostRuntime =
    runtimeChecks.firstOrNull { (check, _) -> check(probe) }?.second ?: HostRuntime.UnknownWasm

internal fun runtimeVersionOf(probe: HostProbe, runtime: HostRuntime): String? {
    val reported = when (runtime) {
        HostRuntime.Jvm -> probe.javaVersion

        HostRuntime.Deno -> probe.denoVersion

        HostRuntime.Bun -> probe.bunVersion

        HostRuntime.NodeJs -> probe.nodeVersion

        HostRuntime.Browser,
        HostRuntime.DedicatedWorker,
        HostRuntime.SharedWorker,
        HostRuntime.ServiceWorker,
        HostRuntime.UnknownWasm,
        -> null
    }
    return reportedVersion(reported)
}

internal fun mobileOf(probe: HostProbe, runtime: HostRuntime): Boolean? {
    if (runtime.hasProcess) return false
    val agent = probe.userAgent?.let { reported ->
        comparableName(reported, maxLength = PlatformLimits.MAX_USER_AGENT_LENGTH)
    }
    return probe.isClientHintsMobile ?: agent?.let { text -> MOBILE_TOKEN in text }
}
