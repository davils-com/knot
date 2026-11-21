# Security Policy

## Supported Versions

Security fixes are released for the latest minor version of the current major release.

| Version | Supported |
|---------|-----------|
| 1.0.x   | Yes       |
| < 1.0   | No        |

## Reporting a Vulnerability

**Do not open a public issue for a security problem.**

Report it privately through GitHub's
[private vulnerability reporting](https://github.com/davils-com/knot/security/advisories/new),
or by email to development@davils.com.

Please include:

- the affected Knot version, module and JDK or Node.js version,
- a description of the impact — what an attacker gains,
- the steps to reproduce it, ideally as a minimal Kotlin snippet or test,
- any mitigation you are already aware of.

We acknowledge a report within three working days and agree on a disclosure date with you.

## Scope

Knot is a library: it runs with the privileges of the application that uses it. It treats the names a runtime reports
for its operating system and architecture, the names and values of environment variables and every declaration of an
environment as untrusted.

The following are **in scope**:

- a reported name or a declaration that makes Knot use memory or CPU beyond `Limits` of Kore or `EnvironmentLimits`,
  or throw where its KDoc promises `null` or a violation,
- a name that resolves to a platform it does not describe, such as a 32-bit machine resolving to a 64-bit build,
- the value of an environment variable that appears in a message, a violation, a report or a `toString`,
- untrusted text that reaches a message or a `toString` unescaped, so it can forge log lines or control a terminal,
- a removed variable that can still be read through a built environment.

The following are **not** vulnerabilities in Knot:

- a vulnerability in the JDK, Node.js, the Kotlin standard library or Kore — report those to the respective project,
- an application that logs the value it reads with `Environment.variable`; the value is returned as it is,
- an application that trusts `Environment.SYSTEM` although another part of the same process can change it.

## Known Limitations

- `Environment.SYSTEM` reads the process live. On the JVM the environment is fixed when the process starts; under
  Node.js, code of the same process may change `process.env` at any time, and a built environment sees the change.
- The operating system and the platform of the running program are read once, on first use of
  `OperatingSystem.current` and `Platform.current`, and kept.
- On the `wasmJs` target the operating system is always `OperatingSystem.Wasm`, in a browser and under Node.js alike,
  and `Platform.current` is `null`. In a browser there is no environment variable: `Environment.SYSTEM` is empty.
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
