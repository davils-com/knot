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

import com.davils.kore.core.validation.violation.ViolationCode

/**
 * The codes that the validation of a shutdown, a registration and a process installation report.
 *
 * Every code is a stable name in the `knot.shutdown` namespace.
 *
 * @since 1.0.0
 */
public object ShutdownViolationCodes {
    /**
     * The name of the shutdown breaks the rules of a name, see [ShutdownLimits.MAX_NAME_LENGTH].
     *
     * @since 1.0.0
     */
    public val INVALID_NAME: ViolationCode = ViolationCode("knot.shutdown.invalid-name")

    /**
     * The name of a task breaks the rules of a name, see [ShutdownLimits.MAX_NAME_LENGTH].
     *
     * @since 1.0.0
     */
    public val INVALID_TASK_NAME: ViolationCode = ViolationCode("knot.shutdown.invalid-task-name")

    /**
     * A task declares no action.
     *
     * @since 1.0.0
     */
    public val MISSING_ACTION: ViolationCode = ViolationCode("knot.shutdown.missing-action")

    /**
     * A task waits on a task that is not declared.
     *
     * @since 1.0.0
     */
    public val UNKNOWN_DEPENDENCY: ViolationCode = ViolationCode("knot.shutdown.unknown-dependency")

    /**
     * A task waits on a task of a later phase, which would never end before it.
     *
     * @since 1.0.0
     */
    public val LATER_PHASE_DEPENDENCY: ViolationCode = ViolationCode("knot.shutdown.later-phase-dependency")

    /**
     * A task is part of a cycle of dependencies, or waits on one.
     *
     * @since 1.0.0
     */
    public val DEPENDENCY_CYCLE: ViolationCode = ViolationCode("knot.shutdown.dependency-cycle")

    /**
     * A task is declared or registered under a name another task has.
     *
     * @since 1.0.0
     */
    public val DUPLICATE_TASK: ViolationCode = ViolationCode("knot.shutdown.duplicate-task")

    /**
     * A timeout is not above zero or not finite.
     *
     * @since 1.0.0
     */
    public val INVALID_TIMEOUT: ViolationCode = ViolationCode("knot.shutdown.invalid-timeout")

    /**
     * The grace period is not above zero or longer than [ShutdownLimits.MAX_GRACE_PERIOD].
     *
     * @since 1.0.0
     */
    public val INVALID_GRACE_PERIOD: ViolationCode = ViolationCode("knot.shutdown.invalid-grace-period")

    /**
     * The cancellation grace is not above zero or not finite.
     *
     * @since 1.0.0
     */
    public val INVALID_CANCELLATION_GRACE: ViolationCode = ViolationCode("knot.shutdown.invalid-cancellation-grace")

    /**
     * The capacity of the event flow lies outside 1 to [ShutdownLimits.MAX_EVENT_CAPACITY].
     *
     * @since 1.0.0
     */
    public val INVALID_EVENT_CAPACITY: ViolationCode = ViolationCode("knot.shutdown.invalid-event-capacity")

    /**
     * A shutdown would hold more than [ShutdownLimits.MAX_TASKS] tasks.
     *
     * @since 1.0.0
     */
    public val TOO_MANY_TASKS: ViolationCode = ViolationCode("knot.shutdown.too-many-tasks")

    /**
     * A task waits on more than [ShutdownLimits.MAX_DEPENDENCIES] tasks.
     *
     * @since 1.0.0
     */
    public val TOO_MANY_DEPENDENCIES: ViolationCode = ViolationCode("knot.shutdown.too-many-dependencies")

    /**
     * A registration declares no task.
     *
     * @since 1.0.0
     */
    public val EMPTY_REGISTRATION: ViolationCode = ViolationCode("knot.shutdown.empty-registration")

    /**
     * The escalation of a process installation halts after a span that is not above zero or not finite.
     *
     * @since 1.0.0
     */
    public val INVALID_ESCALATION: ViolationCode = ViolationCode("knot.shutdown.invalid-escalation")

    /**
     * A warning: a process installation installs no trigger, so the shutdown only runs when it is initiated.
     *
     * @since 1.0.0
     */
    public val NO_TRIGGER: ViolationCode = ViolationCode("knot.shutdown.no-trigger")

    /**
     * A warning: a timeout is longer than the grace period, so the task is cut short.
     *
     * @since 1.0.0
     */
    public val TIMEOUT_EXCEEDS_GRACE: ViolationCode = ViolationCode("knot.shutdown.timeout-exceeds-grace")
}
