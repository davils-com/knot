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

/**
 * The runtime a program runs in: the JVM, a browser window, a web worker or a JavaScript runtime outside the browser.
 *
 * On the `wasmJs` target the runtime is detected from the global scope, so one klib tells a browser tab, a worker,
 * Node.js, Deno and Bun apart without a build of its own for each.
 *
 * @property identifier the stable name of the runtime, such as `nodejs`.
 * @since 1.0.0
 */
public enum class HostRuntime(public val identifier: String) {
    /**
     * A Java virtual machine.
     *
     * @since 1.0.0
     */
    Jvm(identifier = "jvm"),

    /**
     * The main thread of a browser page, with a `window` and a `document`.
     *
     * @since 1.0.0
     */
    Browser(identifier = "browser"),

    /**
     * A dedicated web worker, whose global scope is a `DedicatedWorkerGlobalScope`.
     *
     * @since 1.0.0
     */
    DedicatedWorker(identifier = "dedicated-worker"),

    /**
     * A shared web worker, whose global scope is a `SharedWorkerGlobalScope`.
     *
     * @since 1.0.0
     */
    SharedWorker(identifier = "shared-worker"),

    /**
     * A service worker, whose global scope is a `ServiceWorkerGlobalScope`.
     *
     * @since 1.0.0
     */
    ServiceWorker(identifier = "service-worker"),

    /**
     * Node.js.
     *
     * @since 1.0.0
     */
    NodeJs(identifier = "nodejs"),

    /**
     * Deno, also when it offers the `process` object of Node.js.
     *
     * @since 1.0.0
     */
    Deno(identifier = "deno"),

    /**
     * Bun, also when it offers the `process` object of Node.js.
     *
     * @since 1.0.0
     */
    Bun(identifier = "bun"),

    /**
     * A WebAssembly runtime of the `wasmJs` target that shows none of the known global scopes.
     *
     * @since 1.0.0
     */
    UnknownWasm(identifier = "wasm"),
    ;

    /**
     * Whether the program runs on WebAssembly, which every runtime but [Jvm] does.
     *
     * @since 1.0.0
     */
    public val isWasm: Boolean get() = this != Jvm

    /**
     * Whether the runtime is a web worker: [DedicatedWorker], [SharedWorker] or [ServiceWorker].
     *
     * @since 1.0.0
     */
    public val isWorker: Boolean get() = this == DedicatedWorker || this == SharedWorker || this == ServiceWorker

    /**
     * Whether the runtime is part of a browser: [Browser] or a web worker.
     *
     * @since 1.0.0
     */
    public val isWeb: Boolean get() = this == Browser || isWorker

    /**
     * Whether the runtime is a process of its own with environment variables and an exit: [Jvm], [NodeJs], [Deno] or
     * [Bun].
     *
     * @since 1.0.0
     */
    public val hasProcess: Boolean get() = this == Jvm || this == NodeJs || this == Deno || this == Bun

    /**
     * Returns the [identifier] of the runtime.
     *
     * @return the identifier, such as `nodejs`.
     * @since 1.0.0
     */
    override fun toString(): String = identifier
}
