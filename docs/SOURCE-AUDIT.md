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


## Explicit-Apply follow-up (36.0 test)

- The editor writes only to a private draft; Apply is the only path that writes
  the module's shared settings. Discard reloads the last applied snapshot.
- Removed automatic preview taps and idle polling, root requests and force-stop.
- Fresh installs default to disabled, no trail, no row wave and the original-font
  selection. Background animations have their own off-by-default switch.
- Animation shortcuts change only the named animation, not colors or timing.
- Row-wave creation now checks its switches; row-only animation no longer depends
  on a shader animation being active. The final frame invalidates keys to remove
  stale wave borders.
- Import/remove/reset affect the draft, not live Gboard settings. Leaving a dirty
  draft prompts before exiting. Screen rotation retains the private draft.

Visual correctness on the user's Gboard layout remains unverified; a screenshot
and device/Gboard version are needed to reproduce the remaining visual report.


## Refined ripple follow-up (37.0 test)

The primary editor now contains an interactive preview, one enable switch and two
sliders. Setup and diagnostics are collapsed; Apply/Discard are in a fixed footer.
The first Apply explicitly confirms replacing legacy RGB/font/trail settings.

The ripple is a symmetric traveling band with smooth attack and release; its
opacity is bounded and overlapping waves use the maximum, not additive bloom.
The renderer is shared by the preview and key background hook. Refined mode
bypasses the custom text/cap replacement path to preserve stock glyph layout.
The original background receives state/level/hotspot and drawable callbacks.
Paint and bounds objects are reused and light is clipped inside key bounds.

New tests cover bounded opacity, symmetric travel, compact-row isolation, smooth
expiration, disabled defaults, corrupt preference types and refined-mode limits.
Device visuals, performance and LSPosed compatibility remain unverified here.


## Gboard frame pacing and restored controls (38.0 test)

- Restored an actual EditText for testing the active keyboard. It neither opens
  automatically nor persists typed text in app saved state. Copy distinguishes
  draft preview effects from Gboard's last applied configuration.
- Restored optional background animations in a collapsed section, including
  animation catalog, color mode and primary/secondary hues. Choosing a style does
  not enable it; explicit Apply remains required. Refined mode no longer forcibly
  turns off the background preference. The preview now taps/draws the same field
  renderer used by Gboard, behind its stock-key demo.
- Removed the Gboard controller's fixed 16ms redraw gate. Every scheduled display
  callback advances side-wave state before invalidating the actual keys. Wave
  expiration no longer prevents scheduling the cleanup frame; pause also
  invalidates keys to remove cached highlights.
- Changed the per-frame pre-draw scan from forced to throttled. Touch/settle scans
  still run immediately. This reduces repeated tree discovery/hook application.
- Extended configuration and source regression checks for background opt-in/out,
  restored typing input, frame ordering and the absence of the old fixed gate.

These are source-level causes of divergence/jank, not a measured guarantee of
smoothness on every device. Actual Gboard/LSPosed frame times still need device
validation (especially 60/90/120Hz, overlapping taps and layout changes).


## Ease-out, layout transitions and confirmed restart (39.0 test)

- Ripple travel is now cubic ease-out; its broader spatial band and fade use a
  quintic smoothstep. Background wave styles (except the preserved hologram
  favorite) also use cubic travel and a quintic lifetime envelope.
- Refined keys no longer wrap/replace their original backgrounds: per-key
  ViewOverlay drawables render the clipped ripple above native key paint. Layers
  are explicitly invalidated each frame and removed on rebind/disable/dispose.
  This addresses a plausible reason for invisible ripples; actual device
  compatibility remains to be verified.
- OnGlobalLayout marks discovery dirty, so pre-draw bypasses the 120ms idle
  throttle for keyboard opening and alphabet/symbol layout swaps. It never
  blocks a frame or hides an unsupported keyboard to mask a transition.
- Apply is available even with no new edits and opens a confirmation with Save
  only, Save & restart, and Cancel. Restart is a fixed Gboard-only command on a
  worker thread, after successful persistence, with a 25-second timeout and
  process cleanup. Root failure offers an explicit manual-settings action.
- Desktop Skia shader compile/render/clip tests are now part of the test APK
  workflow, in addition to Java/source checks, build and APK verification.

Idle stock keys are intentional. No permanent custom keyboard layout is created.
Neither transition timing nor root access can be guaranteed without testing the
user's Gboard build and framework/device combination.
