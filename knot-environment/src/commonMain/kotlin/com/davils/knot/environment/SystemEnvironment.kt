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

import com.davils.kore.core.Limits
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.annotation.DavilsPlatformBinding
import com.davils.kore.core.collection.immutableSet
import com.davils.kore.core.processVariable
import com.davils.kore.core.processVariableNames

private const val DISPLAY_NAME: String = "Environment.SYSTEM"
private const val NUL: Char = '\u0000'

@DavilsPlatformBinding
internal object SystemEnvironment : Environment {
    override val names: Set<String> get() = processVariableNames().immutableSet()

    override fun variable(name: String): String? {
        val isReadable = name.length in 1..Limits.MAX_NAME_LENGTH && NUL !in name
        if (!isReadable) return null
        return processVariable(name)
    }

    override fun toString(): String = DISPLAY_NAME
}
