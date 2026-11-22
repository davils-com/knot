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

package com.davils.knot.environment

import com.davils.kore.core.Limits

/**
 * The upper bounds that a declaration of an [environment], a read of it and an
 * [EnvironmentSchema][com.davils.knot.environment.schema.EnvironmentSchema] are held to.
 *
 * A declaration beyond a limit is not kept but reported once, so a hostile or runaway declaration can neither exhaust
 * the memory of a process nor slow every read down; a read beyond a limit fails with an [EnvironmentException]. The
 * names and values of variables are bounded by [Limits.MAX_NAME_LENGTH] and [Limits.MAX_ARGUMENT_LENGTH] of Kore.
 * Raising a limit is a compatible change, lowering one a breaking change.
 *
 * @since 1.0.0
 */
public object EnvironmentLimits {
    /**
     * The most layers one environment stacks on its base, dotenv texts and maps included.
     *
     * @since 1.0.0
     */
    public const val MAX_LAYERS: Int = 64

    /**
     * The most changes one environment declares: overrides, removals and defaults counted together.
     *
     * @since 1.0.0
     */
    public const val MAX_CHANGES: Int = 4_096

    /**
     * The most rules one environment declares: aliases, filters and required variables counted together.
     *
     * @since 1.0.0
     */
    public const val MAX_RULES: Int = 1_024

    /**
     * The most fallback names one alias lists.
     *
     * @since 1.0.0
     */
    public const val MAX_FALLBACKS: Int = 16

    /**
     * The longest dotenv text that [EnvironmentBuilder.dotenv] reads, in UTF-16 code units.
     *
     * @since 1.0.0
     */
    public const val MAX_DOTENV_LENGTH: Int = 1_048_576

    /**
     * The most lines one dotenv text holds.
     *
     * @since 1.0.0
     */
    public const val MAX_DOTENV_LINES: Int = 16_384

    /**
     * The deepest chain of references that one expansion follows, such as `A` referring to `B` referring to `C`.
     *
     * @since 1.0.0
     */
    public const val MAX_EXPANSION_DEPTH: Int = 16

    /**
     * The longest value that an expansion produces, in UTF-16 code units.
     *
     * @since 1.0.0
     */
    public const val MAX_EXPANDED_LENGTH: Int = Limits.MAX_ARGUMENT_LENGTH

    /**
     * The most settings one [EnvironmentSchema][com.davils.knot.environment.schema.EnvironmentSchema] declares.
     *
     * @since 1.0.0
     */
    public const val MAX_SETTINGS: Int = 1_024

    /**
     * The most elements a list or a map that one variable holds is split into.
     *
     * @since 1.0.0
     */
    public const val MAX_ELEMENTS: Int = 1_024
}
