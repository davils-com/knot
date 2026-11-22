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

import com.davils.knot.shutdown.Shutdown

/**
 * Binds this shutdown to the JVM: a shutdown hook, a fatal error trigger and a watchdog, as [block] declares them.
 *
 * ```kotlin
 * val installation = shutdown.install {
 *     fatalErrors = FatalErrorPolicy.Exit()
 *     escalation = Escalation.Halt(after = 10.seconds)
 * }
 * installation.exit()
 * ```
 *
 * This is the only place where Knot touches the process: it adds a JVM shutdown hook, replaces the default handler of
 * uncaught exceptions for [FatalErrorPolicy.Exit], starts a timer thread for the watchdog once the shutdown starts and
 * ends the process on [ProcessInstallation.exit], on a fatal error or when the watchdog fires. There is no such
 * installation on the `wasmJs` target.
 *
 * @param runtime the JVM to bind to; [ShutdownRuntime.Jvm] by default.
 * @param block declares the triggers and the escalation.
 * @return the installation.
 * @throws com.davils.kore.core.validation.report.ValidationException if the declaration breaks a rule.
 * @since 1.0.0
 */
public fun Shutdown.install(
    runtime: ShutdownRuntime = ShutdownRuntime.Jvm,
    block: ProcessInstallationBuilder.() -> Unit = {},
): ProcessInstallation {
    val builder = ProcessInstallationBuilder()
    builder.block()
    return ProcessInstallation(shutdown = this, configuration = builder.build(), runtime = runtime)
}
