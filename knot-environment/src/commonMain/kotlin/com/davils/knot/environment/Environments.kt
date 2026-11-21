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
 * Declares an environment of layers and overrides on top of [base].
 *
 * ```kotlin
 * val environment = environment(base = Environment.SYSTEM) {
 *     layer(defaults)
 *     layer { variable("KNOT_LOG_LEVEL", "info") }
 *     variable("KNOT_HOME", "/opt/knot")
 *     remove("KNOT_TOKEN")
 *     scope("KNOT_")
 * }
 * ```
 *
 * The rules of each declaration are those of [EnvironmentBuilder]; the order of the result is described by
 * [LayeredEnvironment]. An environment declared on [Environment.EMPTY] with nothing but overrides is a complete fake
 * for tests.
 *
 * @param base the environment the declaration starts from; [Environment.EMPTY] by default.
 * @param block declares the layers, overrides, removals and the scope.
 * @return the declared environment.
 * @throws com.davils.kore.core.validation.report.ValidationException if a declaration breaks a rule; its report
 * lists every problem and never a value.
 * @since 1.0.0
 */
public fun environment(base: Environment = Environment.EMPTY, block: EnvironmentBuilder.() -> Unit): Environment {
    val builder = EnvironmentBuilder(base)
    builder.block()
    return builder.build()
}
