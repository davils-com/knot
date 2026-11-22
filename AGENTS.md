# AGENTS.md

Binding rules for every agent and every person working in this repository. Knot holds the system building blocks of
Davils applications, the system modules of kide published as a library: the host and runtime a program runs on, its
environment variables, its coordinated shutdown and the handling of its unexpected failures, for Kotlin on the JVM and
on WebAssembly (`wasmJs`). It reports through events on [konfetti](https://github.com/davils-com/konfetti) topics and
meters, spans and health in [kollect](https://github.com/davils-com/kollect), is built on
[Kore](https://github.com/davils-com/kore), follows the rules of Kore and is published under the Apache License 2.0,
with security first.

## Repository

| Module | Artifact | Purpose |
|---|---|---|
| `knot-platform` | `com.davils:knot-platform` | The host: the runtime (JVM, browser window, dedicated, shared and service workers, Node.js, Deno, Bun), the system and the architecture behind it with the source of each fact, and the native platforms Kreate packages binaries under |
| `knot-environment` | `com.davils:knot-environment` | Environment variables: the live process environment, the `environment { }` DSL with layers, dotenv texts, overrides, defaults, aliases, filters, scopes and expansion, typed reads and declarative `EnvironmentSchema`s |
| `knot-shutdown` | `com.davils:knot-shutdown` | The coordinated shutdown: phases, ordered tasks, budgets, reports, exit codes and telemetry in common code, and on the JVM the shutdown hook, the fatal error trigger and the watchdog in package `process` |
| `knot-failure` | `com.davils:knot-failure` | The failure handler: classification, the storm guard, crash reports rendered as text or JSON and handed to `FailureSink`s of the application, and on the JVM the handler of uncaught exceptions |
| `knot-testing` | `com.davils:knot-testing` | Test support: host probes of every runtime, recording, failing and slow sinks, test failures, ready shutdown reports and, on the JVM, a recording shutdown runtime |
| `knot-bom` | `com.davils:knot-bom` | The bill of materials of every module |
| `build-logic` | - | One convention plugin per Kreate concern, composed by `knot.kotlin-library` for a library module, `knot.bom` for the BOM and `knot.root` for the root project; they hold nothing but `plugins { }` and `kreate { }` |

`knot-platform` and `knot-environment` build on `kore-core` alone. `knot-shutdown` builds on `kore-core`,
`konfetti-core`, `kollect-core` and `kotlinx-coroutines-core`, and `knot-failure` on `knot-shutdown`. `knot-testing`
builds on every library module, `kore-testing` and `konfetti-testing`.

The root package of a module is `com.davils.knot.<module without knot->`, such as `com.davils.knot.shutdown`, and its
Java module has the same name. Every library module declares it in `src/jvmMain/java/module-info.java` and exports
only packages with public API, never an `internal` package. `knot-testing` has no `module-info.java`, because
`kore-testing` is an automatic module.

Knot uses Kore wherever Kore has the building block: `ValidatingBuilder` and `@DavilsDsl`, validation and violation
codes, `Limits`, `SafeText`, `immutableList()`, `Converters`, `contained` and `attempted`, the UUID generators and the
`Davils*` annotations. Never write a second version of something Kore has; what Knot needs from Kore and Kore does not
offer yet is added to Kore.

Knot is ported from the `kide-common-system` modules of kide. A module is ported, not redesigned, but it follows every
rule below: names lose their `Kide` prefix, public API gets KDoc, input is bounded, every message escapes what it shows
and nothing touches the disk.

## Targets

Every library module is Kotlin Multiplatform with two targets, `jvm` and `wasmJs`, like Kore. The Wasm tests run under
Node.js; the published klib runs in the browser, in web workers, under Deno and under Bun as well.

- Code lives in `src/commonMain/kotlin` unless it needs the JVM. What only the JVM offers lives in
  `src/jvmMain/kotlin`: the `process` package of `knot-shutdown`, `installUncaughtHandler` and `exitThrough` of
  `knot-failure` and the recording runtime of `knot-testing`. The `wasmJs` target has no shutdown hook, no handler of
  uncaught exceptions and no way to end its process; an application there calls `Shutdown.initiate` itself.
- A platform difference hides behind an `internal expect fun` in `<Role>.kt` with its `actual` in `<Role>.jvm.kt` and
  `<Role>.wasmJs.kt`, such as `HostFacts` of `knot-platform`, `HostVariables` of `knot-environment`, `Guard` and
  `ResourceClosing` of `knot-shutdown` and `FailureFrames` of `knot-failure`. Knot declares no `expect` or `actual`
  class: they are a Beta language feature.
- The detection of the host is common code over a `HostProbe` of raw facts; only gathering those facts is platform
  code. A new runtime or a new fact is detected in common code and tested with probes on both targets.
- JavaScript interop is written with `js("...")` or `@JsFun` in `src/wasmJsMain/kotlin` only, behind a file-level
  `@file:OptIn(ExperimentalWasmJsInterop::class)`. Every access to `process`, `Deno`, `Bun`, `window`, `navigator` and
  the worker scopes is guarded, so that it gives nothing instead of failing in a runtime that lacks it.

## Verify

```bash
./gradlew check
```

Nothing is done until it is green. `check` compiles with warnings as errors (Kotlin with progressive mode and extra
warnings, javac with `-Xlint:all -Werror`), runs Detekt with type resolution, the Kreate rule set and `allRules` over
the main and the test sources, the unit tests on the JVM and on Node.js, the process tests of the shutdown in child
JVMs, Kover verification at 97 % of lines, branches and instructions, the API and semver check, the license header
check, the changelog check, every Trivy scan, the standards audit and the Kreate assurance gate, which blocks on every
finding. The gate reads OSV.dev and deps.dev and Trivy downloads its database, so `check` needs the network. The build
runs on JDK 25; `gradle/gradle-daemon-jvm.properties` pins the daemon to it and toolchains are never downloaded.

To build against a local checkout of Kore, konfetti or kollect, such as a branch that is not published yet, substitute
it for the published artifact without changing a file:

```bash
./gradlew --include-build ../kore check
```

## Code rules

Enforced by Detekt with the Kreate rule set, the `libraries` rule set, `allRules` and warnings as errors, without a
baseline, for main and test sources alike. Build scripts and `build-logic` follow the same rules, although Detekt does
not analyse them.

- **License header first.** Every `.kt`, `.kts`, `.java`, `.properties` and YAML file starts with the Apache 2.0
  header, byte for byte as in the existing files; only the files Gradle generates in `gradle/` are exempt.
  `kreateLicenseHeaderCheck` and Detekt enforce it.
- **No comments.** No `//`, no `/* */` other than the license header. Names, types and tests carry the intent.
- **KDoc on public API only**, with `@param`, `@return`, `@throws` and `@since`, and never on internal or private
  declarations; protected members of public open or abstract classes are API and get KDoc. A new declaration gets
  the version it first ships in; existing `@since` values never change. KDoc states what a caller must know:
  behaviour on invalid input, the limits that apply, thread safety, the targets it exists on and every security
  consideration.
- **No `else`.** Return early; a `when` over a sealed type or an enum is exhaustive without `else ->`. At most two
  `return` statements per function.
- **At most two chained calls.** Name intermediate results and computed conditions.
- **Named arguments** wherever a call passes two or more values of which the meaning is not obvious from the call.
- **One top-level type per file**, named after it; a file of functions is named after its role, such as
  `EnvironmentReads.kt`.
- **No package for a single file.** A subpackage holds at least two files. A file that would stand alone in a
  package lives in the parent package instead. Exceptions are the root package of a module and an `internal` package.
- **No `!!`, no `lateinit`.** Model absence as a type or return early.
- **Explicit API.** Everything is `internal` unless a consumer needs it. A type only Knot creates, such as a built
  environment, a report or a registration, has an `internal` constructor.
- **Names.** No type, file, function or property carries the name of the library or of kide as a prefix: `Shutdown`,
  never `KnotShutdown` or `KideShutdown`. The package says where a declaration comes from. The only prefix is
  `Davils` on the annotations of Kore. The ids of the Gradle plugins in `build-logic` keep their `knot.` prefix.
- **`@DavilsInternal`** API of Kore is used only through a local or file-level `@OptIn(DavilsInternal::class)`, and
  every such use is listed under [Kore internals](#kore-internals).
- **Immutable values.** Public types are immutable; public collections are copied with `immutableList()`,
  `immutableSet()` or `immutableMap()` of Kore, never stored from `toList()` or `+`. `immutableSet()` keeps no order:
  what is shown in order is sorted first.
- **No public `data class`.** It freezes `copy` and `componentN` into the binary API. Write a regular class with
  `equals`, `hashCode` and `toString` and list it in the `UseDataClass` excludes of `detekt.yaml`; `value class` stays
  allowed.
- **Explicit return types** on public functions with an expression body or a result; a block body that returns
  nothing omits `: Unit`, which the compiler rejects as redundant.
- **Kotlin first**, Java only where Kotlin offers no equivalent, and only in `jvmMain`. Suspending calls are the
  primary API and configuration is a DSL of `ValidatingBuilder`s. Experimental Kotlin API is allowed only through a
  local `@OptIn` and never appears in a public signature.
- **Nothing global.** No mutable singletons, no static configuration and no threads or timers of the library's own,
  outside the sanctioned exceptions, each documented in its KDoc:
  - the host and the platform of the running program, read once into `Host.current`, `Platform.current` and
    `OperatingSystem.current`;
  - the live process environment of `Environment.SYSTEM`;
  - `ShutdownRuntime.Jvm` in the `process` package of `knot-shutdown`, the one place that adds the shutdown hook
    thread, the daemon timer of the watchdog, the default handler of uncaught exceptions and calls `exit` and `halt`,
    and only after `install` or `installUncaughtHandler` asked for it. Detekt's `ExitOutsideMain`,
    `ForbiddenMethodCall` and `ForbiddenImport` exclude that file and no other main code; only the probe of the
    process tests shares the first two excludes.

  Creating a shutdown or a failure handler has no effect outside it. Its work runs in children of the scope the
  caller hands it; Knot never chooses a dispatcher of its own.
- **No logging.** Knot has no logger and no log types. Telemetry runs only through the events on the topics
  `knot.shutdown` and `knot.failure`, the meters, spans and health of kollect and the flows and reports of each
  instance; an application maps them to its own logs.

## Security rules

What the runtime reports and everything a caller declares may come from an attacker: the facts of the host, such as a
user agent, the names and values of environment variables, dotenv texts, the names of tasks, sinks and origins, and the
failures and messages that reach a failure handler. Knot is built so that a mistake of a caller fails closed.

- **Limits before work.** Every entry point checks its input against `Limits` of Kore or a limit object of its
  module, such as `PlatformLimits`, `EnvironmentLimits`, `ShutdownLimits` or `FailureLimits`, before it does work that
  grows with the input. A declaration beyond a limit is not kept but reported once; nothing is truncated silently. A
  new limit goes into the limit object of its module, never into a private constant.
- **No input decides the complexity.** Grammars are written by hand or as `NameRule`s of Kore, never as regular
  expressions. Maps keyed by input, such as the fingerprints of the storm guard, are bounded by a limit. Cause chains
  are walked to a bounded depth and are safe against cycles, and expansions are bounded in depth and length.
- **Unknown means unsupported.** A system or an architecture that matches nothing is `Unknown` or `null`; Knot never
  guesses a platform, a 32-bit machine never resolves to a 64-bit build, and a native platform is derived only from an
  authoritative source such as the JVM, `process` or `Deno.build`, never from a user agent or client hints.
- **Escape untrusted text exactly once, at the sink.** Whatever shows text that may come from input, such as an
  exception message, a crash report or a `toString`, writes it through `SafeText` of Kore. Canonical values with a
  restricted alphabet, such as platform identifiers, are shown as they are. Events show names, numbers and simple type
  names only.
- **Values of environment variables are secrets.** A value never appears in a message, a violation, a `toString`, a
  report or a schema description; a `toString` shows the names and `SafeText.REDACTED` in place of every value.
- **No raw input in exceptions.** Messages are constant text plus escaped, bounded fields; a refused name is reported
  without the name.
- **Knot never touches the disk.** No module reads or writes a file, a directory or a path. A dotenv text is a string
  the application read; a crash report reaches a file, a log or a remote service only through a `FailureSink` of the
  application, which also owns its retention.
- **Foreign code is contained.** Shutdown tasks, sinks, renderers, classification rules and observers run inside
  `contained` or `attempted` of Kore. What they throw is counted and reported, never rethrown into the shutdown and
  never handed back to the failure handler as another failure. Fatal errors pass through.
- **Every wait is bounded.** A shutdown has a grace period, every task a timeout and a cancellation grace, every sink a
  timeout, and the watchdog halts an overrunning shutdown when the installation asks for it. A fatal failure ends the
  process exactly once.
- **Hide what Java must not reach.** Kotlin's `internal` is `public` in the bytecode, so a Java caller on the module
  path reaches every internal class of an exported package. A class whose misuse weakens a guarantee lives in an
  `internal` package that `module-info.java` does not export.
- **Secure randomness only.** No `java.util.Random`, `kotlin.random.Random` or `ThreadLocalRandom`; failure ids are
  version 7 UUIDs of Kore.
- **Every sink has an injection test.** A renderer is tested with `HostileFragments` and `isSafeToShow` of
  `kore-testing`; a sensitive value has a property test that it appears nowhere, and every limit has a test at the
  limit and one past it.

## Tests

- Kotest `FunSpec` only, Kotest assertions only, no mocking library; fakes are environments declared with
  `environment { }`, host probes, the sinks and runtimes of `knot-testing` or live next to the tests that use them.
  Shared test support lives in `knot-testing`, never in main code.
- One `context` per behaviour or function; test names read as sentences.
- Tests live in `src/commonUnitTest/kotlin`, which runs on the JVM and on Wasm, in the package of the code they test.
  A test that needs the JVM lives in `src/jvmUnitTest/kotlin`; a test of common code stays common wherever it can.
  `src/test` is rejected by the build. A `fixture` folder holds at least two files.
- Time is virtual: shutdowns and failure handlers take the clock and the time source of the test scheduler of Kotest
  (`coroutineTestScope = true`).
- What ends a process is tested in a child JVM: the process tests of `knot-shutdown` start `ShutdownProbe` with a
  scenario and check its exit code and output.
- Property tests with adversarial generators for every parser and every security invariant.
- Every change ships with its tests; a fixed weakness ships with a test that would have caught it.

## Build

- A library module applies exactly one plugin, `id("knot.kotlin-library")`, and declares nothing but its description
  and its dependencies, through the configurations of the source sets such as `commonMainApi`; the BOM applies
  `id("knot.bom")` and declares a constraint for every library module. `kore-testing` reaches every test suite through
  `knot.testing`. Detekt, Kover, Kotest, Trivy, the compiler and publishing are configured only in `build-logic`,
  through Kreate.
- `build-logic` holds no Kotlin classes, no tasks of its own and no Gradle API calls beyond reading the version
  catalog. What Kreate cannot express yet is added to Kreate, never programmed here.
- Every plugin in `build-logic` configures one Kreate concern and applies `knot.base`:

  | Plugin | Configures |
  |---|---|
  | `knot.base` | The Kreate plugin, the group and the version |
  | `knot.dependency-locking` | Strict locking of every configuration |
  | `knot.git-hooks` | The Git hooks |
  | `knot.trivy` | Every Trivy scan |
  | `knot.assurance` | The assurance gate, its members and its `ignore` rules |
  | `knot.standards` | The standards Knot follows, reviewed in `standards.lock` |
  | `knot.platform` | The JDK, the Kotlin compiler, the targets and the Java module name |
  | `knot.web-tools` | Node.js and Yarn for the Wasm target, from the repositories of the settings |
  | `knot.testing` | Kotest, the test suites and `kore-testing` |
  | `knot.detekt` | Detekt and its reports |
  | `knot.coverage` | Kover and its verification |
  | `knot.api-validation` | The API dump and the semver check |
  | `knot.docs` | Dokka |
  | `knot.publish` | The POM, the release guard and the Davils repository |
  | `knot.archives` | The legal files and the manifest of every archive |
  | `knot.tool-classpaths` | The version floors on the class paths of the build tools |
  | `knot.changelog` | The changelog check |
  | `knot.license-header` | The license header check |
  | `knot.bom` | The Java platform of the BOM and its publication |

  `knot.kotlin-library` composes them for a module and adds only the class paths the assurance gate reads;
  `knot.root` composes them for the root project. A new concern gets a plugin of its own, never a block in a
  composing plugin.
- A new module is added to `settings.gradle.kts`, to the members of `knot.assurance`, to the constraints of `knot-bom`
  and to `.github/kotlin-workflow.yml`: its reproducible tasks and artifacts, `publish.tasks`,
  `release.publications.list` and `attest.subjects`. A module missing from the release list is silently left out of
  a release.
- Dependencies are declared in `gradle/libs.versions.toml`. Libraries of JetBrains, Kore, konfetti and kollect may be
  added when they are needed; every other dependency needs the approval of a maintainer.
- Only `repo.davils.com` serves `com.davils` and its subgroups, through `exclusiveContent`. Everything else comes
  from Maven Central, except the Gradle plugins `kotlin-dsl`, `kotlinx-benchmark-plugin` and
  `kotest-framework-plugin-gradle`, which only the Gradle Plugin Portal has, and Node.js and Yarn, which come from
  `nodejs.org` and the GitHub releases of Yarn. `gradle/verification-metadata.xml` names the Node.js archive of every
  platform the pipeline runs on. Nothing is ever resolved from `mavenLocal()`.
- After a dependency changes, refresh the locks and the verification metadata in one run and review what changed:

  ```bash
  ./gradlew kreateResolveAndLockAll :build-logic:dependencies --write-locks --write-verification-metadata pgp,sha256 --export-keys --no-configuration-cache
  ```

- Knot builds only with released versions of Kreate, Kore, konfetti and kollect. `gradle/verification-metadata.xml`
  trusts `com.davils` artifacts only by the signature of the Davils release key
  `36F578E8AD2D4391E0FA6C18154C625FAD370063`; a snapshot is never put into the catalog, and no trust rule for unsigned
  snapshots is ever added.
- A finding of the assurance gate that is a known, harmless property of a dependency gets an `ignore` rule in
  `build-logic/src/main/kotlin/knot.assurance.gradle.kts`, with a reason, the exact module and an expiry date no more
  than six months ahead. CRITICAL and HIGH findings can never be ignored.
- Every standard Knot follows is declared in `build-logic/src/main/kotlin/knot.standards.gradle.kts` and reviewed with
  `./gradlew kreateStandardsReview --standard=<id> --note="..."`. Only standards with stable, structured sources are
  tracked, such as the HTML standard, RFCs and Unicode; a repository that is released too often or not tagged is not.

## Kore internals

Knot opts into these `@DavilsInternal` declarations of Kore; Kore changes them only in a major version: `SafeText.text`,
`SafeText.quoted`, `SafeText.isSafe`, `SafeText.REDACTED`, `immutableList`, `immutableSet` and `immutableMap`. A new
use is added here and to the table of Kore's `AGENTS.md`.

## Public API

The `api/*.api` dumps are the contract. `kreateApiCheck` fails on every unrecorded change and `kreateApiSemverCheck`
on a version that does not match it. A deliberate change is recorded with `./gradlew kreateApiDump` and gets a
`CHANGELOG.md` entry. A breaking change needs a major version and a commit marked as breaking. Deprecate before
removing: `@Deprecated` with a `ReplaceWith`, for at least one minor release. Every module carries the same version.
The platform and runtime identifiers, the event topics, the meter names and the values of the violation codes never
change once released.

## Releases

The version lives in `gradle.properties` and nowhere else; `kreateReleaseGuard`, which runs before every
publication, refuses a build whose resolved version differs from it, so no environment variable can change what is
published. Every build without a release tag is a `-SNAPSHOT`. A release is a tag without a `v`, such as `1.0.0`,
that equals that version, sits on a commit of `main` and is built without any snapshot tooling; the guard refuses
every other combination before anything is published. Snapshots go to `https://repo.davils.com/snapshots`, releases
to `https://repo.davils.com/releases`, and never to Maven Central. The pipeline publishes with
`publishAllPublicationsToDavilsRepository` and `--no-configuration-cache`, with the credentials in
`DAVILS_MAVEN_NAME` and `DAVILS_MAVEN_SECRET`, as kotlin-workflow hands them to Gradle, and the signing key in
`ORG_GRADLE_PROJECT_signingInMemoryKey`, `…KeyId` and `…KeyPassword`; a release without a signing key fails. The
GitHub release is titled with a `v`, such as `v1.0.0`, and carries the section of `CHANGELOG.md` as its notes. A
release of Knot needs released versions of Kore, konfetti and kollect.

## Git

Work on `develop`, release from `main`. Conventional Commits in English with the module as scope, such as
`feat(shutdown): ...`, `fix(environment): ...` or `build: ...`. Small commits, one concern each. Commit only when
asked. No `Co-Authored-By` trailer and no other attribution to tools or AI assistants.
