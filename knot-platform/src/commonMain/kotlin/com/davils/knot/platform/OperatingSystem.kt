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

/**
 * An operating system that Davils applications are built for.
 *
 * @property identifier the name of the system in a platform identifier, such as `linux`.
 * @property isUnix whether the system is a Unix, which Linux and macOS are and Windows is not.
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
    ;

    /**
     * Resolves the operating systems that a runtime reports.
     *
     * @since 1.0.0
     */
    public companion object {
        /**
         * Returns the operating system that [osName] names, as a JVM or Node.js reports it.
         *
         * The name matches when it starts with a known prefix, so `Windows 11` is [Windows]. Surrounding whitespace
         * and case are ignored. A name longer than
         * [Limits.MAX_NAME_LENGTH][com.davils.kore.core.Limits.MAX_NAME_LENGTH] or with a character outside printable
         * ASCII is refused before it is compared.
         *
         * @param osName the reported name, such as `Linux`, `Mac OS X` or `win32`.
         * @return the operating system, or `null` if the name matches none.
         * @since 1.0.0
         */
        public fun resolve(osName: String): OperatingSystem? {
            val name = comparableName(osName) ?: return null
            return namePrefixes.firstOrNull { (prefix, _) -> name.startsWith(prefix) }?.second
        }
    }
}
