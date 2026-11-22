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

package com.davils.knot.failure.classify

import com.davils.knot.failure.FailureLimits
import com.davils.knot.failure.FailureSeverity
import com.davils.kore.core.failure.contained

/**
 * Decides how severe a failure is, as [FailureClassificationBuilder] declares it.
 *
 * @since 1.0.0
 */
public class FailureClassification internal constructor(
    private val fatalRules: List<(Throwable) -> Boolean>,
    private val ignoreRules: List<(Throwable) -> Boolean>,
) {
    /**
     * Tells whether [cause] or one of its causes is fatal. The chain is walked at most
     * [FailureLimits.MAX_CAUSE_DEPTH] deep and is safe against cycles.
     *
     * @param cause the failure.
     * @return `true` if it is fatal.
     * @since 1.0.0
     */
    public fun isFatal(cause: Throwable): Boolean {
        val chain = chainOf(cause)
        return chain.any { link -> fatalRules.any { rule -> matches(rule, link) } }
    }

    /**
     * Tells whether [cause] is ignored.
     *
     * @param cause the failure.
     * @return `true` if an ignore rule takes it.
     * @since 1.0.0
     */
    public fun isIgnored(cause: Throwable): Boolean = ignoreRules.any { rule -> matches(rule, cause) }

    /**
     * Returns how severe [cause] is: fatal wins over ignored.
     *
     * @param cause the failure.
     * @return the severity, or `null` if the failure is ignored.
     * @since 1.0.0
     */
    public fun severityOf(cause: Throwable): FailureSeverity? {
        if (isFatal(cause)) return FailureSeverity.Fatal
        return FailureSeverity.Recoverable.takeUnless { isIgnored(cause) }
    }

    /**
     * Returns the number of rules.
     *
     * @return the classification.
     * @since 1.0.0
     */
    override fun toString(): String = "FailureClassification(fatal=${fatalRules.size}, ignore=${ignoreRules.size})"

    private fun matches(rule: (Throwable) -> Boolean, cause: Throwable): Boolean {
        val matched = contained { rule(cause) }
        return matched.getOrDefault(false)
    }

    private fun chainOf(cause: Throwable): List<Throwable> {
        val chain = mutableListOf<Throwable>()
        var current: Throwable? = cause
        while (current != null && isNew(chain, current)) {
            chain += current
            current = current.cause
        }
        return chain
    }

    private fun isNew(chain: List<Throwable>, cause: Throwable): Boolean =
        chain.size < FailureLimits.MAX_CAUSE_DEPTH && chain.none { seen -> seen === cause }
}
