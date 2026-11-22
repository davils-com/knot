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

package com.davils.knot.failure

import com.davils.knot.failure.internal.framesOf
import com.davils.knot.failure.internal.qualifiedTypeOf
import kotlin.jvm.JvmInline

private const val FRAMES: Int = 5
private const val SEPARATOR: String = "|"
private const val OFFSET_BASIS: ULong = 0xcbf29ce484222325uL
private const val PRIME: ULong = 0x100000001b3uL
private const val RADIX: Int = 16
private const val WIDTH: Int = 16

/**
 * What makes failures the same for the storm guard: a 64-bit FNV-1a hash, as 16 hex digits, of the type of the
 * failure and its top five stack frames. Messages are left out, so failures that differ only in their data share a
 * fingerprint.
 *
 * @property value the 16 hex digits.
 * @since 1.0.0
 */
@JvmInline
public value class FailureFingerprint(public val value: String) {
    /**
     * Returns the hex digits.
     *
     * @return the fingerprint.
     * @since 1.0.0
     */
    override fun toString(): String = value

    /**
     * Takes fingerprints.
     *
     * @since 1.0.0
     */
    public companion object {
        /**
         * Takes the fingerprint of [cause].
         *
         * On the JVM the frames are the class and method names of the stack trace; on the `wasmJs` target they are
         * the first frames of the text of the stack trace, whatever the runtime puts there.
         *
         * @param cause the failure.
         * @return its fingerprint.
         * @since 1.0.0
         */
        public fun of(cause: Throwable): FailureFingerprint {
            val parts = listOf(qualifiedTypeOf(cause)) + framesOf(cause).take(FRAMES)
            val bytes = parts.joinToString(SEPARATOR).encodeToByteArray()
            val hash = bytes.fold(OFFSET_BASIS) { current, byte -> (current xor byte.toUByte().toULong()) * PRIME }
            return FailureFingerprint(hash.toString(RADIX).padStart(WIDTH, '0'))
        }
    }
}
