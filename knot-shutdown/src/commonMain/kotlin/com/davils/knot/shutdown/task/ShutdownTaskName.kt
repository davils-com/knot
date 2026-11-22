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

package com.davils.knot.shutdown.task

import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.text.SafeText
import kotlin.jvm.JvmInline

/**
 * The name of a task of a shutdown, unique within it.
 *
 * A valid name starts with an ASCII letter or digit, holds only letters, digits, `.`, `_`, `:` and `-` and is at most
 * [ShutdownLimits.MAX_NAME_LENGTH][com.davils.knot.shutdown.ShutdownLimits.MAX_NAME_LENGTH] long; the declaration of
 * a task checks it.
 *
 * @property value the name.
 * @since 1.0.0
 */
@JvmInline
public value class ShutdownTaskName(public val value: String) {
    /**
     * Returns the name, escaped.
     *
     * @return the name.
     * @since 1.0.0
     */
    override fun toString(): String = SafeText.text(value)
}
