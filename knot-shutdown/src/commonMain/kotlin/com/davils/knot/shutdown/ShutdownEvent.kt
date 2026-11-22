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

import com.davils.knot.shutdown.exit.ExitCode
import com.davils.knot.shutdown.report.PhaseReport
import com.davils.knot.shutdown.report.ShutdownReport
import com.davils.knot.shutdown.report.TaskReport
import com.davils.konfetti.core.Event
import com.davils.konfetti.core.TopicKey
import kotlin.time.Duration

/**
 * What a shutdown tells while it runs, on [Shutdown.events] and on the topic [TOPIC] of a konfetti event bus.
 *
 * Events carry names, phases, numbers, durations and reports; the `toString` of each shows the type of a failure,
 * never its message.
 *
 * @property shutdown the name of the shutdown.
 * @since 1.0.0
 */
public sealed class ShutdownEvent(public val shutdown: String) : Event {
    /**
     * The shutdown started.
     *
     * @property reason why it runs.
     * @property tasks how many tasks it runs.
     * @since 1.0.0
     */
    public class Initiated internal constructor(
        shutdown: String,
        public val reason: ShutdownReason,
        public val tasks: Int,
    ) :
        ShutdownEvent(shutdown) {
        /**
         * Returns the event.
         *
         * @return the event.
         * @since 1.0.0
         */
        override fun toString(): String = "Initiated($shutdown, reason=$reason, tasks=$tasks)"
    }

    /**
     * The shutdown entered a phase.
     *
     * @property phase the phase.
     * @property tasks the names of its tasks.
     * @since 1.0.0
     */
    public class PhaseStarted internal constructor(
        shutdown: String,
        public val phase: ShutdownPhase,
        public val tasks: List<String>,
    ) : ShutdownEvent(shutdown) {
        /**
         * Returns the event.
         *
         * @return the event.
         * @since 1.0.0
         */
        override fun toString(): String = "PhaseStarted($shutdown, $phase, tasks=${tasks.size})"
    }

    /**
     * A task ended.
     *
     * @property task how it ended.
     * @since 1.0.0
     */
    public class TaskConcluded internal constructor(shutdown: String, public val task: TaskReport) : ShutdownEvent(
        shutdown,
    ) {
        /**
         * Returns the event.
         *
         * @return the event.
         * @since 1.0.0
         */
        override fun toString(): String = "TaskConcluded($shutdown, $task)"
    }

    /**
     * A phase ended.
     *
     * @property report how it ended.
     * @since 1.0.0
     */
    public class PhaseCompleted internal constructor(shutdown: String, public val report: PhaseReport) :
        ShutdownEvent(shutdown) {
        /**
         * Returns the event.
         *
         * @return the event.
         * @since 1.0.0
         */
        override fun toString(): String = "PhaseCompleted($shutdown, ${report.phase}, clean=${report.isClean})"
    }

    /**
     * The shutdown ended.
     *
     * @property report how it ended.
     * @since 1.0.0
     */
    public class Terminated internal constructor(shutdown: String, public val report: ShutdownReport) :
        ShutdownEvent(shutdown) {
        /**
         * Returns the event.
         *
         * @return the event.
         * @since 1.0.0
         */
        override fun toString(): String = "Terminated($shutdown, exitCode=${report.exitCode}, clean=${report.isClean})"
    }

    /**
     * The shutdown overran its budget and the watchdog of a JVM installation halts the process.
     *
     * @property exitCode the exit code of the halt.
     * @property after how long after the start of the shutdown the watchdog fired.
     * @since 1.0.0
     */
    public class Escalated internal constructor(
        shutdown: String,
        public val exitCode: ExitCode,
        public val after: Duration,
    ) :
        ShutdownEvent(shutdown) {
        /**
         * Returns the event.
         *
         * @return the event.
         * @since 1.0.0
         */
        override fun toString(): String = "Escalated($shutdown, exitCode=$exitCode, after=$after)"
    }

    /**
     * The topic of the events of a shutdown.
     *
     * @since 1.0.0
     */
    public companion object {
        /**
         * The key of the topic `knot.shutdown`, which a shutdown declares on its event bus if it is not declared yet.
         *
         * @since 1.0.0
         */
        public val TOPIC: TopicKey<ShutdownEvent> = TopicKey("knot.shutdown")
    }
}
