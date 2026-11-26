# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## 1.1.0 - 2026-11-26

### Added

- **`knot-telemetry`, the resource of the process for kollect.** `hostResource(serviceName, attributes, environment,
  host)` builds the OpenTelemetry `Resource` of kollect from the host facts of `knot-platform` (`os.type`,
  `host.arch`, `process.runtime.name` and `process.runtime.version`), the variable `OTEL_RESOURCE_ATTRIBUTES` and the
  attributes of the caller, in rising precedence, and names the service by the argument, `OTEL_SERVICE_NAME` or
  `service.name`. The attribute list is read by hand, percent-decoded as strict UTF-8, bounded by the limits of
  kollect and refused with a constant message that never repeats a value.
- **A shutdown task for the metric exporter of kollect.** `metricExporter(exporter, flushTimeout, name)` stops the
  exporter in `ShutdownPhase.Flush`, so the last snapshot of every earlier phase leaves the process and every point
  stays accounted for.
- **Typed reads of data sizes and UUIDs.** `Environment.dataSize(name, min, max)` and `Environment.uuid(name)` and the
  schema settings `dataSize()` and `uuid()` read through the new converters of Kore 2.4.1.

### Changed

- **Built on Kore 2.4.1, konfetti 1.0.1 and kollect 1.0.1.**

## 1.0.0 - 2026-11-22

The first version of Knot: the system modules of kide as an open source library under the Apache License 2.0, for
Kotlin on the JVM and on `wasmJs`, built with Kreate 5.4.1 on Kore 2.3.2, konfetti 1.0.0 and kollect 1.0.0 and
published to `https://repo.davils.com`. The code comes from `kide-common-system` and is hardened on the way.

### Added

- **The build** of Knot with Kreate 5.4.1: one convention plugin per concern, strict dependency locking and
  verification, Detekt with every rule over main and test sources, Kover at 97 %, the API and semver check, Trivy,
  the standards audit of the HTML standard, RFC 9562 and Unicode, and the assurance gate.
- **`knot-platform`**: `OperatingSystem`, `Architecture` and `Platform` with the identifiers Kreate packages native
  binaries under, and `Host`, which detects the runtime (JVM, browser window, dedicated, shared and service workers,
  Node.js, Deno and Bun), the system and the architecture behind it with the source of each fact, the version of the
  runtime and whether a browser runs on a mobile device. `Host.detailed()` asks Chromium for the architecture through
  the high entropy client hints, `Host.detect(hostProbe { })` reads facts gathered elsewhere, such as the user agent
  of a request, and `PlatformLimits` bounds every reported fact.
- **`knot-environment`**: the `environment { }` DSL with layers of environments, maps and dotenv texts, overrides,
  defaults, removals, aliases, inclusion and exclusion filters, required variables, prefixed declarations, a scope,
  case-insensitive names, expansion of `${NAME}` and `${NAME:-default}`, snapshots and an optional POSIX grammar of
  names; typed reads such as `int`, `duration`, `enum`, `list` and `map` on Kore's converters; and declarative
  `EnvironmentSchema`s that load and check every setting at once and describe them without reading a value.
- **`knot-shutdown`**: the coordinated shutdown with five phases, tasks that run side by side after the tasks they wait
  on, a grace period, task timeouts, cancellation and abandonment, registrations at run time, exit codes, reports, a
  state flow, events on the topic `knot.shutdown`, meters, spans and health, and the tasks that drain and close a
  konfetti event bus. On the JVM, `install` adds the shutdown hook, the fatal error trigger and the watchdog that halts
  an overrunning shutdown, behind the `ShutdownRuntime` of the JVM.
- **`knot-failure`**: the failure handler with classification, a storm guard per fingerprint, crash reports handed to
  `FailureSink`s of the application in plain text or JSON, a flow of failures, events on the topic `knot.failure`,
  meters, spans, health, the tasks of a shutdown that drain and close it, and on the JVM the handler of uncaught
  exceptions and the exit through a process installation.
- **`knot-testing`**: host probes of every runtime, a recording sink, failing and slow sinks, test failures, ready
  shutdown reports and, on the JVM, a recording shutdown runtime.
- **`knot-bom`**: the bill of materials of every module.

### Changed

- Compared with `kide-common-system-platform`, types lost their `Kide` prefix, the environment moved into a module of
  its own and holds environment variables only, and the fake environment of the tests is replaced by the DSL. On the
  `wasmJs` target the operating system is `OperatingSystem.Wasm`, while `Platform.current` is the native platform of
  the machine behind Node.js, Deno and Bun.
- Compared with `kide-common-system-failure`, crash reports are no longer written to disk: a `FailureSink` of the
  application receives each report with a suggested file name, and keeping or deleting reports is the sink's choice.
  Failure ids are version 7 UUIDs, the storm guard keeps a bounded number of fingerprints without losing suppressed
  counts, failures of sinks, rules and renderers are contained, and a fatal failure ends the process exactly once.
- Compared with `kide-common-system-shutdown`, creating a shutdown has no effect outside it until `install` binds it to
  the JVM, the shutdown runs in children of the caller's scope, the number of tasks and dependencies is bounded, and a
  task that another one waits on cannot be unregistered.
- Logging was replaced by events, meters and spans; Knot has no logger.
