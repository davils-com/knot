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

package com.davils.knot.failure.fixture

import com.davils.knot.failure.FailureHandler
import com.davils.knot.failure.FailureHandlerBuilder
import com.davils.knot.failure.FailureOrigin
import com.davils.knot.failure.failures
import com.davils.kore.core.identity.generator.UuidV7Generator
import kotlinx.coroutines.CoroutineScope

const val BROKEN: String = "broken"

val ORIGIN: FailureOrigin = FailureOrigin.Job("nightly")

fun CoroutineScope.testHandler(
    clock: MovableClock = MovableClock(),
    block: FailureHandlerBuilder.() -> Unit = {
    },
): FailureHandler = failures(this) {
    this.clock = clock
    ids = UuidV7Generator
    block()
}

fun failure(message: String = BROKEN): Throwable = IllegalStateException(message)
