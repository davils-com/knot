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

package com.davils.knot.shutdown

import com.davils.kore.core.validation.report.ValidationReport

/**
 * Why a shutdown refused a registration.
 *
 * @since 1.0.0
 */
public sealed interface RegistrationRefusal {
    /**
     * The shutdown has started; it takes no task any more.
     *
     * @since 1.0.0
     */
    public object ShuttingDown : RegistrationRefusal {
        /**
         * Returns the name of the refusal.
         *
         * @return `ShuttingDown`.
         * @since 1.0.0
         */
        override fun toString(): String = "ShuttingDown"
    }

    /**
     * The registration breaks a rule.
     *
     * @property report every problem, with the codes of [ShutdownViolationCodes].
     * @since 1.0.0
     */
    public class Invalid internal constructor(public val report: ValidationReport) : RegistrationRefusal {
        /**
         * Returns the number of problems.
         *
         * @return the refusal, such as `Invalid(2 violations)`.
         * @since 1.0.0
         */
        override fun toString(): String = "Invalid(${report.violations.size} violations)"
    }
}
