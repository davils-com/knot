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

import kotlinx.coroutines.CoroutineScope

/**
 * Declares a shutdown that runs on [scope].
 *
 * ```kotlin
 * val shutdown = shutdown(applicationScope) {
 *     name = "orders"
 *     drain { task("http") { action { server.drain(remaining) } } }
 *     release {
 *         closing("database", dataSource)
 *         task("cache") {
 *             after("database")
 *             action { cache.flush() }
 *         }
 *     }
 * }
 * ```
 *
 * The scope must outlive the shutdown: the shutdown runs its tasks in children of it, so cancelling the scope cancels
 * a running shutdown. Creating a shutdown has no effect outside it; on the JVM,
 * [install][com.davils.knot.shutdown.process.install] binds it to the process.
 *
 * @param scope the scope the shutdown and its tasks run on.
 * @param block declares the shutdown.
 * @return the shutdown.
 * @throws com.davils.kore.core.validation.report.ValidationException if the declaration breaks a rule; the report lists
 * every problem with the codes of [ShutdownViolationCodes].
 * @since 1.0.0
 */
public fun shutdown(scope: CoroutineScope, block: ShutdownBuilder.() -> Unit = {}): Shutdown {
    val builder = ShutdownBuilder()
    builder.block()
    return Shutdown(builder.build(), scope)
}
