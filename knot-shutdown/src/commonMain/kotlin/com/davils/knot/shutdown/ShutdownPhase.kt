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

/**
 * A phase of a shutdown; phases run one after another in the order of their declaration.
 *
 * @since 1.0.0
 */
public enum class ShutdownPhase {
    /**
     * Tells the outside world that the application stops, such as failing its readiness probe.
     *
     * @since 1.0.0
     */
    Announce,

    /**
     * Lets running work finish and takes no new work, such as draining queues and requests.
     *
     * @since 1.0.0
     */
    Drain,

    /**
     * Stops the components of the application, such as servers, schedulers and consumers.
     *
     * @since 1.0.0
     */
    Stop,

    /**
     * Releases resources, such as connections, pools and files.
     *
     * @since 1.0.0
     */
    Release,

    /**
     * Flushes what must leave the process last, such as metrics and traces.
     *
     * @since 1.0.0
     */
    Flush,
}
