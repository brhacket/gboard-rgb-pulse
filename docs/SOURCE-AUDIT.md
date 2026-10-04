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


## Independent effect switches and per-key fades (40.0 test)

Root cause in v39: the UI's ripple switch wrote `enabled`, which was also used as
an overall effects gate, while refined mode forced `glass=true` and `sideStyle=1`.
Backgrounds consequently depended on the mislabelled ripple switch. There was no
independent, authoritative ripple preference.

- `ripple40` now explicitly controls the ripple; `tapEffects36` controls the
  background. In refined mode the controller's internal enabled state is their
  OR, and key styling/side waves follow only ripple40. Existing preferences are
  migrated without overwriting an explicitly saved ripple-off value.
- UI callbacks reload configuration from the private draft so UI, preview and
  hook all use the same derivation. Apply/restart consent remains unchanged.
- New pure-Java BorderFade supplies independent time-based attack/release per key
  (45ms/130ms time constants, not fixed completion times). The release continues
  until visually negligible, including after waves expire. It is used in both
  key overlays and preview, cleared on pause/layout reset and disabled effects.
- Tests cover all four effect combinations against both stale legacy master
  values, reopen/migration, gradual rise/fall, finite cleanup and frame-rate parity.

No Android device is available here; real Gboard rendering and settings sharing
must still be verified on the user's framework/device combination.


## Stable tiles, independent brightness and ripple legends (41.0 test)

- The old shared opacity field no longer controls both refined effects. Ripple
  strength and background brightness have independent saved keys/defaults (70%
  and 90%). Legacy strength remains available to legacy rendering only. Draft
  setup seeds absent duration instead of overwriting existing opacity/duration.
- Keep key tiles visible is explicit and persisted. While either effect is on,
  a reversible background wrapper paints a stable charcoal base underneath native
  backgrounds/content; tiles do not appear/disappear with ripple alpha. Disabling
  both effects restores originals. Native state/callback/padding forwarding remains.
- Removed the white interior wash from ripple overlays. They now draw borders
  only, avoiding unintended contrast changes over key faces/lettering.
- Added narrowly scoped native Canvas text-paint tinting during verified key
  recording/drawing. It reads no typed/editor text. It uses a copied paint and
  restores arguments/scopes after drawing, without resizing/repositioning glyphs.
  Idle light legends are 14% dimmer in RGB; ripple brightens toward white using
  the border's existing per-key fade. Dark original legends stay dark on light
  themes. Original alpha is preserved; shader-based text paints are left alone.
- Preview uses the same LegendTint policy; text and overlays redraw together.
  Tests cover independent brightness, persisted tiles, monotonic legend tint,
  unchanged alpha, and dark-legend contrast.

On-device rendering scope, native key themes, startup timing and performance
remain unverified here. In particular, custom Gboard render paths may not use
hooked Canvas text calls. These changes are not a claim that every dimming or
startup variation has been reproduced and eliminated.


## Material-tonal controls and expanded effects (42.0 test)

- Ripple radius is strictly linear in progress. Per-key opacity attack/release is
  unchanged. Row flow, Soft echo, Wide glow and Touch pulse have bounded intensity.
- Four independent opaque colors cover active/inactive borders and letters.
  Border width is 0.5–3dp. Every color row has a swatch; the picker previews the
  candidate with hex input and tonal presets before Use in draft. Background hue
  controls show their selected colors and the mode's palette. Apply is still required.
- Added Material bloom, Diffused ring and Tonal orbit shaders at IDs 12–14, leaving
  legacy IDs and shuffle ID 11 intact. Shuffle excludes its reserved ID. Fresh
  defaults prefer a soft lavender Material bloom; existing saved selections remain.
- Native letter tint now uses the requested state colors, handles drawGlyphs,
  overrides copied shader/color filters, and includes a reversible TextView color
  adapter for cached legends. It never replaces fonts, glyph positions or reads
  editor text. Tests exercise the adapter with compile-only Android stubs, not a
  device, including disable/detach and native theme updates.
- Shader validation covers all 14 actual programs, including new effects.

Visual fit, color contrast chosen by the user, native glyph caching paths, and
Gboard framework behavior still require device checks. Material-inspired here
means restrained tonal palettes, low-detail lighting and rounded controls—not a
claim of Material library certification or a visually verified Android screenshot.


## Native legend draw scope follow-up (43.0 test)

v42 registered only each key root's public draw method. The refined path did not
register child onDraw methods, unlike the legacy CapHooks implementation. Android
HWUI can record custom child views through onDraw without the parent's public
draw call, leaving Canvas text/glyph hooks without key ownership.

- Register verified key descendants, their draw methods and declared onDraw
  methods throughout the View inheritance chain; retain the existing recording
  hook. Scope nesting restores previous ownership and cleanup removes recycled
  child registrations without clobbering ownership assigned to another key.
- Text tinting remains restricted to verified key descendants. No editor text,
  font metrics or glyph positions are extracted or changed.
- Opt-in, bounded RGBPulse legend diagnostics report binding counts, the first
  scope/tint invocation and observed bitmap/render-node operations. Diagnostic
  observations do not themselves identify bitmap contents as letters.

This addresses a concrete missing rendering path, not a device-confirmed root
cause. If letter colors still fail, obtain the user's Gboard version and filtered
legend diagnostics before attempting further renderer changes.


## Settings transport and all-off follow-up (44.0 test)

The previous editor equated an XML commit with runtime application. Gboard read
cached XSharedPreferences, while the editor could fall back to private storage.
There was no delivery acknowledgement. These are concrete transport weaknesses,
not proof of the cause on the user's device.

- Remove WORLD_READABLE/XSharedPreferences and xposedsharedprefs metadata. Editor
  and provider share private settings in the module's default process.
- Export a narrow read/ack provider; only own UID/Gboard UID can read, and only
  Gboard can acknowledge. No query, file, insert, update or delete API. Large legacy
  font blobs are excluded. Package visibility is explicit (forceQueryable).
- A signature-permission-gated, package-targeted notification triggers an async
  provider fetch. Coalesced generations discard superseded reads. Keyboard-open
  also retries; no draw/UI-thread provider calls. Errors disable effects.
- Apply a snapshot and restore old visuals on the Gboard main thread before
  acknowledging its unique revision. The provider rejects stale acknowledgements;
  the editor only labels matching revisions confirmed. This is receipt, not proof
  of rendered pixels or a continuous liveness/heartbeat guarantee.
- Serialize provider reads and saves. Failed commit restores the previous in-memory
  preferences so Android's commit(false) cache mutation cannot publish failed edits.
- Always restore styling, unbind panel, invalidate descendants and cancel callbacks
  when applying off, including hidden/null-body controllers. Original Gboard native
  press effects remain stock. The emergency off action is explicitly confirmed.

Revision and failed-save rollback have Android-free executable tests. Security,
coalescing and cleanup contracts have source assertions; compilation checks use
Android API 34. These are not Android Binder/LSPosed integration tests. Real-device
scope, provider access, runtime receipt and visuals still require verification.
Reboot once after upgrading to unload pre-v44 injected code.


## Tileless keys and distinct motion (45.0 test)

The v44 screenshot confirms revision delivery, and shows residual native fills
on the bottom row. The previous tiles41 option only controlled module faces;
it restored native backgrounds when off. hideTiles45 instead wraps backgrounds
in verified key subtrees with a non-drawing drawable, preserving padding, state,
latest theme replacements, text/image content and hit targets. Recycling and
all-off restore originals. No broad Canvas shape suppression or toolbar changes.
Ripple remains a letter-color wave without rectangular outlines in this mode.
Direct custom-rendered key fills are not proven covered; phone testing remains
necessary, especially space/Enter/language-switch and symbol layout transitions.

Four new shader programs use sideways sheets, a stationary elliptical spotlight,
diagonal ribbons and an upward curtain rather than radial ring variations.
Desktop shader tests include lifecycle/clipping and pairwise alpha-geometry
differences across three phases. These establish different output, not subjective
visual quality or Android runtime correctness. Existing palettes/Apply/receipt
semantics remain intact; both effects off still restores stock Gboard.


## Pulse Studio and motion options (46.0 test)

- Separate key fills from ripple strokes: hideTiles45 continues suppressing native
  fills but no longer gates the preview or native RippleOverlay border renderer.
- Add full-keyboard distance propagation, rightward chase, inward sweep and a
  two-beat local ripple. Linear travel and soft envelopes remain shared with preview.
- Add diamond-front, undulating-tide and split-horizontal-plane pulse programs.
- Sticky preview is outside the weighted settings ScrollView, can be minimized,
  and adapts height to remaining window space. Three pages split Ripple, Pulse
  and Keyboard controls. Primary Apply saves directly; restart remains confirmed
  under troubleshooting. Consistent dark surfaces, mint controls and 48dp targets.
- Independent optional opening/closing light overlays (Off by default), driven by
  a bounded ValueAnimator on the main thread, restricted to the verified panel.
  Preview buttons are explicit. No key transforms or interception/delay of hide.
  Closing starts at requestHideSelf / non-finishing onFinishInputView and is
  cancelled when hidden: Android can remove the window before any light is drawn.
  This limitation is exposed in the UI, not treated as a guaranteed exit animation.
- Off clears lifecycle options as well as tap/ripple toggles. Read failures, hide,
  detach/dispose and preview pause remove lifecycle overlays. New options never
  start a demo just by selecting a style.

Java policy and shader geometry checks cover added motions; source contracts
cover UI structure, independent borders and lifecycle cancellation. Compilation
uses Android API 34. No emulator or physical-phone UI/render verification yet.


## Fast key-local pulses and delivery follow-up (47.0 test)

- Replace the overlapping 22-entry pulse catalog with ten genuinely different
  geometries: eight local and two explicitly wide. Local coordinates use the real
  hit key's rectangle in the same verified panel coordinate space as the touch.
  Footprint ends at 1.2 times the key half-extents (10% beyond each edge), independent
  of panel dimensions. Preview passes its own actual key rectangle through the
  identical Fx/FieldFx path. Space and function keys retain their actual geometry.
- Pulse lifetime is independently 120–360 ms, default 220. Limit to three concurrent
  local pulses, replace old wide pulses on each tap, cap summed shader alpha at
  .48 local / .28 wide before user brightness. No new off-state animation or demos.
- Fresh pulse47 indices avoid accidentally remapping an old selected effect. The
  removed ripple indices map to Full keyboard; new patterns use ripplePattern47.
  Broad patterns change per-key distance/front geometry, not extra synthetic taps.
- Apply: the old button is wired to persist(false), but no phone trace identifies
  its failure. Add independent observer notifications, bounded notification retries,
  handled-error busy-state release, and visible saved/waiting feedback. Require both
  matching revision and runtime build receipt so an old injected APK cannot falsely
  confirm compatibility. Reads/ack remain UID-restricted, async and fail-closed.
- Lifecycle overlays use stronger, geometric full-panel reveals. Closing starts
  at nonzero intensity; 520 ms opening and 260 ms closing. First actual key tap
  cancels the intro. No keyboard transform or hide delay; Android may still provide
  no visible closing frames. The limitation remains disclosed.

Checks include executable revision/build matching, config migration and ripple
bounds/cross-row reach; desktop production-shader tests prove local support bounds,
aggregate alpha caps under four simultaneous taps, specializations and different
local alpha geometry. These are not real Binder/IME or phone rendering tests.


## Delivery redesign and everyday separation (48.0 test)

The user confirms v47 stays waiting for Gboard, so this is a delivery blocker,
not evidence that animation parsing/rendering ignores a confirmed configuration.
No phone logs identify which hop failed. Instead of adding another provider retry,
v48 uses a signed, package-targeted broadcast containing the actual applied
snapshot. The injected receiver requires the module-defined signature permission.
Successful main-thread application sends an immutable, one-shot PendingIntent to
a non-exported module receipt receiver, which checks revision/build against the
current saved settings. No exported acknowledgement endpoint is added.

A small exported bootstrap receiver accepts only a fixed request action, ignores
all extras, throttles requests and sends only private applied settings to the fixed
Gboard package. It returns no data to the caller and cannot change preferences.
A caller can trigger a bounded refresh, not inject configuration or capture it.
Monotonic elapsedRealtimeNanos stamps reject out-of-order snapshots; unchanged
revisions acknowledge without resetting live effects. A 3s missing-response timeout
restores off, rather than indefinitely using stale visuals. Ordered broadcast
status distinguishes no live receiver reply from a missing application receipt.
The older UID-restricted provider remains for compatibility, but the new Gboard
client does not depend on it. Real Android delivery still needs device validation.

The footer exposes Restart with confirmation, independently of Apply. Root failure
gets a dedicated dialog, not a transient status overwritten by acknowledgement.

The quiet background renderer and the original wide orbit/aurora programs were
removed in the final 1.1.0 build; the wide catalog now offers two background
effects (Comet sweep, Nebula bloom) rendered by the same bounded pulse field.
The everyday preset stages Soft press / 160ms, disabling ripple and
entrance/exit lighting. No application/demo/restart occurs from preset
selection. Tap feedback and wide effects can run without ripple or each other.

Tests cover opt-in combinations and existing policy/render contracts. Security/lifecycle transport contracts are source checks plus executable
revision/build tests, not proof of Android broadcast/IME behavior.
