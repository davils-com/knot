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

import com.davils.knot.shutdown.exit.ExitCode

/**
 * What [RecordingShutdownRuntime] throws instead of halting the process with `Runtime.halt`.
 *
 * @property code the exit code the process would have halted with.
 * @since 1.0.0
 */
public class ProcessHalt internal constructor(public val code: ExitCode) :
    RuntimeException("The process would have halted with $code")
