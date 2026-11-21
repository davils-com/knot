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

private const val NAME: String = "HOME"

class ChangingEnvironment : Environment {
    var value: String? = null

    override val names: Set<String>
        get() {
            if (value == null) return emptySet()
            return setOf(NAME)
        }

    override fun variable(name: String): String? = value.takeIf { name == NAME }
}
