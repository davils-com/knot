# Knot

Knot holds the common building blocks of Davils applications: the platform a program runs on and its environment
variables, for Kotlin on the JVM and on WebAssembly (`wasmJs`). It is built on [Kore](https://github.com/davils-com/kore)
and published under the [Apache License 2.0](LICENSE).

| Module | Artifact | What it holds |
|---|---|---|
| `knot-platform` | `com.davils:knot-platform` | The operating system, the architecture and the supported platform of the running program |
| `knot-environment` | `com.davils:knot-environment` | Environment variables and a DSL that layers, overrides, removes and scopes them |

Both modules are Kotlin Multiplatform libraries with a `jvm` and a `wasmJs` target, like Kore. The JVM target
requires JDK 25; the Wasm target runs in the browser and under Node.js. Knot is written for Kotlin 2.4 or newer. Both
modules depend on `kore-core`. On the JVM they are named Java modules, `com.davils.knot.platform` and
`com.davils.knot.environment`.

## Installation

Knot is published to the Davils repository, not to Maven Central. Declare it for the `com.davils` group only:

```kotlin
// settings.gradle.kts
dependencyResolutionManagement {
    repositories {
        exclusiveContent {
            forRepository { maven("https://repo.davils.com/releases") }
            filter { includeGroupAndSubgroups("com.davils") }
        }
        mavenCentral()
    }
}
```

```kotlin
// build.gradle.kts
dependencies {
    implementation("com.davils:knot-platform:1.0.0")
    implementation("com.davils:knot-environment:1.0.0")
}
```

In a multiplatform project, declare it in `commonMain`; Gradle picks the artifact of each target. A Maven build depends
on `knot-platform-jvm` and `knot-environment-jvm`.

## A short tour

### Platform

```kotlin
val platform = Platform.requireCurrent()      // throws on a machine without a build
platform.identifier                           // "linux-x86_64", "macos-aarch64", "windows-x86_64", ...
platform.isUnix                               // true on Linux and macOS

Platform.resolve(osName = "Mac OS X", osArch = "arm64")   // macos-aarch64
Platform.parse("windows-x86_64").operatingSystem          // OperatingSystem.Windows
Platform.entries                                          // every supported platform

OperatingSystem.current                       // Linux, MacOs or Windows on the JVM, Web on wasmJs
OperatingSystem.current?.isWeb                // true in a browser and under Node.js on wasmJs
```

A system or an architecture without a build resolves to `null`: Knot never guesses, and a 32-bit machine never
resolves to a 64-bit build. Every runtime of the `wasmJs` target, a browser and Node.js alike, runs on
`OperatingSystem.Web`, which has no native build, so `Platform.current` is `null` there.

### Environment

```kotlin
val defaults = environment {
    variable("KNOT_LOG_LEVEL", "info")
}

val environment = environment(base = Environment.SYSTEM) {
    layer(defaults)                       // a later layer wins over an earlier one and over the base
    variable("KNOT_HOME", "/opt/knot")    // an override wins over every layer
    remove("KNOT_TOKEN")                  // hidden from the base and every layer
    scope("KNOT_")                        // only KNOT_* variables, under their short names
}

environment.variable("HOME")              // reads KNOT_HOME: "/opt/knot"
environment.variable("LOG_LEVEL")         // "info", unless the process sets KNOT_LOG_LEVEL
environment.toString()                    // {HOME=****, LOG_LEVEL=****}
```

Every declaration is checked once the block returns; a broken one throws a `ValidationException` of Kore that lists
every problem with a stable code of `EnvironmentViolationCodes` and never a value. An environment declared on
`Environment.EMPTY` is a complete fake for tests.

## Security

The names a runtime reports and every declared variable may come from an attacker. Knot bounds them before it works
with them, shows untrusted text only escaped, and never shows the value of a variable. See [SECURITY.md](SECURITY.md)
for the scope, known limitations and how to report a vulnerability privately.

## Building

```bash
./gradlew check
```

It needs JDK 25 and network access: besides compilation with warnings as errors, Detekt, the tests and coverage it
runs Trivy and the Kreate assurance gate. See [CONTRIBUTING.md](CONTRIBUTING.md) and [AGENTS.md](AGENTS.md).

## License

Copyright 2026 Davils. Licensed under the [Apache License, Version 2.0](LICENSE).
