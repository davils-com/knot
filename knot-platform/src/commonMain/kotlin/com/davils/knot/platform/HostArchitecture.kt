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

/**
 * The processor architecture of the machine behind a runtime, 32-bit ones included.
 *
 * Unlike [Architecture], which lists the 64-bit architectures with native builds, this lists every architecture a
 * runtime commonly reports. [Unknown] stands for an architecture that was not reported or matches nothing.
 *
 * @property identifier the stable name of the architecture, such as `x86_64`.
 * @property architecture the 64-bit architecture with native builds this is, or `null` for one without.
 * @since 1.0.0
 */
public enum class HostArchitecture(public val identifier: String, public val architecture: Architecture?) {
    /**
     * 64-bit x86.
     *
     * @since 1.0.0
     */
    X64(identifier = "x86_64", architecture = Architecture.X64),

    /**
     * 64-bit ARM.
     *
     * @since 1.0.0
     */
    Arm64(identifier = "aarch64", architecture = Architecture.Arm64),

    /**
     * 32-bit x86.
     *
     * @since 1.0.0
     */
    X86(identifier = "x86", architecture = null),

    /**
     * 32-bit ARM.
     *
     * @since 1.0.0
     */
    Arm32(identifier = "arm", architecture = null),

    /**
     * An architecture that was not reported or matches none of the others.
     *
     * @since 1.0.0
     */
    Unknown(identifier = "unknown", architecture = null),
    ;

    /**
     * Returns the [identifier] of the architecture.
     *
     * @return the identifier, such as `x86_64`.
     * @since 1.0.0
     */
    override fun toString(): String = identifier
}
