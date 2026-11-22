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
 * The operating system of the machine behind a runtime, as far as the runtime tells it.
 *
 * Unlike [OperatingSystem], which lists the systems with native builds, this lists every system a runtime commonly
 * reports, the mobile ones included. [Unknown] stands for a system that was not reported or matches nothing.
 *
 * @property identifier the stable name of the system, such as `android`.
 * @property operatingSystem the system with native builds this is, or `null` for a system without one.
 * @since 1.0.0
 */
public enum class HostSystem(public val identifier: String, public val operatingSystem: OperatingSystem?) {
    /**
     * Linux, other than Android and ChromeOS.
     *
     * @since 1.0.0
     */
    Linux(identifier = "linux", operatingSystem = OperatingSystem.Linux),

    /**
     * macOS.
     *
     * @since 1.0.0
     */
    MacOs(identifier = "macos", operatingSystem = OperatingSystem.MacOs),

    /**
     * Windows.
     *
     * @since 1.0.0
     */
    Windows(identifier = "windows", operatingSystem = OperatingSystem.Windows),

    /**
     * Android.
     *
     * @since 1.0.0
     */
    Android(identifier = "android", operatingSystem = null),

    /**
     * iOS and iPadOS, as far as iPadOS does not present itself as macOS.
     *
     * @since 1.0.0
     */
    Ios(identifier = "ios", operatingSystem = null),

    /**
     * ChromeOS.
     *
     * @since 1.0.0
     */
    ChromeOs(identifier = "chromeos", operatingSystem = null),

    /**
     * FreeBSD.
     *
     * @since 1.0.0
     */
    FreeBsd(identifier = "freebsd", operatingSystem = null),

    /**
     * A system that was not reported or matches none of the others.
     *
     * @since 1.0.0
     */
    Unknown(identifier = "unknown", operatingSystem = null),
    ;

    /**
     * Returns the [identifier] of the system.
     *
     * @return the identifier, such as `linux`.
     * @since 1.0.0
     */
    override fun toString(): String = identifier
}
