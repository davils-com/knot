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

package com.davils.knot.environment

import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.text.SafeText
import com.davils.kore.core.validation.violation.ViolationCode

/**
 * A read of a variable failed: its value cannot be converted or expanded.
 *
 * The message names the variable, escaped, and the reason, and never the value or a part of it: values of environment
 * variables are secrets. The exception carries no cause, because the message of a cause may repeat the value.
 *
 * @property code the reason, one of [EnvironmentViolationCodes].
 * @property variable the name of the variable that was read.
 * @since 1.0.0
 */
public class EnvironmentException internal constructor(
    public val code: ViolationCode,
    public val variable: String,
    reason: String,
) : IllegalStateException("The variable ${SafeText.quoted(variable)} $reason")
