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

package com.davils.knot.shutdown.task

import com.davils.knot.shutdown.ShutdownPhase
import com.davils.kore.core.annotation.DavilsDsl
import kotlin.time.Duration

/**
 * Declares the tasks of one phase of a shutdown, for
 * [ShutdownPlanBuilder.phase][com.davils.knot.shutdown.ShutdownPlanBuilder.phase].
 *
 * @property phase the phase of the tasks.
 * @since 1.0.0
 */
@DavilsDsl
public class PhaseBuilder internal constructor(
    public val phase: ShutdownPhase,
    private val declare: (ShutdownPhase, String, ShutdownTaskBuilder.() -> Unit) -> Unit,
) {
    /**
     * Declares the task [name] of this phase.
     *
     * @param name the name of the task, unique within the shutdown.
     * @param block declares the action, the timeout and the dependencies.
     * @since 1.0.0
     */
    public fun task(name: String, block: ShutdownTaskBuilder.() -> Unit) {
        declare(phase, name, block)
    }

    /**
     * Declares the task [name] that closes [resource]. On the JVM the call to `close` can be interrupted when its
     * budget is over.
     *
     * @param name the name of the task, unique within the shutdown.
     * @param resource the resource to close.
     * @param timeout how long closing may take, or `null` for the task timeout of the shutdown.
     * @since 1.0.0
     */
    public fun closing(name: String, resource: AutoCloseable, timeout: Duration? = null) {
        task(name) {
            this.timeout = timeout
            action { closeResource(resource) }
        }
    }
}
