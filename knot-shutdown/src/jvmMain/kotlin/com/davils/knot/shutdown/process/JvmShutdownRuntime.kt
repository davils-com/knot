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

@file:OptIn(DavilsInternal::class)

package com.davils.knot.shutdown.process

import com.davils.knot.shutdown.exit.ExitCode
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.annotation.DavilsPlatformBinding
import java.util.Timer
import kotlin.concurrent.timerTask
import kotlin.system.exitProcess
import kotlin.time.Duration

private const val UNCAUGHT_PREFIX: String = "Exception in thread \""
private const val UNCAUGHT_SUFFIX: String = "\" "

@DavilsPlatformBinding
internal object JvmShutdownRuntime : ShutdownRuntime {
    override fun addHook(name: String, action: () -> Unit): AutoCloseable {
        val hook = Thread(action, name)
        Runtime.getRuntime().addShutdownHook(hook)
        return AutoCloseable { removeHook(hook) }
    }

    override fun uncaughtHandler(): Thread.UncaughtExceptionHandler? = Thread.getDefaultUncaughtExceptionHandler()

    override fun installUncaughtHandler(handler: Thread.UncaughtExceptionHandler?) {
        Thread.setDefaultUncaughtExceptionHandler(handler)
    }

    override fun reportUncaught(thread: Thread, cause: Throwable) {
        System.err.print(UNCAUGHT_PREFIX + thread.name + UNCAUGHT_SUFFIX)
        cause.printStackTrace(System.err)
    }

    override fun schedule(name: String, after: Duration, action: () -> Unit): AutoCloseable {
        val timer = Timer(name, true)
        timer.schedule(timerTask { action() }, after.inWholeMilliseconds)
        return AutoCloseable { timer.cancel() }
    }

    override fun exit(code: ExitCode): Nothing = exitProcess(code.value)

    override fun halt(code: ExitCode): Nothing {
        Runtime.getRuntime().halt(code.value)
        error("The JVM did not halt")
    }

    private fun removeHook(hook: Thread) {
        runCatching { Runtime.getRuntime().removeShutdownHook(hook) }
    }
}
