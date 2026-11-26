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

package com.davils.knot.environment.schema

import com.davils.knot.environment.Environment
import com.davils.knot.environment.EnvironmentLimits
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.arguments.conversion.Converter
import com.davils.kore.core.arguments.conversion.Converters
import com.davils.kore.core.collection.immutableList
import com.davils.kore.core.identity.Uuid
import com.davils.kore.core.size.DataSize
import com.davils.kore.core.validation.validateOrThrow
import kotlin.time.Duration

private const val SUBJECT: String = "environment settings"
private const val STRING_TYPE: String = "string"
private const val LIST_TYPE: String = "list"
private const val LIST_SEPARATOR: Char = ','
private const val TOO_MANY_ELEMENTS_MESSAGE: String = "The value holds too many elements"
private const val STRICT_MESSAGE: String = "A strict schema needs a prefix"

/**
 * The typed settings an application reads from its environment, declared once and loaded and checked at once.
 *
 * ```kotlin
 * object ServerSettings : EnvironmentSchema(prefix = "SERVER_", isStrict = true) {
 *     val port = int("PORT") {
 *         default = 8080
 *         description = "the port to listen on"
 *         accept("must be a port") { port -> port in 1..65535 }
 *     }
 *     val token = string("TOKEN") { required(); secret() }
 * }
 *
 * val settings = ServerSettings.load(Environment.SYSTEM)
 * settings.require(ServerSettings.port)
 * ```
 *
 * A subclass declares its settings in its properties; the name of each is put behind [prefix]. [load] reads and
 * converts every setting, applies its rules and reports every problem at once with the codes of
 * [EnvironmentViolationCodes][com.davils.knot.environment.EnvironmentViolationCodes], never with a value. [describe]
 * documents the settings without reading anything. At most [EnvironmentLimits.MAX_SETTINGS] settings are declared;
 * more, and a name declared twice, make [load] fail. A schema is meant to be declared on one thread; once declared,
 * loading and describing are safe from every thread.
 *
 * @property prefix the prefix of the names of every setting, such as `SERVER_`; empty by default.
 * @property isStrict whether [load] refuses variables under [prefix] that the schema does not declare, with
 * [EnvironmentViolationCodes.UNKNOWN_VARIABLE][com.davils.knot.environment.EnvironmentViolationCodes.UNKNOWN_VARIABLE];
 * a strict schema needs a [prefix].
 * @since 1.0.0
 */
public abstract class EnvironmentSchema(public val prefix: String = "", public val isStrict: Boolean = false) {
    private val declared: MutableList<Setting<*>> = mutableListOf()
    private val declaredNames: MutableSet<String> = mutableSetOf()
    private val duplicatedNames: MutableSet<String> = linkedSetOf()
    internal var hasTooManySettings: Boolean = false
        private set

    internal val settings: List<Setting<*>> get() = declared.immutableList()

    internal val duplicates: Set<String> get() = duplicatedNames

    init {
        require(!isStrict || prefix.isNotEmpty()) { STRICT_MESSAGE }
    }

    /**
     * Declares a setting of the variable [name], read with [converter].
     *
     * @param T the type of the value.
     * @param name the name of the variable without [prefix].
     * @param converter converts the text of the value, such as one of Kore's
     * [Converters][com.davils.kore.core.arguments.conversion.Converters].
     * @param block declares the default, the description and the rules.
     * @return the setting.
     * @since 1.0.0
     */
    protected fun <T : Any> setting(
        name: String,
        converter: Converter<T>,
        block: SettingBuilder<T>.() -> Unit = {
        },
    ): Setting<T> {
        val builder = SettingBuilder<T>()
        builder.block()
        val setting = Setting(name = prefix + name, converter = converter, builder = builder)
        register(setting)
        return setting
    }

    /**
     * Declares a setting whose value is the text of the variable as it is.
     *
     * @param name the name of the variable without [prefix].
     * @param block declares the default, the description and the rules.
     * @return the setting.
     * @since 1.0.0
     */
    protected fun string(name: String, block: SettingBuilder<String>.() -> Unit = {}): Setting<String> =
        setting(name, Converters.custom(STRING_TYPE) { raw -> raw }, block)

    /**
     * Declares a setting read as an [Int].
     *
     * @param name the name of the variable without [prefix].
     * @param block declares the default, the description and the rules.
     * @return the setting.
     * @since 1.0.0
     */
    protected fun int(name: String, block: SettingBuilder<Int>.() -> Unit = {}): Setting<Int> =
        setting(name, Converters.int, block)

    /**
     * Declares a setting read as a [Long].
     *
     * @param name the name of the variable without [prefix].
     * @param block declares the default, the description and the rules.
     * @return the setting.
     * @since 1.0.0
     */
    protected fun long(name: String, block: SettingBuilder<Long>.() -> Unit = {}): Setting<Long> =
        setting(name, Converters.long, block)

    /**
     * Declares a setting read as a finite [Double].
     *
     * @param name the name of the variable without [prefix].
     * @param block declares the default, the description and the rules.
     * @return the setting.
     * @since 1.0.0
     */
    protected fun double(name: String, block: SettingBuilder<Double>.() -> Unit = {}): Setting<Double> =
        setting(name, Converters.double, block)

    /**
     * Declares a setting read as a [Boolean].
     *
     * @param name the name of the variable without [prefix].
     * @param block declares the default, the description and the rules.
     * @return the setting.
     * @since 1.0.0
     */
    protected fun boolean(name: String, block: SettingBuilder<Boolean>.() -> Unit = {}): Setting<Boolean> =
        setting(name, Converters.boolean, block)

    /**
     * Declares a setting read as a [Duration] between [min] and [max].
     *
     * @param name the name of the variable without [prefix].
     * @param min the shortest duration accepted; zero by default.
     * @param max the longest duration accepted; unbounded by default.
     * @param block declares the default, the description and the rules.
     * @return the setting.
     * @throws IllegalArgumentException if [min] is negative or infinite or [max] is shorter than [min].
     * @since 1.0.0
     */
    protected fun duration(
        name: String,
        min: Duration = Duration.ZERO,
        max: Duration = Duration.INFINITE,
        block: SettingBuilder<Duration>.() -> Unit = {},
    ): Setting<Duration> = setting(name, Converters.duration(min = min, max = max), block)

    /**
     * Declares a setting read as a [DataSize] between [min] and [max], such as `512 KiB`.
     *
     * @param name the name of the variable without [prefix].
     * @param min the smallest size accepted; zero by default.
     * @param max the largest size accepted; unbounded by default.
     * @param block declares the default, the description and the rules.
     * @return the setting.
     * @throws IllegalArgumentException if [max] is smaller than [min].
     * @since 1.1.0
     */
    protected fun dataSize(
        name: String,
        min: DataSize = DataSize.ZERO,
        max: DataSize = DataSize(Long.MAX_VALUE),
        block: SettingBuilder<DataSize>.() -> Unit = {},
    ): Setting<DataSize> = setting(name, Converters.dataSize(min = min, max = max), block)

    /**
     * Declares a setting read as a [Uuid] of version 4 or 7 in canonical form.
     *
     * @param name the name of the variable without [prefix].
     * @param block declares the default, the description and the rules.
     * @return the setting.
     * @since 1.1.0
     */
    protected fun uuid(name: String, block: SettingBuilder<Uuid>.() -> Unit = {}): Setting<Uuid> =
        setting(name, Converters.uuid, block)

    /**
     * Declares a setting whose value is one of [choices], compared without regard to ASCII case.
     *
     * @param name the name of the variable without [prefix].
     * @param choices the accepted values.
     * @param block declares the default, the description and the rules.
     * @return the setting.
     * @since 1.0.0
     */
    protected fun choice(
        name: String,
        vararg choices: String,
        block: SettingBuilder<String>.() -> Unit = {
        },
    ): Setting<String> = setting(name, Converters.choice(*choices), block)

    /**
     * Declares a setting whose value is a list of the texts between commas, each trimmed, without empty ones.
     *
     * @param name the name of the variable without [prefix].
     * @param block declares the default, the description and the rules.
     * @return the setting.
     * @since 1.0.0
     */
    protected fun list(name: String, block: SettingBuilder<List<String>>.() -> Unit = {}): Setting<List<String>> =
        setting(name, Converters.custom(LIST_TYPE, ::elementsOf), block)

    /**
     * Reads and checks every setting from [environment].
     *
     * @param environment the environment to read, such as [Environment.SYSTEM] or one declared with
     * [environment][com.davils.knot.environment.environment].
     * @return the values.
     * @throws com.davils.kore.core.validation.report.ValidationException if a setting is missing, unreadable or
     * refused, if a strict schema finds an undeclared variable, or if the schema itself is broken; the report lists
     * every problem under `settings["<name>"]` and never a value.
     * @since 1.0.0
     */
    public fun load(environment: Environment): EnvironmentSettings {
        val check = SettingsCheck(schema = this, environment = environment)
        check.validateOrThrow(SUBJECT)
        return EnvironmentSettings(schema = this, values = check.values.toMap())
    }

    /**
     * Describes every setting in the order of its declaration, without reading any variable.
     *
     * @return the descriptions.
     * @since 1.0.0
     */
    public fun describe(): List<SettingDescription> = declared.map(::SettingDescription).immutableList()

    internal fun declares(setting: Setting<*>): Boolean = declared.any { candidate -> candidate === setting }

    private fun register(setting: Setting<*>) {
        if (declared.size >= EnvironmentLimits.MAX_SETTINGS) {
            hasTooManySettings = true
            return
        }
        if (!declaredNames.add(setting.name)) duplicatedNames += setting.name
        declared += setting
    }

    private fun elementsOf(raw: String): List<String> {
        val parts = raw.split(LIST_SEPARATOR)
        require(parts.size <= EnvironmentLimits.MAX_ELEMENTS) { TOO_MANY_ELEMENTS_MESSAGE }
        val trimmed = parts.map(String::trim)
        return trimmed.filter(String::isNotEmpty).immutableList()
    }
}
