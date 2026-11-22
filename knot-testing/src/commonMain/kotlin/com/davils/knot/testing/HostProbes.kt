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

import com.davils.knot.platform.HostProbe
import com.davils.knot.platform.hostProbe

/**
 * The facts that common runtimes report, for tests of code that depends on the [Host][com.davils.knot.platform.Host].
 *
 * ```kotlin
 * val host = Host.detect(HostProbes.chromeOnWindows)
 * ```
 *
 * Every probe holds the facts a real runtime of its kind reports, with user agents of the end of 2026.
 *
 * @since 1.0.0
 */
public object HostProbes {
    /**
     * A JVM on Linux x86-64.
     *
     * @since 1.0.0
     */
    public val jvmOnLinux: HostProbe = hostProbe {
        isJvm = true
        osName = "Linux"
        osArch = "amd64"
        javaVersion = "25.0.1"
    }

    /**
     * Node.js on Linux x86-64.
     *
     * @since 1.0.0
     */
    public val nodeOnLinux: HostProbe = hostProbe {
        nodeVersion = "22.11.0"
        processPlatform = "linux"
        processArch = "x64"
        userAgent = "Node.js/22"
    }

    /**
     * Deno on macOS on Apple silicon.
     *
     * @since 1.0.0
     */
    public val denoOnMac: HostProbe = hostProbe {
        denoVersion = "2.1.4"
        denoOs = "darwin"
        denoArch = "aarch64"
    }

    /**
     * Bun on Linux on 64-bit ARM.
     *
     * @since 1.0.0
     */
    public val bunOnLinux: HostProbe = hostProbe {
        bunVersion = "1.1.38"
        nodeVersion = "22.6.0"
        processPlatform = "linux"
        processArch = "arm64"
    }

    /**
     * Chrome on Windows on x86-64, with client hints.
     *
     * @since 1.0.0
     */
    public val chromeOnWindows: HostProbe = hostProbe {
        hasWindow = true
        userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) " +
            "Chrome/131.0.0.0 Safari/537.36"
        clientHintsPlatform = "Windows"
        isClientHintsMobile = false
        clientHintsArchitecture = "x86"
        clientHintsBitness = "64"
    }

    /**
     * Safari on macOS, which reports no architecture.
     *
     * @since 1.0.0
     */
    public val safariOnMac: HostProbe = hostProbe {
        hasWindow = true
        userAgent = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) " +
            "Version/18.1 Safari/605.1.15"
    }

    /**
     * Firefox on Linux on x86-64.
     *
     * @since 1.0.0
     */
    public val firefoxOnLinux: HostProbe = hostProbe {
        hasWindow = true
        userAgent = "Mozilla/5.0 (X11; Linux x86_64; rv:133.0) Gecko/20100101 Firefox/133.0"
    }

    /**
     * Chrome on an Android phone.
     *
     * @since 1.0.0
     */
    public val chromeOnAndroid: HostProbe = hostProbe {
        hasWindow = true
        userAgent = "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) " +
            "Chrome/131.0.0.0 Mobile Safari/537.36"
        clientHintsPlatform = "Android"
        isClientHintsMobile = true
    }

    /**
     * Safari on an iPhone.
     *
     * @since 1.0.0
     */
    public val safariOnIphone: HostProbe = hostProbe {
        hasWindow = true
        userAgent = "Mozilla/5.0 (iPhone; CPU iPhone OS 18_1 like Mac OS X) AppleWebKit/605.1.15 " +
            "(KHTML, like Gecko) Version/18.1 Mobile/15E148 Safari/604.1"
    }

    /**
     * A dedicated web worker of Firefox on Linux.
     *
     * @since 1.0.0
     */
    public val dedicatedWorker: HostProbe = hostProbe {
        isDedicatedWorker = true
        userAgent = "Mozilla/5.0 (X11; Linux x86_64; rv:133.0) Gecko/20100101 Firefox/133.0"
    }

    /**
     * A service worker of Chrome on Windows.
     *
     * @since 1.0.0
     */
    public val serviceWorker: HostProbe = hostProbe {
        isServiceWorker = true
        clientHintsPlatform = "Windows"
    }

    /**
     * Every probe of this object.
     *
     * @since 1.0.0
     */
    public val all: List<HostProbe> get() = listOf(
        jvmOnLinux,
        nodeOnLinux,
        denoOnMac,
        bunOnLinux,
        chromeOnWindows,
        safariOnMac,
        firefoxOnLinux,
        chromeOnAndroid,
        safariOnIphone,
        dedicatedWorker,
        serviceWorker,
    )
}
