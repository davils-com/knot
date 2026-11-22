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
 * Where a fact of a [Host] comes from, and whether it can be trusted.
 *
 * A runtime reports its own system and architecture through an authoritative source, such as the system properties
 * of the JVM or `process.platform` of Node.js. A browser does not: its client hints and its user agent are set by the
 * browser, can be changed by the user and by extensions, and are a hint only. Knot never derives a native [Platform]
 * from a source that is not authoritative.
 *
 * @property identifier the stable name of the source, such as `process`.
 * @property isAuthoritative whether the runtime itself reports the fact.
 * @since 1.0.0
 */
public enum class DetectionSource(public val identifier: String, public val isAuthoritative: Boolean) {
    /**
     * The system properties `os.name` and `os.arch` of the JVM.
     *
     * @since 1.0.0
     */
    SystemProperty(identifier = "system-property", isAuthoritative = true),

    /**
     * `process.platform` and `process.arch` of Node.js and Bun.
     *
     * @since 1.0.0
     */
    Process(identifier = "process", isAuthoritative = true),

    /**
     * `Deno.build.os` and `Deno.build.arch` of Deno.
     *
     * @since 1.0.0
     */
    DenoBuild(identifier = "deno-build", isAuthoritative = true),

    /**
     * The user agent client hints of a browser, `navigator.userAgentData`.
     *
     * @since 1.0.0
     */
    ClientHints(identifier = "client-hints", isAuthoritative = false),

    /**
     * The tokens of the user agent string, `navigator.userAgent`.
     *
     * @since 1.0.0
     */
    UserAgent(identifier = "user-agent", isAuthoritative = false),

    /**
     * No source reported the fact.
     *
     * @since 1.0.0
     */
    None(identifier = "none", isAuthoritative = false),
    ;

    /**
     * Returns the [identifier] of the source.
     *
     * @return the identifier, such as `process`.
     * @since 1.0.0
     */
    override fun toString(): String = identifier
}
