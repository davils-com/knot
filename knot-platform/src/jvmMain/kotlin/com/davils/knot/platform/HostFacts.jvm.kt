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

import com.davils.kore.core.processVariable

private const val OS_NAME_PROPERTY: String = "os.name"
private const val OS_ARCH_PROPERTY: String = "os.arch"
private const val JAVA_VERSION_PROPERTY: String = "java.version"
private const val HOST_NAME_VARIABLE: String = "HOSTNAME"
private const val COMPUTER_NAME_VARIABLE: String = "COMPUTERNAME"

internal actual fun observedHostProbe(): HostProbe = hostProbe {
    isJvm = true
    osName = System.getProperty(OS_NAME_PROPERTY)
    osArch = System.getProperty(OS_ARCH_PROPERTY)
    javaVersion = System.getProperty(JAVA_VERSION_PROPERTY)
    processId = ProcessHandle.current().pid()
    hostNameVariable = processVariable(HOST_NAME_VARIABLE)
    computerNameVariable = processVariable(COMPUTER_NAME_VARIABLE)
}

internal actual suspend fun observedHighEntropyProbe(): HostProbe = observedHostProbe()
