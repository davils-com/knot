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

@file:OptIn(DavilsInternal::class)

package com.davils.knot.failure

import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.text.SafeText

/**
 * Where an unexpected failure happened.
 *
 * Each origin has a [kind], a stable tag such as `thread`, which meters and events carry, and a [subject], such as the
 * name of the thread, which may come from input and is shown escaped. Two origins are equal when their kind and
 * subject are.
 *
 * @property kind the stable tag of the origin.
 * @property subject what happened, such as the name of a thread.
 * @since 1.0.0
 */
public sealed class FailureOrigin(public val kind: String, public val subject: String) {
    /**
     * Tells whether [other] is an origin of the same kind and subject.
     *
     * @param other the object to compare with.
     * @return `true` if [other] is equal.
     * @since 1.0.0
     */
    final override fun equals(other: Any?): Boolean =
        other is FailureOrigin && kind == other.kind && subject == other.subject

    /**
     * Returns a hash code consistent with [equals].
     *
     * @return the hash code.
     * @since 1.0.0
     */
    final override fun hashCode(): Int = listOf(kind, subject).hashCode()

    /**
     * Returns the kind and the subject, escaped.
     *
     * @return the origin, such as `thread "main"`.
     * @since 1.0.0
     */
    final override fun toString(): String = "$kind ${SafeText.quoted(subject)}"

    /**
     * A thread that no code handled the failure of.
     *
     * @property name the name of the thread.
     * @since 1.0.0
     */
    public class Thread(public val name: String) : FailureOrigin(kind = "thread", subject = name)

    /**
     * A coroutine whose failure reached a [CoroutineExceptionHandler][kotlinx.coroutines.CoroutineExceptionHandler].
     *
     * @property name the name of the coroutine.
     * @since 1.0.0
     */
    public class Coroutine(public val name: String) : FailureOrigin(kind = "coroutine", subject = name)

    /**
     * A job of the application, such as a scheduled run.
     *
     * @property id the id of the job.
     * @since 1.0.0
     */
    public class Job(public val id: String) : FailureOrigin(kind = "job", subject = id)

    /**
     * An action of a user, such as a request or a command.
     *
     * @property id the id of the action.
     * @since 1.0.0
     */
    public class Action(public val id: String) : FailureOrigin(kind = "action", subject = id)

    /**
     * The start of the application.
     *
     * @property phase the phase of the start.
     * @since 1.0.0
     */
    public class Startup(public val phase: String) : FailureOrigin(kind = "startup", subject = phase)

    /**
     * The shutdown of the application.
     *
     * @property task the task of the shutdown.
     * @since 1.0.0
     */
    public class Shutdown(public val task: String) : FailureOrigin(kind = "shutdown", subject = task)

    /**
     * A component of the application of any other kind.
     *
     * @param kind the stable tag of the component, such as `plugin` or `window`.
     * @property name the name of the component.
     * @since 1.0.0
     */
    public class Component(kind: String, public val name: String) : FailureOrigin(kind = kind, subject = name)
}
