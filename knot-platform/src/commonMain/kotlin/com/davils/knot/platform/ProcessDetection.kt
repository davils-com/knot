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

package com.davils.knot.platform

private const val MAX_LABEL_LENGTH: Int = 63
private const val LABEL_SEPARATOR: Char = '.'
private const val HYPHEN: Char = '-'

internal fun processIdOf(probe: HostProbe, runtime: HostRuntime): Long? =
    probe.processId?.takeIf { id -> runtime.hasProcess && id > 0 }

internal fun hostNameOf(probe: HostProbe, runtime: HostRuntime): String? {
    if (!runtime.hasProcess) return null
    val reported = listOfNotNull(probe.hostNameVariable, probe.computerNameVariable)
    return reported.map(String::trim).firstOrNull(::isHostName)
}

private fun isHostName(name: String): Boolean {
    val isBounded = name.isNotEmpty() && name.length <= PlatformLimits.MAX_HOST_NAME_LENGTH
    return isBounded && name.split(LABEL_SEPARATOR).all(::isLabel)
}

private fun isLabel(label: String): Boolean {
    val isBounded = label.isNotEmpty() && label.length <= MAX_LABEL_LENGTH
    val hasInnerHyphensOnly = label.firstOrNull() != HYPHEN && label.lastOrNull() != HYPHEN
    return isBounded && hasInnerHyphensOnly && label.all(::isLabelCharacter)
}

private fun isLabelCharacter(character: Char): Boolean =
    character in 'a'..'z' || character in 'A'..'Z' || character in '0'..'9' || character == HYPHEN
