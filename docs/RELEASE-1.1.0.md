## Gboard RGB Pulse 1.1.0

Long-session stability, one-button saving, a curated catalog of layered,
luminous tap animations and two new wide background effects.

### Highlights
- **Ten curated tap animations** (Touch page): each pulse is built from
  layered light — a hot near-white core, a saturated mid glow, a wide soft
  halo and a prismatic fringe that shifts hue across the feature — with eased
  motion (comet tails, spring-snapping brackets, spectral swipes, spark
  bursts with twinkle, chromatic rings). Two optional wide sweeps cross the
  whole keyboard. Fully opt-in, with strength, length, size and thickness
  controls.
- **One button replaces Apply/Restart**: *Save & restart Gboard* saves every
  setting and force-stops Gboard in a single tap, so the keyboard always
  reopens with exactly what you saved. The old Apply changes button is gone.
- **Effects no longer vanish during long sessions**: failed settings pulls are
  retried, confirmed settings are never blanked by a slow module app, and the
  keyboard re-syncs every 30 seconds while it is open.
- **Independent key and background animations, in separate tabs**: the studio
  gains a dedicated Background tab. Twelve key animations (including new
  Petal bloom, Glass tap, Orbit dots and Liquid rise) and four background
  light shows (Comet sweep, Nebula bloom, plus new Starfall and Polar veil)
  render as independent passes over the same taps, so both can run at once.
  Each group has its own color mode and hue pickers. Local effects are
  grounded on their key with a rim bloom, and wall-clock flicker was replaced
  by deterministic choreography.
- **Richer hello & goodbye**: ignition, curtains and horizon transitions are
  now layered light — hot crests with chromatic fringes, seam flashes, sky
  glow and reflections — instead of single gradients.
- **Better color controls**: a draggable hue spectrum bar with a round thumb,
  a saturation/value square inside the color dialogs (synced with hex input,
  swatches and the live sample), and a secondary hue row that appears only in
  two-color gradient mode. Ripple duration can go down to 150 ms.

### Requirements and installation
Android 13+ with LSPosed/Vector; scope the module to Gboard
(`com.google.android.inputmethod.latin`). Keep a backup keyboard enabled.

**Signing:** This build uses a new build-generated signing key. It cannot
update prior installs in place. Uninstall the previous module first (module
settings are lost), install `Gboard-RGB-Pulse-1.1.0.apk`, enable the module
for Gboard, and reboot once to unload older hooks.

Choose your options, then press **Save & restart Gboard**. The force-stop
needs root; if it is denied, simply opening the keyboard picks up the saved
settings instead.

### Verification and limitations
Publication requires automated Java/source tests, desktop Skia shader tests
for the pulse field, Android compilation, archive checks, and APK signature
verification. `SHA256SUMS.txt` and `APK-verification.txt` accompany the APK.

Rendering and force-stop behavior remain device-unverified; Android may cut
closing light short. Publishing this version does not imply those device
checks have passed.
