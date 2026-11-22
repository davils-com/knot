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

package com.davils.knot.failure.render

import com.davils.knot.failure.FailureLimits
import com.davils.knot.failure.internal.qualifiedTypeOf
import com.davils.knot.failure.internal.stackLinesOf

internal fun causeChainOf(failure: Throwable, options: RenderOptions): List<CauseEntry> {
    val chain = mutableListOf<Throwable>()
    var current: Throwable? = failure
    while (current != null && chain.size <= options.maxCauseDepth && chain.none { seen -> seen === current }) {
        chain += current
        current = current.cause
    }
    return chain.map { cause -> entryOf(cause, options) }
}

private fun entryOf(cause: Throwable, options: RenderOptions): CauseEntry {
    val message = cause.message?.takeIf { options.hasMessages }
    val lines = stackLinesOf(cause).take(FailureLimits.MAX_FRAMES)
    val frames = lines.takeIf { options.hasStackTrace }.orEmpty()
    return CauseEntry(type = qualifiedTypeOf(cause), message = message, frames = frames)
}
