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

import com.davils.knot.shutdown.ShutdownViolationCodes
import com.davils.kore.core.validation.Validatable
import com.davils.kore.core.validation.ValidationScope

private const val ESCALATION_MESSAGE: String = "must halt after a span above zero and finite"
private const val NO_TRIGGER_MESSAGE: String = "installs no trigger, so the shutdown only runs when it is initiated"

/**
 * The declaration of a [ProcessInstallation], as [install] built and checked it.
 *
 * @since 1.0.0
 */
public class ProcessInstallationConfiguration internal constructor(builder: ProcessInstallationBuilder) : Validatable {
    /**
     * See [ProcessInstallationBuilder.hasExitHook].
     *
     * @since 1.0.0
     */
    public val hasExitHook: Boolean = builder.hasExitHook

    /**
     * See [ProcessInstallationBuilder.fatalErrors].
     *
     * @since 1.0.0
     */
    public val fatalErrors: FatalErrorPolicy = builder.fatalErrors

    /**
     * See [ProcessInstallationBuilder.escalation].
     *
     * @since 1.0.0
     */
    public val escalation: Escalation = builder.escalation

    /**
     * Checks the escalation and warns when nothing triggers the shutdown.
     *
     * @since 1.0.0
     */
    override fun ValidationScope.validate() {
        if (!isValidEscalation(
                escalation,
            )
        ) {
            fail(::escalation, ESCALATION_MESSAGE, ShutdownViolationCodes.INVALID_ESCALATION)
        }
        val hasTrigger = hasExitHook || fatalErrors is FatalErrorPolicy.Exit
        if (!hasTrigger) warn(NO_TRIGGER_MESSAGE, ShutdownViolationCodes.NO_TRIGGER)
    }

    /**
     * Returns the declaration.
     *
     * @return the configuration.
     * @since 1.0.0
     */
    override fun toString(): String =
        "ProcessInstallationConfiguration(hasExitHook=$hasExitHook, fatalErrors=$fatalErrors, escalation=$escalation)"
}

private fun isValidEscalation(escalation: Escalation): Boolean {
    val halt = escalation as? Escalation.Halt ?: return true
    return halt.after.isPositive() && halt.after.isFinite()
}
