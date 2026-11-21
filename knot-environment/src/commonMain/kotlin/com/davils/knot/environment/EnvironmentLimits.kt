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

/**
 * The upper bounds that a declaration of an [environment] is held to.
 *
 * A declaration beyond a limit is not kept but reported once, so a hostile or runaway declaration can neither exhaust
 * the memory of a process nor slow every read down. The names and values of variables are bounded by
 * [Limits.MAX_NAME_LENGTH][com.davils.kore.core.Limits.MAX_NAME_LENGTH] and
 * [Limits.MAX_ARGUMENT_LENGTH][com.davils.kore.core.Limits.MAX_ARGUMENT_LENGTH] of Kore. Raising a limit is a
 * compatible change, lowering one a breaking change.
 *
 * @since 1.0.0
 */
public object EnvironmentLimits {
    /**
     * The most layers one environment stacks on its base.
     *
     * @since 1.0.0
     */
    public const val MAX_LAYERS: Int = 64

    /**
     * The most changes one environment declares, overrides and removals counted together.
     *
     * @since 1.0.0
     */
    public const val MAX_CHANGES: Int = 4_096
}
