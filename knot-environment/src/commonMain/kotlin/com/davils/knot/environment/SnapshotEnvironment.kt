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
import com.davils.kore.core.collection.immutableMap

private const val DISPLAY_NAME: String = "Environment.snapshot"

internal class SnapshotEnvironment(source: Environment) : Environment {
    private val variables: Map<String, String> = read(source)

    override val names: Set<String> get() = variables.keys

    override fun variable(name: String): String? = variables[name]

    override fun toString(): String = DISPLAY_NAME

    private fun read(source: Environment): Map<String, String> {
        val read = LinkedHashMap<String, String>()
        source.names.forEach { name -> source.variable(name)?.let { value -> read[name] = value } }
        return read.immutableMap()
    }
}
