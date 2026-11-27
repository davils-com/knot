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

package com.davils.knot.platform

import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.text.SafeText

/**
 * The raw facts a runtime reported about itself, from which [Host.detect] derives a [Host].
 *
 * [Host.current] reads the facts of the running program; [hostProbe] declares them by hand, for a test or for facts
 * that a caller gathered elsewhere, such as the user agent of a request. A probe is immutable and keeps every fact as
 * it was reported, unbounded: the bounds of [PlatformLimits] apply when a host is detected, and the `toString` shows
 * every text escaped.
 *
 * @since 1.0.0
 */
public class HostProbe internal constructor(builder: HostProbeBuilder) {
    /**
     * The fact [HostProbeBuilder.isJvm] declared.
     *
     * @since 1.0.0
     */
    public val isJvm: Boolean = builder.isJvm

    /**
     * The fact [HostProbeBuilder.osName] declared.
     *
     * @since 1.0.0
     */
    public val osName: String? = builder.osName

    /**
     * The fact [HostProbeBuilder.osArch] declared.
     *
     * @since 1.0.0
     */
    public val osArch: String? = builder.osArch

    /**
     * The fact [HostProbeBuilder.javaVersion] declared.
     *
     * @since 1.0.0
     */
    public val javaVersion: String? = builder.javaVersion

    /**
     * The fact [HostProbeBuilder.processPlatform] declared.
     *
     * @since 1.0.0
     */
    public val processPlatform: String? = builder.processPlatform

    /**
     * The fact [HostProbeBuilder.processArch] declared.
     *
     * @since 1.0.0
     */
    public val processArch: String? = builder.processArch

    /**
     * The fact [HostProbeBuilder.nodeVersion] declared.
     *
     * @since 1.0.0
     */
    public val nodeVersion: String? = builder.nodeVersion

    /**
     * The fact [HostProbeBuilder.denoOs] declared.
     *
     * @since 1.0.0
     */
    public val denoOs: String? = builder.denoOs

    /**
     * The fact [HostProbeBuilder.denoArch] declared.
     *
     * @since 1.0.0
     */
    public val denoArch: String? = builder.denoArch

    /**
     * The fact [HostProbeBuilder.denoVersion] declared.
     *
     * @since 1.0.0
     */
    public val denoVersion: String? = builder.denoVersion

    /**
     * The fact [HostProbeBuilder.bunVersion] declared.
     *
     * @since 1.0.0
     */
    public val bunVersion: String? = builder.bunVersion

    /**
     * The fact [HostProbeBuilder.hasWindow] declared.
     *
     * @since 1.0.0
     */
    public val hasWindow: Boolean = builder.hasWindow

    /**
     * The fact [HostProbeBuilder.isDedicatedWorker] declared.
     *
     * @since 1.0.0
     */
    public val isDedicatedWorker: Boolean = builder.isDedicatedWorker

    /**
     * The fact [HostProbeBuilder.isSharedWorker] declared.
     *
     * @since 1.0.0
     */
    public val isSharedWorker: Boolean = builder.isSharedWorker

    /**
     * The fact [HostProbeBuilder.isServiceWorker] declared.
     *
     * @since 1.0.0
     */
    public val isServiceWorker: Boolean = builder.isServiceWorker

    /**
     * The fact [HostProbeBuilder.userAgent] declared.
     *
     * @since 1.0.0
     */
    public val userAgent: String? = builder.userAgent

    /**
     * The fact [HostProbeBuilder.clientHintsPlatform] declared.
     *
     * @since 1.0.0
     */
    public val clientHintsPlatform: String? = builder.clientHintsPlatform

    /**
     * The fact [HostProbeBuilder.isClientHintsMobile] declared.
     *
     * @since 1.0.0
     */
    public val isClientHintsMobile: Boolean? = builder.isClientHintsMobile

    /**
     * The fact [HostProbeBuilder.clientHintsArchitecture] declared.
     *
     * @since 1.0.0
     */
    public val clientHintsArchitecture: String? = builder.clientHintsArchitecture

    /**
     * The fact [HostProbeBuilder.clientHintsBitness] declared.
     *
     * @since 1.0.0
     */
    public val clientHintsBitness: String? = builder.clientHintsBitness

    /**
     * The fact [HostProbeBuilder.processId] declared.
     *
     * @since 1.3.0
     */
    public val processId: Long? = builder.processId

    /**
     * The fact [HostProbeBuilder.hostNameVariable] declared.
     *
     * @since 1.3.0
     */
    public val hostNameVariable: String? = builder.hostNameVariable

    /**
     * The fact [HostProbeBuilder.computerNameVariable] declared.
     *
     * @since 1.3.0
     */
    public val computerNameVariable: String? = builder.computerNameVariable

    private val texts: List<String?> get() = listOf(
        osName,
        osArch,
        javaVersion,
        processPlatform,
        processArch,
        nodeVersion,
        denoOs,
        denoArch,
        denoVersion,
        bunVersion,
        userAgent,
        clientHintsPlatform,
        clientHintsArchitecture,
        clientHintsBitness,
        hostNameVariable,
        computerNameVariable,
    )

    private val flags: List<Boolean?> get() = listOf(
        isJvm,
        hasWindow,
        isDedicatedWorker,
        isSharedWorker,
        isServiceWorker,
        isClientHintsMobile,
    )

    private val facts: List<Any?> get() = listOf(texts, flags, processId)

    /**
     * Tells whether [other] is a probe with the same facts.
     *
     * @param other the object to compare with.
     * @return `true` if [other] is an equal probe.
     * @since 1.0.0
     */
    override fun equals(other: Any?): Boolean = other is HostProbe && facts == other.facts

    /**
     * Returns a hash code derived from every fact, consistent with [equals].
     *
     * @return the hash code.
     * @since 1.0.0
     */
    override fun hashCode(): Int = facts.hashCode()

    /**
     * Returns the facts that were reported, every text escaped and quoted.
     *
     * @return the facts, such as `HostProbe(processPlatform="linux", nodeVersion="22.11.0")`.
     * @since 1.0.0
     */
    override fun toString(): String {
        val shownTexts = textNames.zip(texts).mapNotNull { (name, value) ->
            value?.let { text -> "$name=${SafeText.quoted(text)}" }
        }
        val shownFlags = flagNames.zip(flags).mapNotNull { (name, value) ->
            value?.takeIf { flag -> flag || name == MOBILE_NAME }?.let { flag -> "$name=$flag" }
        }
        val shownProcess = listOfNotNull(processId?.let { id -> "processId=$id" })
        val shown = shownFlags + shownProcess + shownTexts
        return shown.joinToString(prefix = "HostProbe(", postfix = ")")
    }

    private companion object {
        const val MOBILE_NAME: String = "isClientHintsMobile"

        val textNames: List<String> = listOf(
            "osName",
            "osArch",
            "javaVersion",
            "processPlatform",
            "processArch",
            "nodeVersion",
            "denoOs",
            "denoArch",
            "denoVersion",
            "bunVersion",
            "userAgent",
            "clientHintsPlatform",
            "clientHintsArchitecture",
            "clientHintsBitness",
            "hostNameVariable",
            "computerNameVariable",
        )

        val flagNames: List<String> = listOf(
            "isJvm",
            "hasWindow",
            "isDedicatedWorker",
            "isSharedWorker",
            "isServiceWorker",
            MOBILE_NAME,
        )
    }
}
