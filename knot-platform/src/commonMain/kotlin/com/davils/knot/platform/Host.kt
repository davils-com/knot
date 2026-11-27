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

@file:OptIn(DavilsInternal::class)

package com.davils.knot.platform

import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.text.SafeText

private const val UNKNOWN: String = "unknown"

/**
 * The machine and the runtime a program runs on: the [runtime], the [system] and the [architecture] behind it, and
 * where each fact comes from.
 *
 * On the JVM the facts come from the system properties. On the `wasmJs` target the same klib detects whether it runs
 * in a browser window, a web worker, Node.js, Deno or Bun, and reads the system and the architecture of the machine
 * behind it: authoritatively from `process` or `Deno.build`, and as a hint from the client hints and the user agent of
 * a browser. No native build target is needed for any of it.
 *
 * A host is immutable and safe to share between threads. Two hosts are equal when every fact and its source are equal.
 *
 * Security: a browser's client hints and user agent are set by the browser, the user and its extensions, so a fact
 * whose [DetectionSource] is not authoritative is a hint and never decides anything that matters for security.
 * [platform] is derived from authoritative sources only.
 *
 * @property runtime the runtime the program runs in.
 * @property system the operating system behind the runtime, [HostSystem.Unknown] if none was reported.
 * @property architecture the processor architecture behind the runtime, [HostArchitecture.Unknown] if none was
 * reported.
 * @property systemSource where [system] comes from.
 * @property architectureSource where [architecture] comes from.
 * @property runtimeVersion the version the runtime reports, such as `22.11.0` for Node.js, or `null` in a browser or
 * when it is longer than [PlatformLimits.MAX_VERSION_LENGTH] or not printable ASCII.
 * @property isMobile whether the host is a mobile device, as far as a browser tells it; `false` for a runtime with a
 * process of its own and `null` when a browser does not tell.
 * @property processId the id the operating system gave the process, or `null` in a browser, in a web worker or when
 * the runtime reports none or one that is not positive. Since 1.3.0.
 * @property hostName the name of the machine from the environment variable `HOSTNAME`, or else `COMPUTERNAME`, or
 * `null` if neither holds a host name of RFC 1123 of at most [PlatformLimits.MAX_HOST_NAME_LENGTH] characters. Knot
 * never asks DNS and never reads a file for it, so the name is what the environment of the process claims and never
 * decides anything that matters for security. Since 1.3.0.
 * @since 1.0.0
 */
public class Host private constructor(
    public val runtime: HostRuntime,
    public val system: HostSystem,
    public val architecture: HostArchitecture,
    public val systemSource: DetectionSource,
    public val architectureSource: DetectionSource,
    public val runtimeVersion: String?,
    public val isMobile: Boolean?,
    public val processId: Long?,
    public val hostName: String?,
) {
    /**
     * Whether both the system and the architecture come from an authoritative source.
     *
     * @since 1.0.0
     */
    public val isAuthoritative: Boolean get() = systemSource.isAuthoritative && architectureSource.isAuthoritative

    /**
     * The native platform of the machine behind the runtime, or `null` if the system or the architecture has no
     * native build or does not come from an authoritative source.
     *
     * A Node.js, Deno or Bun process on Linux x86-64 has the platform `linux-x86_64` even though the program itself
     * runs on WebAssembly; a browser never has one.
     *
     * @since 1.0.0
     */
    public val platform: Platform?
        get() {
            val operatingSystem = system.operatingSystem.takeIf { isAuthoritative }
            val nativeArchitecture = architecture.architecture
            if (operatingSystem == null || nativeArchitecture == null) return null
            return Platform.of(operatingSystem = operatingSystem, architecture = nativeArchitecture)
        }

    private val facts: List<Any?>
        get() = listOf(
            runtime,
            system,
            architecture,
            systemSource,
            architectureSource,
            runtimeVersion,
            isMobile,
            processId,
            hostName,
        )

    /**
     * Tells whether [other] is a host with the same facts from the same sources.
     *
     * @param other the object to compare with.
     * @return `true` if [other] is an equal host.
     * @since 1.0.0
     */
    override fun equals(other: Any?): Boolean = other is Host && facts == other.facts

    /**
     * Returns a hash code derived from every fact, consistent with [equals].
     *
     * @return the hash code.
     * @since 1.0.0
     */
    override fun hashCode(): Int = facts.hashCode()

    /**
     * Returns the facts of the host, the version escaped; the process id and the host name are left out, so that the
     * text does not tell one machine or process from another.
     *
     * @return the facts, such as `Host(runtime=nodejs, system=linux (process), architecture=x86_64 (process),
     * version="22.11.0", mobile=false)`.
     * @since 1.0.0
     */
    override fun toString(): String {
        val version = runtimeVersion?.let(SafeText::quoted) ?: UNKNOWN
        val mobile = isMobile?.toString() ?: UNKNOWN
        return "Host(runtime=$runtime, system=$system ($systemSource), " +
            "architecture=$architecture ($architectureSource), version=$version, mobile=$mobile)"
    }

    /**
     * Detects hosts.
     *
     * @since 1.0.0
     */
    public companion object {
        /**
         * The host this program runs on.
         *
         * It is detected once, on first use, and then kept; it is safe to share between threads. Reading the facts
         * never fails: a fact that a runtime does not offer, or whose access throws, counts as not reported.
         *
         * @since 1.0.0
         */
        public val current: Host by lazy { detect(observedHostProbe()) }

        /**
         * Detects the host this program runs on again, and asks a browser for the high entropy client hints
         * `architecture` and `bitness` as well.
         *
         * Browsers that offer `navigator.userAgentData.getHighEntropyValues`, the Chromium family today, answer
         * asynchronously; elsewhere, on the JVM and outside a browser, the result equals [current]. The answer is a
         * hint, see [DetectionSource.ClientHints].
         *
         * @return the host with the architecture of the client hints where a browser offers them.
         * @since 1.0.0
         */
        public suspend fun detailed(): Host = detect(observedHighEntropyProbe())

        /**
         * Derives a host from the facts of [probe].
         *
         * The runtime is the first that the facts show, in the order JVM, Deno, Bun, Node.js, service worker, shared
         * worker, dedicated worker and browser window. The system and the architecture come from the first source
         * that names a known one, in the order of [DetectionSource]. A text longer than its bound of
         * [PlatformLimits] or [Limits.MAX_NAME_LENGTH][com.davils.kore.core.Limits.MAX_NAME_LENGTH], or with a
         * character outside printable ASCII, is ignored before it is compared.
         *
         * @param probe the facts.
         * @return the host.
         * @since 1.0.0
         */
        public fun detect(probe: HostProbe): Host {
            val runtime = runtimeOf(probe)
            val system = systemOf(probe)
            val architecture = architectureOf(probe)
            return Host(
                runtime = runtime,
                system = system.value,
                architecture = architecture.value,
                systemSource = system.source,
                architectureSource = architecture.source,
                runtimeVersion = runtimeVersionOf(probe, runtime),
                isMobile = mobileOf(probe, runtime),
                processId = processIdOf(probe, runtime),
                hostName = hostNameOf(probe, runtime),
            )
        }
    }
}
