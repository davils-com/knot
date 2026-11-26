# Security Policy

## Supported Versions

Security fixes are released for the latest minor version of the current major release.

| Version | Supported |
|---------|-----------|
| 1.1.x   | Yes       |
| < 1.1   | No        |

## Reporting a Vulnerability

**Do not open a public issue for a security problem.**

Report it privately through GitHub's
[private vulnerability reporting](https://github.com/davils-com/knot/security/advisories/new),
or by email to development@davils.com.

Please include:

- the affected Knot version, module and target (JVM, or Wasm in which runtime),
- a description of the impact — what an attacker gains,
- the steps to reproduce it, ideally as a minimal Kotlin snippet or test,
- any mitigation you are already aware of.

We acknowledge a report within three working days and agree on a disclosure date with you.

## Scope

Knot is a library: it runs with the privileges of the application that uses it. It treats as untrusted the facts a
runtime reports about itself (the names of its system and architecture, its version, the user agent and the client
hints of a browser), the names and values of environment variables, dotenv texts and every declaration of an
environment, the failures and messages that reach a failure handler, the names of tasks, sinks and origins, and what
shutdown tasks, sinks, rules and renderers of the application do.

The following are **in scope**:

- a reported name or a declaration that makes Knot use memory or CPU beyond `Limits` of Kore or `EnvironmentLimits`,
  or throw where its KDoc promises `null` or a violation,
- a name that resolves to a platform it does not describe, such as a 32-bit machine resolving to a 64-bit build,
- the value of an environment variable that appears in a message, a violation, a report or a `toString`,
- untrusted text that reaches a message or a `toString` unescaped, so it can forge log lines or control a terminal,
- a removed variable that can still be read through a built environment,
- a dotenv text or an expansion of references that makes Knot use memory or CPU beyond `EnvironmentLimits`,
- a native `Platform` derived from a source that is not authoritative, such as the user agent of a browser,
- a failure handler that writes a file, or that loses, repeats or re-enters a failure because a sink, a rule or a
  renderer of the application threw,
- a crash report, an event or a `toString` that shows a message or a stack frame unescaped, or a `toString` of a
  failure, a shutdown reason or a task outcome that shows the message of an exception,
- a shutdown that runs a task twice, runs it outside its budget without reporting it, or ends the process more than
  once.

The following are **not** vulnerabilities in Knot:

- a vulnerability in the JDK, Node.js, the Kotlin standard library or Kore — report those to the respective project,
- an application that logs the value it reads with `Environment.variable`; the value is returned as it is,
- an application that trusts `Environment.SYSTEM` although another part of the same process can change it,
- an application that trusts `Host.system` or `Host.architecture` from a browser: they are hints, see
  `DetectionSource.isAuthoritative`,
- what a `FailureSink` of the application does with a crash report, such as where it writes it and how long it keeps
  it; a crash report holds messages and stack frames, so a sink that leaves the process may render them out with
  `RenderOptions`.

## Known Limitations

- `Environment.SYSTEM` reads the process live. On the JVM the environment is fixed when the process starts; under
  Node.js, code of the same process may change `process.env` at any time, and a built environment sees the change.
- The operating system and the platform of the running program are read once, on first use of
  `OperatingSystem.current` and `Platform.current`, and kept.
- On the `wasmJs` target the operating system is always `OperatingSystem.Wasm`, in a browser and under Node.js alike.
  `Host.current` tells the runtime and the machine behind it; `Platform.current` is the native platform under
  Node.js, Deno and Bun and `null` in a browser. In a browser there is no environment variable: `Environment.SYSTEM`
  is empty.
- A shutdown task that blocks a thread and ignores its cancellation keeps running after its budget; it is reported as
  abandoned. On the JVM the watchdog of a process installation halts the process when the whole shutdown overruns.
- On the JVM, `install` of `knot-shutdown` and `installUncaughtHandler` of `knot-failure` change state of the whole
  process: a shutdown hook, the default handler of uncaught exceptions and, while a shutdown runs, a timer thread.
  These are the only places where Knot touches the process; `knot-failure` never touches a file.
- The storm guard forgets the oldest windows once it tracks `FailureLimits.MAX_TRACKED_FINGERPRINTS` fingerprints, so
  a flood of distinct failures can be reported more often than `maxReports` per fingerprint.
- `@DavilsInternal` is a compile-time opt-in, not an access boundary. Kotlin `internal` is public in JVM bytecode:
  Java code and reflection reach internal declarations through their mangled names. On the module path the
  `module-info` of each module exports only packages with public API; on the class path every class is reachable.

## Verifying a Release

Releases are published to `https://repo.davils.com/releases` and signed with the release key of Davils; its
fingerprint is published in this section with the first release, so a consumer can pin it as a `trusted-key` in its
own `gradle/verification-metadata.xml`. A release also carries a signed build provenance attestation made by the
GitHub Actions run that built it:

```bash
gh attestation verify knot-platform-jvm-1.0.0.jar --repo davils-com/knot --signer-repo davils-com/kotlin-workflow
```

Snapshots in `https://repo.davils.com/snapshots` are neither signed nor attested and are not meant for production.
