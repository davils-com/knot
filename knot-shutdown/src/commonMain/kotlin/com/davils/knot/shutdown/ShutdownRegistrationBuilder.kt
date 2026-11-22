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

import com.davils.knot.shutdown.internal.GraphNode
import com.davils.kore.core.annotation.DavilsDsl

private const val SUBJECT: String = "shutdown registration"

/**
 * Declares the tasks of one registration, for [Shutdown.register].
 *
 * @since 1.0.0
 */
@DavilsDsl
public class ShutdownRegistrationBuilder internal constructor(
    internal val registered: () -> Collection<GraphNode>,
) : ShutdownPlanBuilder<ShutdownPlan>() {
    override val subject: String get() = SUBJECT

    override fun assemble(): ShutdownPlan = ShutdownPlan(this)
}
