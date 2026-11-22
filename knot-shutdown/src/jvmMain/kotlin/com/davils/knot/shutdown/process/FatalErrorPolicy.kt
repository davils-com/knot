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

package com.davils.knot.shutdown.process

/**
 * What a [ProcessInstallation] does with an exception that no thread handled.
 *
 * @since 1.0.0
 */
public sealed interface FatalErrorPolicy {
    /**
     * Leaves the exception to the handler that was installed before; the shutdown does not start. The default.
     *
     * @since 1.0.0
     */
    public object Ignore : FatalErrorPolicy {
        /**
         * Returns the name of the policy.
         *
         * @return `Ignore`.
         * @since 1.0.0
         */
        override fun toString(): String = "Ignore"
    }

    /**
     * Runs the shutdown and ends the process with the fatal exit code when [matching] takes the exception, after the
     * handler that was installed before has seen it.
     *
     * @property matching tells whether an exception is fatal; a `VirtualMachineError`, such as an
     * `OutOfMemoryError`, by default.
     * @since 1.0.0
     */
    public class Exit(public val matching: (Throwable) -> Boolean = { cause -> cause is VirtualMachineError }) :
        FatalErrorPolicy {
        /**
         * Returns the name of the policy.
         *
         * @return `Exit`.
         * @since 1.0.0
         */
        override fun toString(): String = "Exit"
    }
}
