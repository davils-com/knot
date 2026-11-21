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

private val reportedNames: Map<String, Architecture> = mapOf(
    "x86_64" to Architecture.X64,
    "amd64" to Architecture.X64,
    "x86-64" to Architecture.X64,
    "x64" to Architecture.X64,
    "em64t" to Architecture.X64,
    "aarch64" to Architecture.Arm64,
    "aarch_64" to Architecture.Arm64,
    "arm64" to Architecture.Arm64,
)

/**
 * A 64-bit processor architecture that Davils applications are built for.
 *
 * Only architectures with a build of their own are listed. A 32-bit machine never resolves to a 64-bit architecture,
 * so a caller refuses an unsupported machine instead of guessing.
 *
 * @property identifier the name of the architecture in a platform identifier, such as `x86_64`.
 * @since 1.0.0
 */
public enum class Architecture(public val identifier: String) {
    /**
     * 64-bit x86, also reported as `amd64`, `x86-64`, `x64` and `em64t`.
     *
     * @since 1.0.0
     */
    X64(identifier = "x86_64"),

    /**
     * 64-bit ARM, also reported as `aarch_64` and `arm64`.
     *
     * @since 1.0.0
     */
    Arm64(identifier = "aarch64"),
    ;

    /**
     * Resolves the architectures that a runtime reports.
     *
     * @since 1.0.0
     */
    public companion object {
        /**
         * Returns the architecture that [osArch] names, in any spelling a JVM, Node.js or a toolchain reports.
         *
         * Surrounding whitespace and case are ignored. A name longer than
         * [Limits.MAX_NAME_LENGTH][com.davils.kore.core.Limits.MAX_NAME_LENGTH] or with a character outside printable
         * ASCII is refused before it is compared.
         *
         * @param osArch the reported name, such as `amd64` or `arm64`.
         * @return the architecture, or `null` if the name matches none.
         * @since 1.0.0
         */
        public fun resolve(osArch: String): Architecture? {
            val name = comparableName(osArch) ?: return null
            return reportedNames[name]
        }
    }
}
