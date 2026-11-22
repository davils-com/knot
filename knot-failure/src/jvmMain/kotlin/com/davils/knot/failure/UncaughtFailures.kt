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

package com.davils.knot.failure

import com.davils.knot.failure.internal.ThreadInstallation
import com.davils.knot.shutdown.process.ProcessInstallation
import com.davils.knot.shutdown.process.ShutdownRuntime

/**
 * Hands every exception that no thread handled to this handler, with the origin [FailureOrigin.Thread], and then to
 * the handler of uncaught exceptions that was installed before.
 *
 * The handler of [runtime] is replaced until the returned handle is closed or the failure handler closes; then the
 * previous one comes back, unless another handler was installed meanwhile. A process installation of
 * `knot-shutdown` with `FatalErrorPolicy.Exit` installed before would end the process before the report is written;
 * let the failure handler end it with [exitThrough] instead.
 *
 * @param runtime the JVM to install into; [ShutdownRuntime.Jvm] by default.
 * @return the handle that uninstalls the handler again.
 * @since 1.0.0
 */
public fun FailureHandler.installUncaughtHandler(runtime: ShutdownRuntime = ShutdownRuntime.Jvm): AutoCloseable {
    val installation = ThreadInstallation(runtime) { thread, cause -> submit(cause, FailureOrigin.Thread(thread.name)) }
    val handle = installation.install()
    whenClosed(handle)
    return handle
}

/**
 * Lets a fatal failure end the process through [installation] once its report was written: the shutdown runs with
 * [ShutdownReason.FatalError][com.davils.knot.shutdown.ShutdownReason.FatalError] and the process ends with the exit
 * code of its report. It replaces [FailureHandlerBuilder.shutdown].
 *
 * @param installation the process installation of the shutdown.
 * @since 1.0.0
 */
public fun FailureHandlerBuilder.exitThrough(installation: ProcessInstallation) {
    fatalAction = { reason -> installation.exit(reason) }
}
