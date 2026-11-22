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

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class OperatingSystemWasmTest : FunSpec({
    context("current system on wasmJs") {
        test("is wasm, even under Node.js") {
            OperatingSystem.current shouldBe OperatingSystem.Wasm
        }

        test("finds the native platform of the machine behind Node.js") {
            Platform.current shouldBe Host.current.platform
            Host.current.systemSource shouldBe DetectionSource.Process
        }
    }

    context("current host on wasmJs") {
        test("is Node.js with its version, the runtime the tests run in") {
            Host.current.runtime shouldBe HostRuntime.NodeJs
            Host.current.runtimeVersion shouldNotBe null
            Host.current.isMobile shouldBe false
        }

        test("knows the system and the architecture from the process") {
            Host.current.system shouldNotBe HostSystem.Unknown
            Host.current.architectureSource shouldBe DetectionSource.Process
        }
    }
})
