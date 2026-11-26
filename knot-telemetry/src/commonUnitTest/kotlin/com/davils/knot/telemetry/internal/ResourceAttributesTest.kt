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

package com.davils.knot.telemetry.internal

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotContain
import io.kotest.property.Arb
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll

private const val SECRET: String = "hunter2"

private const val BYTE_MASK: Int = 0xFF
private const val HEX: Int = 16

private fun percentEncoded(text: String): String {
    val bytes = text.encodeToByteArray()
    return bytes.joinToString(separator = "") { byte -> "%" + hexOf(byte) }
}

private fun hexOf(byte: Byte): String {
    val digits = (byte.toInt() and BYTE_MASK).toString(HEX)
    return digits.padStart(length = 2, padChar = '0')
}

class ResourceAttributesTest : FunSpec({
    context("a list of resource attributes") {
        test("reads key value pairs, trims them and skips empty entries") {
            resourceAttributesOf(" a = 1 ,, b=two words ,") shouldBe listOf("a" to "1", "b" to "two words")
        }

        test("keeps an equals sign inside a value") {
            resourceAttributesOf("query=a=b") shouldBe listOf("query" to "a=b")
        }

        test("decodes percent-encoded UTF-8") {
            resourceAttributesOf("city=K%C3%B6ln,emoji=%F0%9F%98%80,comma=a%2Cb") shouldBe listOf(
                "city" to "Köln",
                "emoji" to "😀",
                "comma" to "a,b",
            )
        }

        test("reads back every text it was given percent-encoded") {
            checkAll(Arb.string()) { text ->
                val isEncodable = runCatching { text.encodeToByteArray(throwOnInvalidSequence = true) }.isSuccess
                if (isEncodable) resourceAttributesOf("key=${percentEncoded(text)}") shouldBe listOf("key" to text)
            }
        }

        test("refuses entries without a key or an assignment, never repeating the value") {
            listOf(SECRET, "=$SECRET", " =$SECRET").forEach { text ->
                val failure = shouldThrow<IllegalArgumentException> { resourceAttributesOf(text) }
                failure.message.orEmpty() shouldNotContain SECRET
            }
        }

        test("reads escapes in either case") {
            resourceAttributesOf("k=%2f%2F%7e%7E%e2%82%AC") shouldBe listOf("k" to "//~~\u20AC")
        }

        test("refuses broken escapes and bytes that are no UTF-8") {
            val broken = listOf("k=%G1", "k=%g1", "k=%:1", "k=%`1", "k=%/1", "k=%@1", "k=%4", "k=%", "k=%FF", "k=%C3")
            (broken + "k=\uD800").forEach { text ->
                shouldThrow<IllegalArgumentException> { resourceAttributesOf(text) }
            }
        }
    }
})
