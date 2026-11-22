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

import com.davils.kore.core.annotation.DavilsDsl
import com.davils.kore.core.dsl.ValidatingBuilder

private const val SUBJECT: String = "process installation"

/**
 * Declares how a shutdown is bound to the JVM, for [install].
 *
 * @since 1.0.0
 */
@DavilsDsl
public class ProcessInstallationBuilder internal constructor() : ValidatingBuilder<ProcessInstallationConfiguration>() {
    override val subject: String get() = SUBJECT

    /**
     * Whether a shutdown hook runs the shutdown when the JVM shuts down, such as on `System.exit`, at the end of
     * `main` or on `SIGTERM`; `true` by default. The process then ends with the status the JVM chose.
     *
     * @since 1.0.0
     */
    public var hasExitHook: Boolean = true

    /**
     * What an exception that no thread handled does; [FatalErrorPolicy.Ignore] by default.
     *
     * @since 1.0.0
     */
    public var fatalErrors: FatalErrorPolicy = FatalErrorPolicy.Ignore

    /**
     * What happens when the shutdown overruns its grace period; [Escalation.Halt] by default.
     *
     * @since 1.0.0
     */
    public var escalation: Escalation = Escalation.Halt()

    override fun assemble(): ProcessInstallationConfiguration = ProcessInstallationConfiguration(this)
}
