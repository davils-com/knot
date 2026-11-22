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

import com.davils.knot.failure.sink.SinkResult
import com.davils.konfetti.core.Event
import com.davils.konfetti.core.TopicKey

/**
 * What a failure handler tells, on the topic [TOPIC] of a konfetti event bus.
 *
 * @property failure the failure the event is about.
 * @since 1.0.0
 */
public sealed class FailureEvent(public val failure: UnexpectedFailure) : Event {
    /**
     * A failure was reported.
     *
     * @since 1.0.0
     */
    public class Occurred internal constructor(failure: UnexpectedFailure) : FailureEvent(failure) {
        /**
         * Returns the event.
         *
         * @return the event.
         * @since 1.0.0
         */
        override fun toString(): String = "Occurred($failure)"
    }

    /**
     * A sink took the report of a failure, or failed to.
     *
     * @property result how writing to the sink ended.
     * @since 1.0.0
     */
    public class Written internal constructor(failure: UnexpectedFailure, public val result: SinkResult) :
        FailureEvent(failure) {
        /**
         * Returns the event.
         *
         * @return the event.
         * @since 1.0.0
         */
        override fun toString(): String = "Written(${failure.id}, $result)"
    }

    /**
     * The topic of the events of a failure handler.
     *
     * @since 1.0.0
     */
    public companion object {
        /**
         * The key of the topic `knot.failure`, which a handler declares on its event bus if it is not declared yet.
         *
         * @since 1.0.0
         */
        public val TOPIC: TopicKey<FailureEvent> = TopicKey("knot.failure")
    }
}
