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

package com.davils.knot.environment.schema.fixture

import com.davils.knot.environment.schema.EnvironmentSchema
import com.davils.knot.environment.schema.Setting
import com.davils.kore.core.arguments.conversion.Converters
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

private const val DEFAULT_PORT: Int = 8080
private const val HIGHEST_PORT: Int = 65_535
private const val DEFAULT_TIMEOUT_SECONDS: Int = 30
private const val MAX_TIMEOUT_SECONDS: Int = 60

object ServerSettings : EnvironmentSchema(prefix = "SERVER_", isStrict = true) {
    val port: Setting<Int> = int("PORT") {
        default = DEFAULT_PORT
        description = "the port to listen on"
        accept("must be a port") { port -> port in 1..HIGHEST_PORT }
    }
    val token: Setting<String> = string("TOKEN") {
        required()
        secret()
    }
    val timeout: Setting<Duration> = duration(
        "TIMEOUT",
        max = MAX_TIMEOUT_SECONDS.seconds,
    ) { default = DEFAULT_TIMEOUT_SECONDS.seconds }
    val ratio: Setting<Double> = double("RATIO")
    val count: Setting<Long> = long("COUNT")
    val debug: Setting<Boolean> = boolean("DEBUG") { default = false }
    val level: Setting<String> = choice("LEVEL", "low", "high")
    val hosts: Setting<List<String>> = list("HOSTS")
    val mode: Setting<Mode> = setting("MODE", Converters.enum<Mode>())
}
