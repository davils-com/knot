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

class OperatingSystemJvmTest : FunSpec({
    context("current system on the JVM") {
        test("is the system the JVM reports and never wasm") {
            OperatingSystem.current shouldBe OperatingSystem.resolve(System.getProperty("os.name"))
            OperatingSystem.current?.isWasm shouldBe false
        }
    }

    context("current host on the JVM") {
        test("is the JVM with the version and the machine it reports") {
            val host = Host.current

            host.runtime shouldBe HostRuntime.Jvm
            host.runtimeVersion shouldBe System.getProperty("java.version")
            host.systemSource shouldBe DetectionSource.SystemProperty
            host.platform shouldBe Platform.resolve(
                osName = System.getProperty("os.name"),
                osArch = System.getProperty("os.arch"),
            )
        }

        test("has the id of this process") {
            Host.current.processId shouldBe ProcessHandle.current().pid()
        }
    }
})
