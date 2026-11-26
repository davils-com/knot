# Knot

Knot holds the system building blocks of Davils applications: the host and runtime a program runs on, its environment
variables, its coordinated shutdown and the handling of its unexpected failures, for Kotlin on the JVM and on
WebAssembly (`wasmJs`). It reports through events on [konfetti](https://github.com/davils-com/konfetti) topics and
meters, spans and health in [kollect](https://github.com/davils-com/kollect), is built on
[Kore](https://github.com/davils-com/kore) and is published under the [Apache License 2.0](LICENSE).

| Module | Artifact | What it holds |
|---|---|---|
| `knot-platform` | `com.davils:knot-platform` | The host: runtime (JVM, browser, web workers, Node.js, Deno, Bun), system and architecture behind it, and the native platforms |
| `knot-environment` | `com.davils:knot-environment` | Environment variables: the `environment { }` DSL, dotenv texts, expansion, typed reads and declarative schemas |
| `knot-shutdown` | `com.davils:knot-shutdown` | The coordinated shutdown: phases, ordered tasks, budgets and, on the JVM, the shutdown hook, the fatal error trigger and the watchdog |
| `knot-failure` | `com.davils:knot-failure` | The failure handler: classification, storm protection, crash reports for sinks of the application and, on the JVM, uncaught exceptions |
| `knot-telemetry` | `com.davils:knot-telemetry` | The OpenTelemetry resource of the process for kollect: host facts, `OTEL_SERVICE_NAME` and `OTEL_RESOURCE_ATTRIBUTES` |
| `knot-testing` | `com.davils:knot-testing` | Host probes of every runtime, recording sinks and runtimes, failing and slow sinks and ready reports |
| `knot-bom` | `com.davils:knot-bom` | The bill of materials of every module |

Every module is a Kotlin Multiplatform library for `jvm` and `wasmJs`, written for Kotlin 2.4 or newer. On the JVM it
requires JDK 25 and is a named Java module, such as `com.davils.knot.shutdown`. On Wasm it runs in the browser, in web
workers, under Node.js, Deno and Bun, and is tested under Node.js.

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
    implementation(platform("com.davils:knot-bom:1.1.0"))
    implementation("com.davils:knot-platform")
    implementation("com.davils:knot-environment")
    implementation("com.davils:knot-shutdown")
    implementation("com.davils:knot-failure")
    testImplementation("com.davils:knot-testing")
}
```

In a multiplatform project, declare it in `commonMain`; Gradle picks the artifact of each target. A Maven build names
the JVM artifacts directly, such as `com.davils:knot-shutdown-jvm`.

## Platform

```kotlin
val host = Host.current
host.runtime                  // Jvm, Browser, DedicatedWorker, SharedWorker, ServiceWorker, NodeJs, Deno, Bun
host.system                   // Linux, MacOs, Windows, Android, Ios, ChromeOs, FreeBsd or Unknown
host.architecture             // X64, Arm64, X86, Arm32 or Unknown
host.systemSource             // where the system comes from, such as Process or UserAgent
host.platform                 // linux-x86_64 under Node.js on Linux, null in a browser
host.runtime.isWeb            // true in a browser window and in every web worker

val detailed = Host.detailed()                 // asks Chromium for the architecture through client hints
val visitor = Host.detect(hostProbe { userAgent = request.userAgent })

Platform.requireCurrent()                      // throws on a machine without a native build
Platform.parse("windows-x86_64").operatingSystem
OperatingSystem.current                        // Wasm on every wasmJs runtime, the native system on the JVM
```

The same klib tells a browser tab, a web worker, Node.js, Deno and Bun apart without a build target of its own. A fact
from the JVM, `process` or `Deno.build` is authoritative; the client hints and the user agent of a browser are hints,
and Knot never derives a native platform from them. A system or an architecture that matches nothing is `Unknown` or
`null`: Knot never guesses, and a 32-bit machine never resolves to a 64-bit build.

## Environment

```kotlin
val environment = environment(base = Environment.SYSTEM) {
    dotenv(defaultsText)                     // a dotenv text the application read, never a file Knot opens
    layer(mapOf("APP_REGION" to "eu"))
    prefixed("APP_") {
        default("LOG_LEVEL", "info")         // only when nothing else sets APP_LOG_LEVEL
        variable("HOME", "/opt/app")         // wins over the base and every layer
        alias("TOKEN", "LEGACY_TOKEN")       // reads APP_LEGACY_TOKEN when APP_TOKEN is not set
        remove("DEBUG")
        require("TOKEN")
    }
    filter { excludePrefix("APP_INTERNAL_") }
    scope("APP_")                            // short names: variable("HOME") reads APP_HOME
    expand()                                 // ${NAME} and ${NAME:-default} in every value
    snapshot()                               // read the base and the layers once
}

environment.int("PORT")
environment.duration("TIMEOUT", max = 1.minutes)
environment.list("HOSTS")
environment.enum<Level>("LOG_LEVEL")
environment.requireValue("TOKEN", Converters.custom("token") { raw -> Token(raw) })
environment.toString()                      // {HOME=****, LOG_LEVEL=****, …}
```

A schema declares typed settings once and loads them at once:

```kotlin
object ServerSettings : EnvironmentSchema(prefix = "SERVER_", isStrict = true) {
    val port = int("PORT") {
        default = 8080
        description = "the port to listen on"
        accept("must be a port") { port -> port in 1..65_535 }
    }
    val token = string("TOKEN") {
        required()
        secret()
    }
}

val settings = ServerSettings.load(Environment.SYSTEM)
settings.require(ServerSettings.port)
ServerSettings.describe()                   // every setting with its type, default and description
```

Every declaration is checked once the block returns; a broken one throws a `ValidationException` of Kore that lists
every problem with a stable code of `EnvironmentViolationCodes` and never a value. Dotenv texts, expansions, lists and
schemas are bounded by `EnvironmentLimits`.

## Shutdown

```kotlin
val shutdown = shutdown(applicationScope) {
    name = "orders"
    gracePeriod = 30.seconds
    events = eventBus
    meters = meterRegistry
    announce { task("readiness") { action { readiness.down() } } }
    drain { task("http") { action { server.drain(remaining) } } }
    eventBus(eventBus)                           // drains the bus, then closes it
    release {
        closing("database", dataSource)
        task("cache") {
            after("database")
            timeout = 2.seconds
            action { cache.flush() }
        }
    }
}

val installation = shutdown.install {            // JVM only
    fatalErrors = FatalErrorPolicy.Exit()
    escalation = Escalation.Halt(after = 10.seconds)
}
installation.exit()                              // runs the shutdown and ends the process with its exit code
```

Phases run one after another, the tasks of a phase side by side after the tasks they wait on. A task that throws, runs
out of its budget or never ends is reported and never stops the shutdown; the report chooses the exit code. On the JVM,
`install` adds the shutdown hook, the fatal error trigger and the watchdog that halts an overrunning shutdown. The
`wasmJs` target has no such hooks: an application calls `shutdown.initiate()` itself, such as when a page is hidden or
a Node.js process receives a signal.

## Failures

```kotlin
val diskSink = FailureSink { report ->
    withContext(Dispatchers.IO) {
        Files.writeString(crashes.resolve(report.suggestedName), report.text)
    }
}

val failures = failures(applicationScope) {
    shutdown = applicationShutdown                // or exitThrough(installation) on the JVM
    classify { fatal<DatabaseCorruptedException>() }
    storm { maxReports = 20 }
    sinks {
        sink("disk", diskSink) { renderer = CrashReportRenderer.json() }
        sink("alerts", alertSink) { severities = setOf(FailureSeverity.Fatal) }
    }
}

failures.installUncaughtHandler()                 // JVM only
val job = applicationScope.launch(failures.coroutineHandler()) { work() }
failures.report(cause, FailureOrigin.Job("nightly-import"))
```

Knot never touches a file: a crash report reaches the disk, a log or a remote service only through a `FailureSink` of
the application. Reports come as plain text or JSON with every untrusted text escaped, and `RenderOptions` leave out
messages or stack frames for reports that leave the process. The storm guard suppresses repeats of the same
fingerprint, fatal failures run the shutdown exactly once after their report was written, and nothing a sink, a rule
or a renderer throws ever comes back as another failure.

## Testing

```kotlin
Host.detect(HostProbes.chromeOnAndroid).isMobile            // true
val sink = RecordingFailureSink()
val runtime = RecordingShutdownRuntime()                    // JVM: records hooks, timers, exits and halts
shutdown.install(runtime).exit()                            // throws ProcessExit instead of ending the JVM
```

## Security

Knot assumes that what a runtime reports, the names and values of environment variables, dotenv texts, the failures
and messages that reach a failure handler and the names of tasks, sinks and origins may come from an attacker. Every
input is bounded by a limit object before work grows with it, untrusted text is shown only through `SafeText` of Kore,
the value of an environment variable never appears in a message or a `toString`, and foreign code runs contained.
[SECURITY.md](SECURITY.md) describes the scope, the known limitations and how to report a vulnerability.

## Building

```bash
./gradlew check
```

It needs JDK 25 and network access: besides compilation with warnings as errors, Detekt over main and test sources, the
tests on the JVM and under Node.js, the process tests of the shutdown and coverage, it runs Trivy, the standards audit
and the Kreate assurance gate. [AGENTS.md](AGENTS.md) holds the rules every change follows and
[CONTRIBUTING.md](CONTRIBUTING.md) how a change gets in.

## License

Copyright 2026 Davils. Licensed under the [Apache License, Version 2.0](LICENSE).
