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

package com.davils.knot.failure.classify

import com.davils.knot.failure.FailureLimits
import com.davils.knot.failure.FailureSeverity
import com.davils.knot.failure.fixture.BROKEN
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlin.coroutines.cancellation.CancellationException

private fun classification(block: FailureClassificationBuilder.() -> Unit = {}): FailureClassification {
    val builder = FailureClassificationBuilder()
    builder.block()
    return builder.build()
}

private fun chain(depth: Int, root: Throwable): Throwable =
    (1 until depth).fold(root) { cause, _ -> IllegalStateException(BROKEN, cause) }

class FailureClassificationTest : FunSpec({
    test("takes a failure as recoverable and a cancellation as ignored by default") {
        val defaults = classification()

        defaults.severityOf(IllegalStateException(BROKEN)) shouldBe FailureSeverity.Recoverable
        defaults.severityOf(CancellationException(BROKEN)) shouldBe null
        defaults.toString() shouldBe "FailureClassification(fatal=1, ignore=1)"
    }

    test("finds a fatal failure among the causes, within the limit") {
        val rules = classification { fatal<UnsupportedOperationException>() }

        rules.isFatal(IllegalStateException(BROKEN, UnsupportedOperationException(BROKEN))) shouldBe true
        rules.isFatal(chain(FailureLimits.MAX_CAUSE_DEPTH, UnsupportedOperationException(BROKEN))) shouldBe true
        rules.isFatal(chain(FailureLimits.MAX_CAUSE_DEPTH + 1, UnsupportedOperationException(BROKEN))) shouldBe false
    }

    test("lets fatal win over ignored") {
        val rules = classification {
            fatal { cause -> cause.message == BROKEN }
            ignore<IllegalStateException>()
        }

        rules.severityOf(IllegalStateException(BROKEN)) shouldBe FailureSeverity.Fatal
        rules.severityOf(IllegalStateException("other")) shouldBe null
    }

    test("takes a rule that throws as no match") {
        val rules = classification {
            fatal { error(BROKEN) }
            ignore { error(BROKEN) }
        }

        rules.severityOf(IllegalArgumentException(BROKEN)) shouldBe FailureSeverity.Recoverable
    }
})
