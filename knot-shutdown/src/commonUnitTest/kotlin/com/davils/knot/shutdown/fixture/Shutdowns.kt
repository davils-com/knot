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

@file:OptIn(ExperimentalKotest::class)

package com.davils.knot.shutdown.fixture

import com.davils.knot.shutdown.Shutdown
import com.davils.knot.shutdown.ShutdownBuilder
import com.davils.knot.shutdown.shutdown
import com.davils.kore.testing.TestClock
import io.kotest.common.ExperimentalKotest
import io.kotest.core.test.TestScope
import io.kotest.core.test.testCoroutineScheduler
import kotlin.time.Instant

val STARTED_AT: Instant = Instant.parse("2026-11-22T12:00:00Z")

fun TestScope.virtualShutdown(block: ShutdownBuilder.() -> Unit = {}): Shutdown = shutdown(this) {
    timeSource = testCoroutineScheduler.timeSource
    clock = TestClock(STARTED_AT)
    block()
}
