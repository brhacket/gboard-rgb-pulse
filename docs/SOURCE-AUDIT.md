# Source review and UI cleanup

## CI build update

GitHub Actions run [36931162569](https://github.com/brhacket/gboard-rgb-pulse/actions/runs/36931162569)
successfully ran the full Java and source-contract suite, compiled the APK,
verified its archive and signature, and published the
[35.0 test release](https://github.com/brhacket/gboard-rgb-pulse/releases/tag/test-35-36931162569-1).
Build commit: `c468044`. The APK uses a new test signing key; uninstall an older
module before installing it (module settings will be lost). Device/LSPosed
behavior and performance have not been verified. The local-workspace limitations
below describe the initial review, before this successful remote build.

## Scope and limitations

Reviewed the checked-out v34 source. Attempted to download the published
`Gboard-RGB-Pulse-34.0.apk` using both `gh release download` and its HTTPS release
URL. GitHub's asset server returned EOF/TLS errors; the release binary was **not**
available for inspection. The checked-in v33 APK has not been modified.

No JDK, Android SDK, emulator or LSPosed device is available in this workspace.
Attempts to install build tools failed due to network connection/TLS errors.
These changes are not a verified release and no new APK has been built.

## Fixed in source

- Zero-valued row/origin coordinates no longer act as missing-coordinate
  sentinels. Body and parent coordinates are selected consistently using map
  presence. Shared wave math now has a pure Java regression test.
- The preview uses actual tap waves and expires them before drawing. Removed
  the fixed middle-row highlight and low-contrast simulated tile legends.
- Disabled previews no longer create waves, draw trails or schedule demo ticks.
  Side waves request animation frames independently of shader availability.
- Preview paint/rectangle objects and character labels are reused, and font
  configuration no longer runs once per key per frame.
- Disposing an effect renderer resets shader initialization state for reuse.
- Initial spinner callbacks no longer write unchanged settings or reset effects.
- General controls moved to the top. Row waves, trails and typography are grouped
  separately. Obsolete tile-color and retro-preset controls removed; legacy
  preferences remain intact. Descriptions now reflect stock backgrounds and
  white waves. Reset requires confirmation. Controls have accessibility labels
  and larger minimum touch targets.
- Partial build downloads no longer become cached dependencies. An incomplete
  signing identity fails explicitly instead of silently replacing a key.
  Local signing material is ignored by Git.

## Validation

Passed locally:

- `python3 RGBPulse/tests/regression_contract_test.py`
- `python3 RGBPulse/tests/source_contract_test.py`
- `python3 RGBPulse/tests/material_contract_test.py`
- `bash -n RGBPulse/build.sh RGBPulse/test.sh`
- `git diff --check`

Not run: Java policy suite (including new `WavePolicyTest`), APK compilation,
APK signature verification, visual UI checks, performance benchmarks and device
integration. Source-contract tests are guardrails, not proof of runtime behavior.

## Before release

1. Run `bash RGBPulse/test.sh` with JDK 11+.
2. Build with the documented Android tools and a trusted signing identity.
3. Verify the APK and compare its signing certificate with the installed release.
   A fresh local key cannot update an existing differently signed installation.
4. On Android 13+ with LSPosed/Vector, test portrait/landscape, the top row at zero,
   taps at both edges, overlapping waves, space/shift/enter, glide cancellation,
   disabling effects, background/resume, and imported fonts.
5. Check font scaling, screen-reader labels, settings persistence, and Gboard
   scope/restart behavior. Profile frame time and allocations on device.

Remaining review candidates: corrupted preference types currently abort the rest
of configuration loading; wave state is process-global across controllers;
fixed row tolerance may need tuning for very compact layouts. No claim is made
that every error has been found.
