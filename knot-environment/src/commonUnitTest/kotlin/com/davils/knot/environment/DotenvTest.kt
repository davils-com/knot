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

import com.davils.kore.core.validation.report.ValidationException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotContain
import io.kotest.property.Arb
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll

private const val SECRET: String = "hunter2-secret"
private const val NEWLINE: String = "\n"
private const val NAME: String = "A"
private const val MAX_FRAGMENT: Int = 64
private const val LITERAL: String = """a \n ${'$'}{B} # not a comment"""
private const val ESCAPED: String = """line\nnext\t\"quoted\" \\ \${'$'}"""
private const val TAB: String = "\t"
private const val QUOTE: String = "\""
private const val BACKSLASH: String = "\\"
private const val UNESCAPED: String =
    "line" + NEWLINE + "next" + TAB + QUOTE + "quoted" + QUOTE + " " + BACKSLASH + " ${'$'}"

private fun dotenvOf(vararg lines: String): Environment = environment { dotenv(lines.joinToString(NEWLINE)) }

private fun problemsOf(vararg lines: String): List<String> = problemsOfText(lines.joinToString(NEWLINE))

private fun problemsOfText(text: String): List<String> {
    val failure = shouldThrow<ValidationException> { environment { dotenv(text) } }
    return failure.report.violations.map { violation -> "${violation.code.value} ${violation.message}" }
}

class DotenvTest : FunSpec({
    context("assignments") {
        test("read names and unquoted values, trimmed, with comments and blank lines skipped") {
            val parsed = dotenvOf(
                "# the database",
                "DB_HOST = localhost",
                "DB_PORT=5432 # the default port",
                "export DB_USER=knot",
                "",
                "EMPTY=",
                "URL=http://example.com/#anchor",
            )

            parsed.variable("DB_HOST") shouldBe "localhost"
            parsed.variable("DB_PORT") shouldBe "5432"
            parsed.variable("DB_USER") shouldBe "knot"
            parsed.variable("EMPTY") shouldBe ""
            parsed.variable("URL") shouldBe "http://example.com/#anchor"
        }

        test("keep the last value of a name assigned twice") {
            dotenvOf("A=1", "A=2").variable(NAME) shouldBe "2"
        }

        test("read Windows line endings") {
            dotenvOf("A=1\r", "B=2\r").variable(NAME) shouldBe "1"
        }
    }

    context("quotes") {
        test("take a single-quoted value literally") {
            dotenvOf("A='$LITERAL'").variable(NAME) shouldBe LITERAL
        }

        test("read the escapes of a double-quoted value") {
            dotenvOf("A=\"$ESCAPED\"").variable(NAME) shouldBe UNESCAPED
        }

        test("let a quoted value span lines and end in a comment") {
            dotenvOf(
                "KEY=\"first",
                "second\" # comment",
                "NEXT=1",
            ).variable("KEY") shouldBe "first" + NEWLINE + "second"
        }
    }

    context("broken lines") {
        test("are reported with their number and never their text") {
            problemsOf(
                "OK=1",
                "no assignment $SECRET",
                "=$SECRET",
                "SPACED NAME=$SECRET",
                "A=\"$SECRET\" trailing",
                """B="bad \q escape $SECRET"""",
            ) shouldContainExactly listOf(
                "knot.environment.invalid-dotenv line 2 holds no assignment NAME=value",
                "knot.environment.invalid-dotenv line 3 assigns no name",
                "knot.environment.invalid-dotenv line 4 has whitespace inside the name",
                "knot.environment.invalid-dotenv line 5 has text after the closing quote",
                "knot.environment.invalid-dotenv line 6 holds an unknown escape",
            )
        }

        test("report a quote that is never closed") {
            problemsOf("A='open", "B=2") shouldBe
                listOf("knot.environment.invalid-dotenv line 1 opens a quote that is never closed")
        }

        test("report a name the environment refuses under the layer") {
            val failure = shouldThrow<ValidationException> { dotenvOf("A${'\u0000'}=1") }

            failure.report.violations.map { violation -> violation.path.toString() } shouldBe
                listOf("layers[0].variables")
        }
    }

    context("limits") {
        test("refuse a text with too many lines without reading it") {
            val text = NEWLINE.repeat(EnvironmentLimits.MAX_DOTENV_LINES)

            problemsOfText(text) shouldBe
                listOf("knot.environment.too-long the text is longer than the limits of a dotenv text")
        }

        test("refuse a text that is too long without reading it") {
            val text = "A=" + "x".repeat(EnvironmentLimits.MAX_DOTENV_LENGTH)

            problemsOf(text).size shouldBe 1
        }
    }

    context("any text") {
        test("is read or refused and never shows a value in a problem") {
            checkAll(Arb.string(maxSize = MAX_FRAGMENT)) { fragment ->
                val message = runCatching { dotenvOf("A=$fragment$SECRET", fragment) }.exceptionOrNull()?.message

                message.orEmpty() shouldNotContain SECRET
            }
        }
    }
})
