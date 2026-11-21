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

private val namePrefixes: List<Pair<String, OperatingSystem>> = listOf(
    "linux" to OperatingSystem.Linux,
    "mac" to OperatingSystem.MacOs,
    "darwin" to OperatingSystem.MacOs,
    "windows" to OperatingSystem.Windows,
    "win32" to OperatingSystem.Windows,
)

private val exactNames: Map<String, OperatingSystem> = mapOf(
    "web" to OperatingSystem.Web,
)

/**
 * An operating system that Davils applications are built for, or [Web] for a program that runs on WebAssembly.
 *
 * Every system but [Web] has native builds, one per [Architecture], which [Platform] lists.
 *
 * @property identifier the name of the system in a platform identifier, such as `linux`.
 * @property isUnix whether the system is a Unix, which Linux and macOS are and Windows and the web are not.
 * @since 1.0.0
 */
public enum class OperatingSystem(public val identifier: String, public val isUnix: Boolean) {
    /**
     * Linux, glibc and musl alike.
     *
     * @since 1.0.0
     */
    Linux(identifier = "linux", isUnix = true),

    /**
     * macOS, also reported as `Mac OS X` and `darwin`.
     *
     * @since 1.0.0
     */
    MacOs(identifier = "macos", isUnix = true),

    /**
     * Windows, also reported as `win32` by Node.js.
     *
     * @since 1.0.0
     */
    Windows(identifier = "windows", isUnix = false),

    /**
     * The web: every runtime of the `wasmJs` target, a browser and Node.js alike, whatever machine runs it.
     *
     * It has no native build, so no [Platform] is web. Only the exact name `web` resolves to it.
     *
     * @since 1.0.0
     */
    Web(identifier = "web", isUnix = false),
    ;

    /**
     * Whether this is [Web], the system of every runtime of the `wasmJs` target.
     *
     * @since 1.0.0
     */
    public val isWeb: Boolean get() = this == Web

    /**
     * Resolves the operating systems that a runtime reports.
     *
     * @since 1.0.0
     */
    public companion object {
        /**
         * The operating system this program runs on, or `null` if the JVM reports a system without a build.
         *
         * On the JVM it is read from the system property `os.name`; on the `wasmJs` target it is always [Web], in a
         * browser and under Node.js alike. It is read once, on first use, and then kept; it is safe to share between
         * threads.
         *
         * @since 1.0.0
         */
        public val current: OperatingSystem? by lazy { resolve(hostOsName()) }

        /**
         * Returns the operating system that [osName] names, as a JVM or Node.js reports it.
         *
         * The name matches when it starts with a known prefix, so `Windows 11` is [Windows]; [Web] matches only the
         * exact name `web`, so that a system such as `webOS` is not taken for the web. Surrounding whitespace and case
         * are ignored. A name longer than
         * [Limits.MAX_NAME_LENGTH][com.davils.kore.core.Limits.MAX_NAME_LENGTH] or with a character outside printable
         * ASCII is refused before it is compared.
         *
         * @param osName the reported name, such as `Linux`, `Mac OS X`, `win32` or `web`.
         * @return the operating system, or `null` if the name matches none.
         * @since 1.0.0
         */
        public fun resolve(osName: String): OperatingSystem? {
            val name = comparableName(osName) ?: return null
            val prefixed = namePrefixes.firstOrNull { (prefix, _) -> name.startsWith(prefix) }?.second
            return exactNames[name] ?: prefixed
        }
    }
}
