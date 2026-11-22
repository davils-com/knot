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

@file:OptIn(DavilsInternal::class)

package com.davils.knot.shutdown

import com.davils.knot.shutdown.task.ShutdownTaskName
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.collection.immutableSet

/**
 * The answer of [Shutdown.register]: the tasks were registered, or the registration was refused.
 *
 * @since 1.0.0
 */
public sealed interface ShutdownRegistration {
    /**
     * Returns the registered tasks or throws why they were refused.
     *
     * @return the registration.
     * @throws com.davils.kore.core.validation.report.ValidationException if the registration was invalid.
     * @throws IllegalStateException if the shutdown has started.
     * @since 1.0.0
     */
    public fun getOrThrow(): Registered

    /**
     * The tasks were registered and run with the shutdown.
     *
     * @param tasks the names of the registered tasks.
     * @param release removes the tasks from the shutdown again.
     * @since 1.0.0
     */
    public class Registered internal constructor(
        tasks: Set<ShutdownTaskName>,
        private val release: (Set<ShutdownTaskName>) -> Boolean,
    ) : ShutdownRegistration {
        /**
         * The names of the registered tasks.
         *
         * @since 1.0.0
         */
        public val tasks: Set<ShutdownTaskName> = tasks.immutableSet()

        /**
         * Removes the tasks again, unless the shutdown has started, they were removed already or another task still
         * waits on one of them.
         *
         * @return `true` if the tasks were removed.
         * @since 1.0.0
         */
        public fun unregister(): Boolean = release(tasks)

        /**
         * Returns this registration.
         *
         * @return this registration.
         * @since 1.0.0
         */
        override fun getOrThrow(): Registered = this

        /**
         * Returns the registered tasks.
         *
         * @return the registration.
         * @since 1.0.0
         */
        override fun toString(): String = "Registered($tasks)"
    }

    /**
     * The registration was refused.
     *
     * @property refusal why.
     * @since 1.0.0
     */
    public class Refused internal constructor(public val refusal: RegistrationRefusal) : ShutdownRegistration {
        /**
         * Throws why the registration was refused.
         *
         * @return never.
         * @throws com.davils.kore.core.validation.report.ValidationException if the registration was invalid.
         * @throws IllegalStateException if the shutdown has started.
         * @since 1.0.0
         */
        override fun getOrThrow(): Registered {
            if (refusal is RegistrationRefusal.Invalid) refusal.report.throwIfInvalid()
            error("The shutdown refused the registration because it is $refusal")
        }

        /**
         * Returns the refusal.
         *
         * @return the registration.
         * @since 1.0.0
         */
        override fun toString(): String = "Refused($refusal)"
    }
}
