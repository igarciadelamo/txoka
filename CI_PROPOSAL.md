# Follow-up: opt-in APK builds

This is a proposal, not an implemented workflow. The card appearance PR deliberately leaves
`.github/workflows/` unchanged. Existing CI builds debug and unsigned release APKs and uploads test
and coverage reports; it does not publish an APK download artifact.

The agreed trigger is the **PR label `build-apk`**, not a Git tag. Rebuild after a labelled PR's
head changes and link the exact source SHA and download location from the PR. Removing the label
should stop new preview builds. A manual retry can supplement the label.

The desired APK updates the installed app. Its application ID must remain
`es.igarciadelamo.txoka`, and its signing certificate must be compatible with the APK supplied by
the author. That key and certificate have not been identified. Do not promise an install-over
preview, generate a replacement key or change the package name to bypass the requirement.

## Separate trust boundaries

1. Build and test untrusted PR code without signing secrets or write permissions. Pin actions and
   tools, preserve the complete commit SHA and produce an immutable unsigned artifact with a digest.
2. Put the trusted signing workflow on `main` in a separate CI PR before enabling signed previews.
   Validate the triggering repository, successful workflow, run ID, artifact digest, current PR head,
   label, package identity and version before signing. Never execute files or scripts from the PR
   in a job that can read the signing key.
3. Restrict the key through a protected environment and the maintainer's agreed release policy.
   Download and parse artifacts as untrusted data. Pin signing tools and bound archive extraction.
   Do not use `pull_request_target` to check out and run PR code with secrets.
4. Publish only the intended APK, certificate fingerprint, source SHA and checksum. Keep keys,
   passwords, logs containing secrets and debug identity material out of artifacts and comments.

If signing is not configured, the build can produce an explicitly labelled test artifact, but it
must not claim it can update the author's installed APK. Release/store signing is a separate step.

## Checks to retain and extend

- Current debug/release builds, unit tests, 95% logic coverage, Detekt, ktlint and Android lint.
- Appearance codec golden vectors, malformed STYLE fallback, exact NFC byte-budget boundaries and
  old-contact compatibility.
- Room migrations on a disposable emulator, preserving notes, colours and appearance.
- English/Spanish resource parity and locale/font-scale checks; offline asset validation.
- Review suitable reusable checks from the contributor's Prebostes Taraos repository before copying
  them. Transfer the purpose and constraints, not unrelated project tooling or signing credentials.

No workflow should install over a person's existing app or wipe its data during automated tests.
Use disposable devices/emulators for destructive installation and migration exercises.
