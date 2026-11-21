# Third-Party Material

No published artifact of Knot contains third-party code. Knot is ported from the `kide-common` modules of kide,
Copyright Davils, and builds on [Kore](https://github.com/davils-com/kore), Copyright 2026 Davils, under the same
Apache License 2.0.

The build and the tests depend on libraries under their own licenses; `gradle/verification-metadata.xml` records
each of them, and the Trivy license scan of `./gradlew check` checks them.
