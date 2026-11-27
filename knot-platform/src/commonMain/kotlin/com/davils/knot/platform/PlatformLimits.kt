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
 * The upper bounds that the facts a runtime reports are held to before Knot reads them.
 *
 * A reported name or version longer than its bound, or with a character outside printable ASCII, is not cut but
 * ignored, so that the fact counts as not reported. Raising a limit is a compatible change, lowering one a breaking
 * change.
 *
 * @since 1.0.0
 */
public object PlatformLimits {
    /**
     * The longest user agent string the host detection reads; a longer one is ignored.
     *
     * @since 1.0.0
     */
    public const val MAX_USER_AGENT_LENGTH: Int = 512

    /**
     * The longest version of a runtime that [Host.runtimeVersion] keeps; a longer one is ignored.
     *
     * @since 1.0.0
     */
    public const val MAX_VERSION_LENGTH: Int = 64

    /**
     * The longest host name that [Host.hostName] keeps, the bound of RFC 1123; a longer one is ignored.
     *
     * @since 1.3.0
     */
    public const val MAX_HOST_NAME_LENGTH: Int = 253
}
