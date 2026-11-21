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
import com.davils.kore.core.collection.immutableList
import com.davils.kore.core.collection.immutableSet
import com.davils.kore.core.text.SafeText

private const val SEPARATOR: Char = '-'

/**
 * A pair of an [OperatingSystem] and an [Architecture] that Davils applications are built for.
 *
 * Every pair of a system with native builds and a supported architecture is a platform, and [entries] lists them all;
 * [OperatingSystem.Wasm] has no native build and is part of no platform. A
 * platform is immutable and equal to every other platform of the same system and architecture, so two spellings of
 * one machine, such as `Darwin`/`arm64` and `Mac OS X`/`aarch64`, give equal platforms.
 *
 * @property operatingSystem the operating system.
 * @property architecture the processor architecture.
 * @since 1.0.0
 */
public class Platform private constructor(
    public val operatingSystem: OperatingSystem,
    public val architecture: Architecture,
) {
    /**
     * The name of the platform in the form `<system>-<architecture>`, such as `linux-x86_64` or `macos-aarch64`, as
     * Kreate names the directory of the native binaries of a platform.
     *
     * @since 1.0.0
     */
    public val identifier: String get() = "${operatingSystem.identifier}$SEPARATOR${architecture.identifier}"

    /**
     * Whether the operating system is [OperatingSystem.Linux].
     *
     * @since 1.0.0
     */
    public val isLinux: Boolean get() = operatingSystem == OperatingSystem.Linux

    /**
     * Whether the operating system is [OperatingSystem.MacOs].
     *
     * @since 1.0.0
     */
    public val isMacOs: Boolean get() = operatingSystem == OperatingSystem.MacOs

    /**
     * Whether the operating system is [OperatingSystem.Windows].
     *
     * @since 1.0.0
     */
    public val isWindows: Boolean get() = operatingSystem == OperatingSystem.Windows

    /**
     * Whether the operating system is a Unix, see [OperatingSystem.isUnix].
     *
     * @since 1.0.0
     */
    public val isUnix: Boolean get() = operatingSystem.isUnix

    /**
     * Tells whether [other] is a platform with the same operating system and architecture.
     *
     * @param other the object to compare with.
     * @return `true` if [other] is an equal platform.
     * @since 1.0.0
     */
    override fun equals(other: Any?): Boolean =
        other is Platform && operatingSystem == other.operatingSystem && architecture == other.architecture

    /**
     * Returns a hash code derived from the [identifier], consistent with [equals].
     *
     * @return the hash code.
     * @since 1.0.0
     */
    override fun hashCode(): Int = identifier.hashCode()

    /**
     * Returns the [identifier] of the platform.
     *
     * @return the identifier, such as `linux-x86_64`.
     * @since 1.0.0
     */
    override fun toString(): String = identifier

    /**
     * Finds, reads and lists platforms.
     *
     * @since 1.0.0
     */
    public companion object {
        /**
         * Every supported platform, each operating system but [OperatingSystem.Wasm] paired with each architecture, in
         * the order of the entries of [OperatingSystem] and then of [Architecture].
         *
         * @since 1.0.0
         */
        public val entries: List<Platform> = pairs().immutableList()

        /**
         * The [identifier] of every supported platform.
         *
         * @since 1.0.0
         */
        public val identifiers: Set<String> = entries.map { platform -> platform.identifier }.immutableSet()

        /**
         * The platform this program runs on, or `null` if the JVM reports a system or an architecture that is not
         * supported.
         *
         * It is read once, on first use, from the system properties `os.name` and `os.arch` on the JVM, and then kept;
         * it is safe to share between threads. On the `wasmJs` target, in a browser and under Node.js alike, the
         * operating system is [OperatingSystem.Wasm], which has no native build, so there is none; check
         * [OperatingSystem.current] instead.
         *
         * @since 1.0.0
         */
        public val current: Platform? by lazy { resolve(osName = hostOsName(), osArch = hostOsArch()) }

        /**
         * Returns the platform this program runs on and refuses to go on without one.
         *
         * @return the current platform.
         * @throws IllegalStateException if the runtime reports a system or an architecture that is not supported, and
         * always on the `wasmJs` target, whose system is [OperatingSystem.Wasm].
         * @since 1.0.0
         */
        public fun requireCurrent(): Platform = requireSupported(osName = hostOsName(), osArch = hostOsArch())

        /**
         * Returns the platform that the names [osName] and [osArch] describe, in any spelling that
         * [OperatingSystem.resolve] and [Architecture.resolve] accept.
         *
         * @param osName the reported name of the operating system, such as `Linux` or `Windows 11`.
         * @param osArch the reported name of the architecture, such as `amd64`.
         * @return the platform, or `null` if either name matches nothing supported or the system is
         * [OperatingSystem.Wasm].
         * @since 1.0.0
         */
        public fun resolve(osName: String, osArch: String): Platform? {
            val operatingSystem = OperatingSystem.resolve(osName)
            val architecture = Architecture.resolve(osArch)
            if (operatingSystem == null || architecture == null) return null
            return entries.firstOrNull { platform ->
                platform.operatingSystem == operatingSystem && platform.architecture == architecture
            }
        }

        /**
         * Returns the platform whose [identifier] is [identifier].
         *
         * @param identifier the identifier, such as `linux-x86_64`, compared exactly.
         * @return the platform.
         * @throws IllegalArgumentException if [identifier] names no supported platform; the message lists the
         * supported identifiers but never repeats [identifier].
         * @since 1.0.0
         */
        public fun parse(identifier: String): Platform = requireNotNull(parseOrNull(identifier)) {
            "Not a supported platform, which is one of ${supportedList()}"
        }

        /**
         * Returns the platform whose [identifier] is [identifier], or `null` if there is none.
         *
         * @param identifier the identifier, such as `linux-x86_64`, compared exactly.
         * @return the platform, or `null` if [identifier] names no supported platform.
         * @since 1.0.0
         */
        public fun parseOrNull(identifier: String): Platform? =
            entries.firstOrNull { platform -> platform.identifier == identifier }

        internal fun requireSupported(osName: String, osArch: String): Platform =
            checkNotNull(resolve(osName = osName, osArch = osArch)) {
                val system = SafeText.quoted(osName)
                val architecture = SafeText.quoted(osArch)
                "The system $system with the architecture $architecture is not supported, only ${supportedList()} are"
            }

        private fun pairs(): List<Platform> {
            val native = OperatingSystem.entries.filterNot { operatingSystem -> operatingSystem.isWasm }
            return native.flatMap { operatingSystem ->
                Architecture.entries.map { architecture -> Platform(operatingSystem, architecture) }
            }
        }

        private fun supportedList(): String {
            val sorted = identifiers.sorted()
            return sorted.joinToString()
        }
    }
}
