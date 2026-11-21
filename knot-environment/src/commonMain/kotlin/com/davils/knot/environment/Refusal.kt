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

package com.davils.knot.environment

import com.davils.kore.core.Limits
import com.davils.kore.core.validation.violation.ViolationCode

private const val NUL: Char = '\u0000'
private const val ASSIGNMENT: Char = '='

internal enum class Refusal(
    val code: ViolationCode,
    val message: String,
    val isAboutName: Boolean,
    private val test: (String) -> Boolean,
) {
    NameTooLong(
        code = EnvironmentViolationCodes.TOO_LONG,
        message = "a name must not be longer than ${Limits.MAX_NAME_LENGTH} characters",
        isAboutName = true,
        test = { name -> name.length > Limits.MAX_NAME_LENGTH },
    ),
    BlankName(
        code = EnvironmentViolationCodes.INVALID_NAME,
        message = "a name must not be blank",
        isAboutName = true,
        test = { name -> name.isBlank() },
    ),
    ForbiddenNameCharacter(
        code = EnvironmentViolationCodes.INVALID_NAME,
        message = "a name must not contain '=' or the NUL character",
        isAboutName = true,
        test = { name -> ASSIGNMENT in name || NUL in name },
    ),
    ValueTooLong(
        code = EnvironmentViolationCodes.TOO_LONG,
        message = "must not be longer than ${Limits.MAX_ARGUMENT_LENGTH} characters",
        isAboutName = false,
        test = { value -> value.length > Limits.MAX_ARGUMENT_LENGTH },
    ),
    ForbiddenValueCharacter(
        code = EnvironmentViolationCodes.INVALID_VALUE,
        message = "must not contain the NUL character",
        isAboutName = false,
        test = { value -> NUL in value },
    ),
    ;

    fun refuses(text: String): Boolean = test(text)
}
