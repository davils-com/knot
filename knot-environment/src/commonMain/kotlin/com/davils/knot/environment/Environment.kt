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
 * A set of environment variables, read by name.
 *
 * [SYSTEM] reads the variables of the running process, [EMPTY] holds none, and [environment] composes one from a base,
 * layers on top of it and overrides. An implementation is safe to share between threads.
 *
 * Environment variables often hold secrets, such as tokens and passwords. No implementation of Knot shows a value in
 * its `toString`; whoever reads a value with [variable] keeps it out of logs.
 *
 * @since 1.0.0
 */
public interface Environment {
    /**
     * The names of all variables, as an immutable snapshot taken when the property is read.
     *
     * @since 1.0.0
     */
    public val names: Set<String>

    /**
     * Returns the value of the variable [name], or `null` if it is not set.
     *
     * @param name the name of the variable, compared exactly.
     * @return the value, or `null` if the variable is not set.
     * @since 1.0.0
     */
    public fun variable(name: String): String?

    /**
     * The environments that need no declaration.
     *
     * @since 1.0.0
     */
    public companion object {
        /**
         * The live environment of the running process: `System.getenv` on the JVM and `process.env` under Node.js.
         * In a browser it holds no variables.
         *
         * Every read asks the process again, so a variable that Node.js code sets later shows up.
         *
         * @since 1.0.0
         */
        public val SYSTEM: Environment = SystemEnvironment

        /**
         * An environment without any variable.
         *
         * @since 1.0.0
         */
        public val EMPTY: Environment = EmptyEnvironment
    }
}
