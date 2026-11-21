# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## Unreleased

The first version of Knot: the platform and environment modules of kide as an open source library under the Apache
License 2.0, for the JVM and WebAssembly, built on Kore 2.3.2 with Kreate 5.3.0.

### Added

- `knot-platform`: `OperatingSystem`, `Architecture` and `Platform`, which resolve what the JVM reports into Linux,
  macOS or Windows on x86_64 or aarch64, parse and list the platform identifiers Kreate packages native binaries
  under, and give the operating system and the platform of the running program. Every runtime of the `wasmJs`
  target, a browser and Node.js alike, runs on `OperatingSystem.Wasm`, which `isWasm` of the operating system and of a
  platform tells apart and which has no native platform.
- `knot-environment`: `Environment` with the live process environment `Environment.SYSTEM` and `Environment.EMPTY`,
  and the `environment { }` DSL, which stacks layers on a base, overrides and removes variables, scopes them to a
  prefix and reports every broken declaration with a code of `EnvironmentViolationCodes`, bounded by
  `EnvironmentLimits`.

### Changed

- Compared with `kide-common-system-platform`, types lost their `Kide` prefix, the environment moved into a module of
  its own and holds environment variables only, without the user name, the home and the temporary directory, and the
  fake environment of the tests is replaced by the DSL.
- Reported names longer than `Limits.MAX_NAME_LENGTH` of Kore or with a character outside printable ASCII resolve to
  nothing, `win32` as Node.js reports it resolves to Windows, and the message of `Platform.requireCurrent` escapes the
  names it shows.
