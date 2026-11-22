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

package com.davils.knot.failure

/**
 * The upper bounds that a failure handler, its storm guard, its sinks and its crash reports are held to.
 *
 * A declaration beyond a limit is refused when the handler is built; at run time a limit keeps a storm of failures
 * from exhausting the memory of the process. Raising a limit is a compatible change, lowering one a breaking change.
 *
 * @since 1.0.0
 */
public object FailureLimits {
    /**
     * The most fingerprints whose storm window the handler keeps at once; beyond it, the oldest window is forgotten.
     *
     * @since 1.0.0
     */
    public const val MAX_TRACKED_FINGERPRINTS: Int = 1_024

    /**
     * The most failures the count of recent failures holds.
     *
     * @since 1.0.0
     */
    public const val MAX_RECENT: Int = 10_000

    /**
     * The most sinks one handler writes to.
     *
     * @since 1.0.0
     */
    public const val MAX_SINKS: Int = 16

    /**
     * The longest name of a sink.
     *
     * @since 1.0.0
     */
    public const val MAX_SINK_NAME_LENGTH: Int = 64

    /**
     * The deepest cause chain that classification and crash reports walk.
     *
     * @since 1.0.0
     */
    public const val MAX_CAUSE_DEPTH: Int = 32

    /**
     * The most stack frames a crash report shows per cause.
     *
     * @since 1.0.0
     */
    public const val MAX_FRAMES: Int = 256

    /**
     * The longest crash report a renderer produces, in UTF-16 code units; a longer one is cut and marked as cut.
     *
     * @since 1.0.0
     */
    public const val MAX_REPORT_LENGTH: Int = 1_048_576

    /**
     * The most failures one storm window reports per fingerprint.
     *
     * @since 1.0.0
     */
    public const val MAX_STORM_REPORTS: Int = 10_000

    /**
     * The most failures the [FailureHandler.failures] flow keeps for a slow collector before it drops the oldest.
     *
     * @since 1.0.0
     */
    public const val MAX_EVENT_CAPACITY: Int = 4_096
}
