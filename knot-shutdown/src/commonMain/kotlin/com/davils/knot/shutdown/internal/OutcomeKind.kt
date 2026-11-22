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

package com.davils.knot.shutdown.internal

import com.davils.knot.shutdown.report.TaskOutcome

internal enum class OutcomeKind(val label: String) {
    Completed("completed"),
    Failed("failed"),
    TimedOut("timed_out"),
    Abandoned("abandoned"),
    DeadlineExceeded("deadline_exceeded"),
    ;

    companion object {
        fun of(outcome: TaskOutcome): OutcomeKind = when (outcome) {
            is TaskOutcome.Completed -> Completed
            is TaskOutcome.Failed -> Failed
            is TaskOutcome.TimedOut -> TimedOut
            is TaskOutcome.Abandoned -> Abandoned
            TaskOutcome.DeadlineExceeded -> DeadlineExceeded
        }
    }
}
