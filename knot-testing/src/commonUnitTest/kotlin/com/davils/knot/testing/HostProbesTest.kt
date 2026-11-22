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

package com.davils.knot.testing

import com.davils.knot.platform.Host
import com.davils.knot.platform.HostRuntime
import com.davils.knot.platform.HostSystem
import com.davils.knot.platform.Platform
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class HostProbesTest : FunSpec({
    test("every probe detects the runtime it stands for") {
        HostProbes.all.map { probe -> Host.detect(probe).runtime } shouldBe listOf(
            HostRuntime.Jvm,
            HostRuntime.NodeJs,
            HostRuntime.Deno,
            HostRuntime.Bun,
            HostRuntime.Browser,
            HostRuntime.Browser,
            HostRuntime.Browser,
            HostRuntime.Browser,
            HostRuntime.Browser,
            HostRuntime.DedicatedWorker,
            HostRuntime.ServiceWorker,
        )
    }

    test("the probes name the system and the native platform behind the runtime") {
        Host.detect(HostProbes.nodeOnLinux).platform shouldBe Platform.parse("linux-x86_64")
        Host.detect(HostProbes.denoOnMac).platform shouldBe Platform.parse("macos-aarch64")
        Host.detect(HostProbes.bunOnLinux).platform shouldBe Platform.parse("linux-aarch64")
        Host.detect(HostProbes.jvmOnLinux).platform shouldBe Platform.parse("linux-x86_64")
        Host.detect(HostProbes.chromeOnWindows).system shouldBe HostSystem.Windows
        Host.detect(HostProbes.safariOnIphone).system shouldBe HostSystem.Ios
        Host.detect(HostProbes.chromeOnAndroid).isMobile shouldBe true
        Host.detect(HostProbes.safariOnMac).platform shouldBe null
        Host.detect(HostProbes.firefoxOnLinux).system shouldBe HostSystem.Linux
    }
})
