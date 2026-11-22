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

import com.davils.knot.failure.internal.isPlatformFatal
import com.davils.kore.core.annotation.DavilsDsl
import kotlin.coroutines.cancellation.CancellationException

/**
 * Declares which failures are fatal and which are ignored, for
 * [FailureHandlerBuilder.classify][com.davils.knot.failure.FailureHandlerBuilder.classify].
 *
 * A failure is fatal when it, or a cause within [FailureLimits.MAX_CAUSE_DEPTH][com.davils.knot.failure.FailureLimits]
 * of it, matches a fatal rule; by default that is a `VirtualMachineError` on the JVM, such as an `OutOfMemoryError`.
 * A failure is ignored when it matches an ignore rule itself; by default that is a `CancellationException`. Fatal wins
 * over ignored. Declaring a rule adds it to the defaults.
 *
 * @since 1.0.0
 */
@DavilsDsl
public class FailureClassificationBuilder internal constructor() {
    private val fatalRules: MutableList<(Throwable) -> Boolean> = mutableListOf(::isPlatformFatal)
    private val ignoreRules: MutableList<(Throwable) -> Boolean> = mutableListOf(
        { cause -> cause is CancellationException },
    )

    /**
     * Makes a failure fatal when [matching] takes it or one of its causes.
     *
     * @param matching tells whether a failure is fatal; it is contained, and a failure of it counts as no match.
     * @since 1.0.0
     */
    public fun fatal(matching: (Throwable) -> Boolean) {
        fatalRules += matching
    }

    /**
     * Makes a failure fatal when it or one of its causes is a [T].
     *
     * @param T the type of the fatal failures.
     * @since 1.0.0
     */
    public inline fun <reified T : Throwable> fatal() {
        fatal { cause -> cause is T }
    }

    /**
     * Ignores a failure that [matching] takes; an ignored failure is counted and nothing else.
     *
     * @param matching tells whether a failure is ignored; it is contained, and a failure of it counts as no match.
     * @since 1.0.0
     */
    public fun ignore(matching: (Throwable) -> Boolean) {
        ignoreRules += matching
    }

    /**
     * Ignores a failure that is a [T].
     *
     * @param T the type of the ignored failures.
     * @since 1.0.0
     */
    public inline fun <reified T : Throwable> ignore() {
        ignore { cause -> cause is T }
    }

    internal fun build(): FailureClassification = FailureClassification(
        fatalRules = fatalRules.toList(),
        ignoreRules = ignoreRules.toList(),
    )
}
