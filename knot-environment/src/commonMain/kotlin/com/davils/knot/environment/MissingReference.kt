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
 * What an expansion does with a reference `${NAME}` to a variable that is not set, see [EnvironmentBuilder.expand].
 *
 * @since 1.0.0
 */
public enum class MissingReference {
    /**
     * The read fails with an [EnvironmentException] of [EnvironmentViolationCodes.UNRESOLVED_REFERENCE]; the default,
     * so that a missing variable is never mistaken for an empty one.
     *
     * @since 1.0.0
     */
    Fail,

    /**
     * The reference expands to nothing, as in a POSIX shell.
     *
     * @since 1.0.0
     */
    Empty,

    /**
     * The reference stays in the value as it was written.
     *
     * @since 1.0.0
     */
    Keep,
}
