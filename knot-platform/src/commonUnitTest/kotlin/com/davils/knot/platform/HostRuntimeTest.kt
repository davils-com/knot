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
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe

class HostRuntimeTest : FunSpec({
    context("kinds of runtime") {
        test("runs every runtime but the JVM on WebAssembly") {
            HostRuntime.entries.filterNot { runtime -> runtime.isWasm } shouldBe listOf(HostRuntime.Jvm)
        }

        test("counts the three web workers as workers") {
            HostRuntime.entries.filter { runtime -> runtime.isWorker } shouldContainExactlyInAnyOrder listOf(
                HostRuntime.DedicatedWorker,
                HostRuntime.SharedWorker,
                HostRuntime.ServiceWorker,
            )
        }

        test("counts the browser window and the workers as the web") {
            HostRuntime.entries.filter { runtime -> runtime.isWeb } shouldContainExactlyInAnyOrder listOf(
                HostRuntime.Browser,
                HostRuntime.DedicatedWorker,
                HostRuntime.SharedWorker,
                HostRuntime.ServiceWorker,
            )
        }

        test("gives a process only to the JVM, Node.js, Deno and Bun") {
            HostRuntime.entries.filter { runtime -> runtime.hasProcess } shouldContainExactlyInAnyOrder listOf(
                HostRuntime.Jvm,
                HostRuntime.NodeJs,
                HostRuntime.Deno,
                HostRuntime.Bun,
            )
        }
    }

    context("identifiers") {
        test("shows every value by its identifier") {
            HostRuntime.entries.forEach { runtime -> runtime.toString() shouldBe runtime.identifier }
            HostSystem.entries.forEach { system -> system.toString() shouldBe system.identifier }
            HostArchitecture.entries.forEach { architecture ->
                architecture.toString() shouldBe architecture.identifier
            }
            DetectionSource.entries.forEach { source -> source.toString() shouldBe source.identifier }
        }

        test("keeps the identifiers unique") {
            HostRuntime.entries
                .map { runtime -> runtime.identifier }
                .toSet()
                .size shouldBe HostRuntime.entries.size
            HostSystem.entries
                .map { system -> system.identifier }
                .toSet()
                .size shouldBe HostSystem.entries.size
        }
    }

    context("native counterparts") {
        test("map the systems with native builds only") {
            HostSystem.entries.mapNotNull { system -> system.operatingSystem } shouldBe listOf(
                OperatingSystem.Linux,
                OperatingSystem.MacOs,
                OperatingSystem.Windows,
            )
        }

        test("map the 64-bit architectures only") {
            HostArchitecture.entries.mapNotNull { architecture -> architecture.architecture } shouldBe
                Architecture.entries
        }

        test("trust only what a runtime reports about itself") {
            DetectionSource.entries.filter { source -> source.isAuthoritative } shouldContainExactlyInAnyOrder listOf(
                DetectionSource.SystemProperty,
                DetectionSource.Process,
                DetectionSource.DenoBuild,
            )
        }
    }
})
