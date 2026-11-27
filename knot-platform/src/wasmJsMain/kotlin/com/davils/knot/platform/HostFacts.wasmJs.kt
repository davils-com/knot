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

@file:OptIn(ExperimentalWasmJsInterop::class)

package com.davils.knot.platform

import kotlinx.coroutines.await
import kotlin.js.Promise

private const val ARCHITECTURE_HINT: String = "architecture"
private const val BITNESS_HINT: String = "bitness"
private const val TRUE: Int = 1
private const val FALSE: Int = 0
private const val HOST_NAME_VARIABLE: String = "HOSTNAME"
private const val COMPUTER_NAME_VARIABLE: String = "COMPUTERNAME"

private val mobileFlags: Map<Int, Boolean> = mapOf(TRUE to true, FALSE to false)

internal actual fun observedHostProbe(): HostProbe = hostProbe { observe() }

internal actual suspend fun observedHighEntropyProbe(): HostProbe {
    val hints = highEntropyHints()?.await<JsAny?>()
    return hostProbe {
        observe()
        clientHintsArchitecture = hints?.let { found -> hintOf(found, ARCHITECTURE_HINT) }
        clientHintsBitness = hints?.let { found -> hintOf(found, BITNESS_HINT) }
    }
}

private fun processIdOf(reported: Double): Long? = reported.takeIf { id -> id > 0 }?.toLong()

private fun HostProbeBuilder.observe() {
    processPlatform = processText("platform")
    processArch = processText("arch")
    nodeVersion = nodeVersionText()
    denoOs = denoBuildText("os")
    denoArch = denoBuildText("arch")
    denoVersion = denoVersionText()
    bunVersion = bunVersionText()
    hasWindow = hasWindowAndDocument()
    isDedicatedWorker = isGlobalScope("DedicatedWorkerGlobalScope")
    isSharedWorker = isGlobalScope("SharedWorkerGlobalScope")
    isServiceWorker = isGlobalScope("ServiceWorkerGlobalScope")
    userAgent = userAgentText()
    clientHintsPlatform = clientHintsPlatformText()
    isClientHintsMobile = mobileFlags[isClientHintsMobileFlag()]
    processId = processIdOf(processIdNumber())
    hostNameVariable = environmentText(HOST_NAME_VARIABLE)
    computerNameVariable = environmentText(COMPUTER_NAME_VARIABLE)
}

@JsFun(
    """(name) => { try { return (typeof process === "object" && process !== null &&
        typeof process[name] === "string") ? process[name] : null; } catch (e) { return null; } }""",
)
private external fun processText(name: String): String?

@JsFun(
    """() => { try { return (typeof process === "object" && process !== null && process.versions &&
        typeof process.versions.node === "string") ? process.versions.node : null; } catch (e) { return null; } }""",
)
private external fun nodeVersionText(): String?

@JsFun(
    """(name) => { try { return (typeof Deno === "object" && Deno !== null && Deno.build &&
        typeof Deno.build[name] === "string") ? Deno.build[name] : null; } catch (e) { return null; } }""",
)
private external fun denoBuildText(name: String): String?

@JsFun(
    """() => { try { return (typeof Deno === "object" && Deno !== null && Deno.version &&
        typeof Deno.version.deno === "string") ? Deno.version.deno : null; } catch (e) { return null; } }""",
)
private external fun denoVersionText(): String?

@JsFun(
    """() => { try { return (typeof Bun === "object" && Bun !== null && typeof Bun.version === "string")
        ? Bun.version : null; } catch (e) { return null; } }""",
)
private external fun bunVersionText(): String?

@JsFun(
    """() => { try { return typeof window === "object" && window !== null && typeof document === "object" &&
        document !== null; } catch (e) { return false; } }""",
)
private external fun hasWindowAndDocument(): Boolean

@JsFun(
    """(name) => { try { const scope = globalThis[name]; return typeof scope === "function" &&
        globalThis instanceof scope; } catch (e) { return false; } }""",
)
private external fun isGlobalScope(name: String): Boolean

@JsFun(
    """() => { try { return (typeof navigator === "object" && navigator !== null &&
        typeof navigator.userAgent === "string") ? navigator.userAgent : null; } catch (e) { return null; } }""",
)
private external fun userAgentText(): String?

@JsFun(
    """() => { try { const data = (typeof navigator === "object" && navigator !== null)
        ? navigator.userAgentData : undefined; return (data && typeof data.platform === "string")
        ? data.platform : null; } catch (e) { return null; } }""",
)
private external fun clientHintsPlatformText(): String?

@JsFun(
    """() => { try { const data = (typeof navigator === "object" && navigator !== null)
        ? navigator.userAgentData : undefined; return (data && typeof data.mobile === "boolean")
        ? (data.mobile ? 1 : 0) : -1; } catch (e) { return -1; } }""",
)
private external fun isClientHintsMobileFlag(): Int

@JsFun(
    """() => { try { const data = (typeof navigator === "object" && navigator !== null)
        ? navigator.userAgentData : undefined; if (!data || typeof data.getHighEntropyValues !== "function")
        return null; return data.getHighEntropyValues(["architecture", "bitness"]).then((hints) => hints, () => null); }
        catch (e) { return null; } }""",
)
private external fun highEntropyHints(): Promise<JsAny?>?

@JsFun(
    """(hints, name) => { try { return (hints && typeof hints[name] === "string") ? hints[name] : null; }
        catch (e) { return null; } }""",
)
private external fun hintOf(hints: JsAny, name: String): String?

@JsFun(
    """() => { try { const id = (typeof Deno === "object" && Deno !== null) ? Deno.pid
        : ((typeof process === "object" && process !== null) ? process.pid : undefined);
        return Number.isSafeInteger(id) ? id : -1; } catch (e) { return -1; } }""",
)
private external fun processIdNumber(): Double

@JsFun(
    """(name) => { try { if (typeof Deno === "object" && Deno !== null) { const state = (Deno.permissions &&
        typeof Deno.permissions.querySync === "function") ? Deno.permissions.querySync({ name: "env", variable: name })
        .state : "denied"; const value = state === "granted" ? Deno.env.get(name) : undefined;
        return typeof value === "string" ? value : null; } const env = (typeof process === "object" &&
        process !== null) ? process.env : undefined; return (env && typeof env[name] === "string") ? env[name] : null; }
        catch (e) { return null; } }""",
)
private external fun environmentText(name: String): String?
