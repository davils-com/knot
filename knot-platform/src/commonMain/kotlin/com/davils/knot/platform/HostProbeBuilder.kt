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

import com.davils.kore.core.annotation.DavilsDsl

/**
 * Declares the raw facts a runtime reports about itself, for [hostProbe].
 *
 * Every fact is optional; one that is not declared counts as not reported. The facts are read as they are reported
 * and bounded only when a [Host] is detected from them, so a builder accepts any text. A builder is meant for one
 * thread and one block.
 *
 * @since 1.0.0
 */
@DavilsDsl
public class HostProbeBuilder internal constructor() {
    /**
     * Whether the program runs on a JVM.
     *
     * @since 1.0.0
     */
    public var isJvm: Boolean = false

    /**
     * The system property `os.name` of the JVM, such as `Linux`.
     *
     * @since 1.0.0
     */
    public var osName: String? = null

    /**
     * The system property `os.arch` of the JVM, such as `amd64`.
     *
     * @since 1.0.0
     */
    public var osArch: String? = null

    /**
     * The system property `java.version` of the JVM, such as `25.0.1`.
     *
     * @since 1.0.0
     */
    public var javaVersion: String? = null

    /**
     * `process.platform` of Node.js or Bun, such as `linux` or `win32`.
     *
     * @since 1.0.0
     */
    public var processPlatform: String? = null

    /**
     * `process.arch` of Node.js or Bun, such as `x64`.
     *
     * @since 1.0.0
     */
    public var processArch: String? = null

    /**
     * `process.versions.node` of Node.js, such as `22.11.0`.
     *
     * @since 1.0.0
     */
    public var nodeVersion: String? = null

    /**
     * `Deno.build.os` of Deno, such as `darwin`.
     *
     * @since 1.0.0
     */
    public var denoOs: String? = null

    /**
     * `Deno.build.arch` of Deno, such as `aarch64`.
     *
     * @since 1.0.0
     */
    public var denoArch: String? = null

    /**
     * `Deno.version.deno` of Deno, such as `2.1.4`.
     *
     * @since 1.0.0
     */
    public var denoVersion: String? = null

    /**
     * `Bun.version` of Bun, such as `1.1.38`.
     *
     * @since 1.0.0
     */
    public var bunVersion: String? = null

    /**
     * Whether the global scope has a `window` and a `document`, as the main thread of a browser page has.
     *
     * @since 1.0.0
     */
    public var hasWindow: Boolean = false

    /**
     * Whether the global scope is a `DedicatedWorkerGlobalScope`.
     *
     * @since 1.0.0
     */
    public var isDedicatedWorker: Boolean = false

    /**
     * Whether the global scope is a `SharedWorkerGlobalScope`.
     *
     * @since 1.0.0
     */
    public var isSharedWorker: Boolean = false

    /**
     * Whether the global scope is a `ServiceWorkerGlobalScope`.
     *
     * @since 1.0.0
     */
    public var isServiceWorker: Boolean = false

    /**
     * `navigator.userAgent`, the user agent string.
     *
     * @since 1.0.0
     */
    public var userAgent: String? = null

    /**
     * `navigator.userAgentData.platform`, such as `Windows` or `macOS`.
     *
     * @since 1.0.0
     */
    public var clientHintsPlatform: String? = null

    /**
     * `navigator.userAgentData.mobile`.
     *
     * @since 1.0.0
     */
    public var isClientHintsMobile: Boolean? = null

    /**
     * The high entropy client hint `architecture`, such as `x86` or `arm`.
     *
     * @since 1.0.0
     */
    public var clientHintsArchitecture: String? = null

    /**
     * The high entropy client hint `bitness`, such as `64`.
     *
     * @since 1.0.0
     */
    public var clientHintsBitness: String? = null

    internal fun build(): HostProbe = HostProbe(this)
}
