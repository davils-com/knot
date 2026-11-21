# AGENTS.md

Binding rules for every agent and every person working in this repository. Knot holds the common building blocks of
Davils applications, the system modules of kide published as a library: the platform a program runs on and its
environment variables, for Kotlin on the JVM and on WebAssembly (`wasmJs`). It is built on
[Kore](https://github.com/davils-com/kore), follows the rules of Kore and is published under the Apache License 2.0,
with security first.

## Repository

| Module | Artifact | Purpose |
|---|---|---|
| `knot-platform` | `com.davils:knot-platform` | The operating system, the 64-bit architecture and the supported platform a program runs on, resolved from what the JVM or Node.js reports, and the platform identifiers Kreate packages native binaries under |
| `knot-environment` | `com.davils:knot-environment` | Environment variables: the live process environment, an empty one and the `environment { }` DSL that layers environments, overrides and removes variables and scopes them to a prefix |
| `build-logic` | - | One convention plugin per Kreate concern, composed by `knot.kotlin-library` for the modules and `knot.root` for the root project; they hold nothing but `plugins { }` and `kreate { }` |

The root package of a module is `com.davils.knot.<module without knot->`, such as `com.davils.knot.platform`, and its
Java module has the same name. Every module declares it in `src/jvmMain/java/module-info.java` and exports only
packages with public API. A module name holds a single word after `knot-`, so the package name needs no conversion.

Knot uses Kore wherever Kore has the building block: `ValidatingBuilder` and `@DavilsDsl`, validation and violation
codes, `Limits`, `SafeText`, `immutableList()` and the `Davils*` annotations. Never write a second version of
something Kore has; what Knot needs from Kore and Kore does not offer yet is added to Kore.

Knot is ported from the `kide-common` modules of kide. A module is ported, not redesigned, but it follows every rule
below: names lose their `Kide` prefix, public API gets KDoc, input is bounded and every message escapes what it shows.

## Targets

Every module is a Kotlin Multiplatform library with two targets, `jvm` and `wasmJs`, like Kore. The Wasm tests run
under Node.js; the published klib runs in the browser as well, where there is no platform and no environment variable.

- Code lives in `src/commonMain/kotlin` unless it needs the JVM. What only the JVM can offer lives in
  `src/jvmMain/kotlin`.
- A platform difference hides behind an `internal expect fun` in `<Role>.kt` with its `actual` in `<Role>.jvm.kt` and
  `<Role>.wasmJs.kt`, such as `HostNames` of `knot-platform` and `HostVariables` of `knot-environment`. Knot
  declares no `expect` or `actual` class: they are a Beta language feature.
- JavaScript interop is written with `js("...")` or `@JsFun` in `src/wasmJsMain/kotlin` only, behind a file-level
  `@file:OptIn(ExperimentalWasmJsInterop::class)`, and every access to `process` is guarded, so that it gives nothing
  instead of failing in a browser.

## Verify

```bash
./gradlew check
```

Nothing is done until it is green. `check` compiles with warnings as errors (Kotlin with progressive mode and
extra warnings, javac with `-Xlint:all -Werror`), runs Detekt with type resolution, the Kreate rule set and
`allRules`, the unit tests on the JVM and on Node.js, Kover verification at 97 % of lines, branches and
instructions, the API and semver check, the license header check, the changelog check, every Trivy scan and the
Kreate assurance gate, which blocks on every finding. The gate reads OSV.dev and deps.dev and Trivy downloads its
database, so `check` needs the network. The build runs on JDK 25; `gradle/gradle-daemon-jvm.properties` pins the
daemon to it and toolchains are never downloaded.

To build against a local checkout of Kore, such as a branch that is not published yet, substitute it for the published
artifact without changing a file:

```bash
./gradlew --include-build ../kore check
```

## Code rules

Enforced by Detekt with the Kreate rule set, the `libraries` rule set, `allRules` and warnings as errors, without
a baseline. Build scripts and `build-logic` follow the same rules, although Detekt does not analyse them.

- **License header first.** Every `.kt`, `.kts`, `.java`, `.properties` and YAML file starts with the Apache 2.0
  header, byte for byte as in the existing files; only the files Gradle generates in `gradle/` are exempt.
  `kreateLicenseHeaderCheck` and Detekt enforce it.
- **No comments.** No `//`, no `/* */` other than the license header. Names, types and tests carry the intent.
- **KDoc on public API only**, with `@param`, `@return`, `@throws` and `@since`, and never on internal or private
  declarations; protected members of public open or abstract classes are API and get KDoc. A new declaration gets
  the version it first ships in; existing `@since` values never change. KDoc states what a caller must know:
  behaviour on invalid input, the limits that apply, thread safety and every security consideration.
- **No `else`.** Return early; a `when` over a sealed type or an enum is exhaustive without `else ->`.
- **At most two chained calls.** Name intermediate results and computed conditions.
- **Named arguments** wherever a call passes two or more values of which the meaning is not obvious from the call.
- **One top-level type per file**, named after it; a file of functions is named after its role, such as
  `Environments.kt`.
- **No package for a single file.** A subpackage holds at least two files. A file that would stand alone in a
  package lives in the parent package instead. Exceptions are the root package of a module and an `internal` package.
- **No `!!`, no `lateinit`.** Model absence as a type or return early.
- **Explicit API.** Everything is `internal` unless a consumer needs it. A type only Knot creates, such as a built
  environment, has an `internal` constructor.
- **Names.** No type, file, function or property carries the name of the library or of kide as a prefix: `Platform`,
  never `KnotPlatform` or `KidePlatform`. The package says where a declaration comes from. The only prefix is
  `Davils` on the annotations of Kore. The ids of the Gradle plugins in `build-logic` keep their `knot.` prefix.
- **`@DavilsInternal`** API of Kore is used only through a local or file-level `@OptIn(DavilsInternal::class)`, and
  every such use is listed in the AGENTS.md of Kore, so that Kore keeps it stable.
- **Immutable values.** Public types are immutable; public collections are copied with `immutableList()`,
  `immutableSet()` or `immutableMap()` of Kore, never stored from `toList()` or `+`.
- **No public `data class`.** It freezes `copy` and `componentN` into the binary API. Write a regular class with
  `equals`, `hashCode` and `toString`; `value class` stays allowed.
- **Explicit return types** on public functions with an expression body or a result; a block body that returns
  nothing omits `: Unit`, which the compiler rejects as redundant.
- **Kotlin first**, Java only where Kotlin offers no equivalent, and only in `jvmMain`. Experimental Kotlin API is
  allowed only through a local `@OptIn` and never appears in a public signature.
- **Nothing global.** No mutable singletons, no static configuration, no threads or timers of the library's own. The
  sanctioned exceptions are the platform of the running program, read once into `Platform.current`, and the live
  process environment of `Environment.SYSTEM`, both documented in their KDoc. Detekt does not enforce this rule;
  reviews do.

## Security rules

What the runtime reports and everything a caller declares may come from an attacker: the names of the operating
system and the architecture, the names and values of environment variables and the declarations of an environment.
Knot is built so that a mistake of a caller fails closed.

- **Limits before work.** Every entry point checks its input against `Limits` of Kore or a limit object of its
  module, such as `EnvironmentLimits`, before it does work that grows with the input. A declaration beyond a limit is
  not kept but reported once; nothing is truncated silently. A new limit goes into the limit object of its module,
  never into a private constant.
- **Unknown means unsupported.** A system or an architecture that matches nothing resolves to `null`; Knot never
  guesses a platform, and a 32-bit machine never resolves to a 64-bit build.
- **Escape untrusted text exactly once, at the sink.** Whatever shows text that may come from input, such as an
  exception message or a `toString`, writes it through `SafeText` of Kore. Canonical values with a restricted
  alphabet, such as platform identifiers, are shown as they are.
- **Values of environment variables are secrets.** A value never appears in a message, a violation, a `toString` or
  a report; a `toString` shows the names and `SafeText.REDACTED` in place of every value.
- **No raw input in exceptions.** Messages are constant text plus escaped, bounded fields; a refused name is reported
  without the name.
- **Hide what Java must not reach.** Kotlin's `internal` is `public` in the bytecode, so a Java caller on the module
  path reaches every internal class of an exported package. A class whose misuse weakens a guarantee lives in an
  `internal` package that `module-info.java` does not export.
- **Secure randomness only.** No `java.util.Random`, `kotlin.random.Random` or `ThreadLocalRandom`.
- **Every sink has an injection test.** A renderer is tested with `HostileFragments` and `isSafeToShow` of
  `kore-testing`; a sensitive value has a property test that it appears nowhere.

## Tests

- Kotest `FunSpec` only, Kotest assertions only, no mocking library; fakes are environments declared with
  `environment { }` or live next to the tests that use them.
- One `context` per behaviour or function; test names read as sentences.
- Tests live in `src/commonUnitTest/kotlin`, which runs on the JVM and on Wasm, in the package of the code they test.
  A test that needs the JVM lives in `src/jvmUnitTest/kotlin`; a test of common code stays common wherever it can.
  `src/test` is rejected by the build.
- Property tests with adversarial generators for every parser and every security invariant.
- Every change ships with its tests; a fixed weakness ships with a test that would have caught it.

## Build

- Every module applies exactly one plugin, `id("knot.kotlin-library")`, and declares nothing but its description
  and its dependencies, through the configurations of the source sets such as `commonMainApi`. `kore-testing` is
  added to the unit tests through the configurations that Kreate creates late. Detekt, Kover, Kotest, Trivy, the
  compiler and publishing are configured only in `build-logic`, through Kreate.
- `build-logic` holds no Kotlin classes, no tasks of its own and no Gradle API calls beyond reading the version
  catalog. What Kreate cannot express yet is added to Kreate, never programmed here.
- Every plugin in `build-logic` configures one Kreate concern and applies `knot.base`:

  | Plugin | Configures |
  |---|---|
  | `knot.base` | The Kreate plugin, the group and the version |
  | `knot.dependency-locking` | Strict locking of every configuration |
  | `knot.git-hooks` | The Git hooks |
  | `knot.trivy` | Every Trivy scan |
  | `knot.assurance` | The assurance gate and its `ignore` rules |
  | `knot.platform` | The JDK, the Kotlin compiler, the targets and the Java module name |
  | `knot.web-tools` | Node.js and Yarn for the Wasm target, from the repositories of the settings |
  | `knot.testing` | Kotest and the test suites |
  | `knot.detekt` | Detekt and its reports |
  | `knot.coverage` | Kover and its verification |
  | `knot.api-validation` | The API dump and the semver check |
  | `knot.docs` | Dokka |
  | `knot.publish` | The POM, the release guard and the Davils repository |
  | `knot.archives` | The legal files and the manifest of every archive |
  | `knot.tool-classpaths` | The version floors on the class paths of the build tools |
  | `knot.changelog` | The changelog check |
  | `knot.license-header` | The license header check |

  `knot.kotlin-library` composes them for a module and adds only the class paths the assurance gate reads;
  `knot.root` composes them for the root project. A new concern gets a plugin of its own, never a block in a
  composing plugin.
- Dependencies are declared in `gradle/libs.versions.toml`. Knot depends on `kore-core`, and its tests on
  `kore-testing`. Libraries of JetBrains and of Davils may be added when they are needed; every other dependency
  needs the approval of a maintainer.
- Only `repo.davils.com` serves `com.davils` and its subgroups, through `exclusiveContent`. Everything else comes
  from Maven Central, except the Gradle plugins `kotlin-dsl`, `kotlinx-benchmark-plugin` and
  `kotest-framework-plugin-gradle`, which only the Gradle Plugin Portal has, and Node.js and Yarn, which come from
  `nodejs.org` and the GitHub releases of Yarn. `gradle/verification-metadata.xml` names the Node.js archive of every
  platform the pipeline runs on. Nothing is ever resolved from `mavenLocal()`.
- After a dependency changes, refresh the locks and the verification metadata and review what changed:

  ```bash
  ./gradlew kreateResolveAndLockAll :build-logic:dependencies --write-locks --no-configuration-cache
  ```

  ```bash
  ./gradlew --write-verification-metadata pgp,sha256 --export-keys --no-configuration-cache check
  ```

- Knot builds only with released versions of Kreate and Kore. `gradle/verification-metadata.xml` trusts
  `com.davils` artifacts only by the signature of the Davils release key `36F578E8AD2D4391E0FA6C18154C625FAD370063`;
  a snapshot is never put into the catalog, and no trust rule for unsigned snapshots is ever added.
- A finding of the assurance gate that is a known, harmless property of a dependency gets an `ignore` rule in
  `build-logic/src/main/kotlin/knot.assurance.gradle.kts`, with a reason, the exact module and an expiry date no more
  than six months ahead. CRITICAL and HIGH findings can never be ignored.

## Public API

The `api/*.api` dumps are the contract. `kreateApiCheck` fails on every unrecorded change and
`kreateApiSemverCheck` on a version that does not match it. A deliberate change is recorded with
`./gradlew kreateApiDump` and gets a `CHANGELOG.md` entry. A breaking change needs a major version and a commit
marked as breaking. Deprecate before removing: `@Deprecated` with a `ReplaceWith`, for at least one minor release.
The platform identifiers and the values of the violation codes never change once released.

## Releases

The version lives in `gradle.properties` and nowhere else; `kreateReleaseGuard`, which runs before every
publication, refuses a build whose resolved version differs from it, so no environment variable can change what is
published. Every build without a release tag is a `-SNAPSHOT`. A release is a tag without a `v`, such as `1.0.0`,
that equals that version, sits on a commit of `main` and is built without any snapshot tooling; the guard refuses
every other combination before anything is published. Snapshots go to `https://repo.davils.com/snapshots`, releases
to `https://repo.davils.com/releases`, and never to Maven Central. The pipeline publishes with
`publishAllPublicationsToDavilsRepository` and `--no-configuration-cache`, with the credentials in
`DAVILS_MAVEN_NAME` and `DAVILS_MAVEN_SECRET`, as kotlin-workflow hands them to Gradle, and the signing key in
`ORG_GRADLE_PROJECT_signingInMemoryKey`, `…KeyId` and `…KeyPassword`; a release without a signing key fails. A
release of Knot needs a released Kore.

## Git

Work on `develop`, release from `main`. Conventional Commits in English with the module as scope, such as
`feat(platform): ...` or `fix(environment): ...`. Small commits, one concern each. Commit only when asked. No
`Co-Authored-By` trailer and no other attribution to tools or AI assistants.
