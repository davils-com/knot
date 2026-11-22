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

private const val NEWLINE: Char = '\n'
private const val RETURN: String = "\r"
private const val NO_LINE: Int = 0
private const val TOO_LONG_REASON: String = "the text is longer than the limits of a dotenv text"

internal object Dotenv {
    fun parse(text: String): DotenvResult {
        val lines = text.split(NEWLINE)
        val isTooLong = text.length > EnvironmentLimits.MAX_DOTENV_LENGTH ||
            lines.size > EnvironmentLimits.MAX_DOTENV_LINES
        if (isTooLong) {
            val problem = DotenvProblem(line = NO_LINE, reason = TOO_LONG_REASON, isTooLong = true)
            return DotenvResult(variables = emptyMap(), problems = listOf(problem))
        }
        val reader = DotenvReader(lines.map { line -> line.removeSuffix(RETURN) })
        return reader.read()
    }
}
